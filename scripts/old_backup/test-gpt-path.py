#!/usr/bin/env python3
"""Start an isolated three-process stack. Default: local OpenAI Stub. --mock: full Mock smoke suite."""
import argparse
import http.cookiejar
import http.server
import json
import os
from pathlib import Path
import socket
import subprocess
import threading
import time
import urllib.request

# 저장소 루트를 기준으로 미리 빌드된 각 모듈의 JAR 파일을 실행한다.
ROOT = Path(__file__).resolve().parents[2]
# 기존 개발 서버와 구분되는 테스트용 포트: 클라이언트 → 에이전트 → MCP, 별도 GPT 대역.
CLIENT, AGENT, MCP, STUB = 18080,18081,18082,18083
# GPT 대역이 받은 요청 종류를 기록해 의도 해석·도구 호출 경로를 검증한다.
calls_seen=[]

# 실제 OpenAI API 대신 정해진 응답을 돌려주는 로컬 HTTP 서버다.
# 모델의 답변 품질이 아니라 GPT 연동 코드와 실제 MCP까지 이어지는 흐름을 검사한다.
class Stub(http.server.BaseHTTPRequestHandler):
    # HTTP 기본 접근 로그는 생략해 테스트 출력이 복잡해지지 않게 한다.
    def log_message(self, *args): pass
    def do_POST(self):
        # 요청 본문이 chunked 전송이면 조각을 합치고, 아니면 지정된 길이만큼 읽는다.
        if self.headers.get('Transfer-Encoding','').lower() == 'chunked':
            chunks=[]
            while True:
                size=int(self.rfile.readline().split(b';',1)[0],16)
                if not size:
                    while self.rfile.readline().strip(): pass
                    break
                chunks.append(self.rfile.read(size)); self.rfile.read(2)
            raw=b''.join(chunks)
        else:
            raw=self.rfile.read(int(self.headers['Content-Length']))
        request=json.loads(raw)
        # 사용자 메시지를 합쳐 이번 테스트가 요구하는 고정 응답을 선택한다.
        user='\n'.join(m['content'] for m in request['messages'] if m['role']=='user')
        tools=[]
        # 도구가 포함된 요청은 일반 상품 검색 에이전트의 도구 호출 루프다.
        if request.get('tools'):
            # 이 검색에서 허용한 도구가 searchProducts 하나뿐인지 확인한다.
            assert [t['function']['name'] for t in request['tools']] == ['searchProducts'], request['tools']
            previous=[m for m in request['messages'] if m['role']=='tool']
            # 첫 요청에는 검색 도구 호출을, 도구 실행 결과를 받은 뒤에는 상품 ID를 반환한다.
            if not previous:
                tools=[{'id':'call-catalog','type':'function','function':{'name':'searchProducts','arguments':json.dumps({
                    'keyword':'텀블러','minPrice':0,'maxPrice':30000,'category':None,
                    'excludeProductIds':[],'shippingGroups':[],'inStockOnly':True,'sort':'PRICE_ASC','limit':20})}}]
                content=None
            else:
                # 일부러 존재하지 않는 ID를 선택해 서버가 잘못된 선택을 감지하는지도 검사한다.
                content=json.dumps({'productIds':['invented'] if '잘못된 선택' in user else ['P009']})
            calls_seen.append('catalog-tool' if tools else 'catalog-selection')
        else:
            # 도구가 없는 요청은 자연어를 NEW/REFINE 및 검색 조건으로 바꾸는 의도 해석이다.
            text=user.split('현재 발화: ',1)[1]
            decision={'action':'NEW','capabilityId':'cart.freeShippingRecommendation','keyword':None,'category':None,
                      'budget':None,'coupons':False,'points':False,'question':None}
            # 후속 요청의 프롬프트에 기존 작업과 3만 원 예산이 전달됐는지 확인한다.
            if '음료' in text:
                assert 'cart.freeShippingRecommendation' in user and '"maxPrice":30000' in user, user
                decision.update(action='REFINE',category={'operation':'SET','value':'음료'})
            elif '텀블러' in text:
                decision.update(capabilityId='catalog.search',keyword={'operation':'SET','value':'텀블러'},budget={'operation':'SET','value':30000})
            else:
                decision['budget']={'operation':'SET','value':30000}
            content=json.dumps(decision,ensure_ascii=False)
            calls_seen.append('interpret')
        # 애플리케이션이 실제 API 응답처럼 읽을 수 있도록 Chat Completions 형식을 맞춘다.
        message={'role':'assistant','content':content}
        if tools: message['tool_calls']=tools
        payload={'id':'chatcmpl-stub','object':'chat.completion','created':1,'model':'gpt-4.1-mini',
                 'choices':[{'index':0,'message':message,'finish_reason':'tool_calls' if tools else 'stop'}],
                 'usage':{'prompt_tokens':10,'completion_tokens':10,'total_tokens':20}}
        data=json.dumps(payload).encode()
        self.send_response(200);self.send_header('Content-Type','application/json');self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)

# 프로세스가 살아 있고 health API가 200을 반환할 때까지 최대 90회 확인한다.
# 먼저 종료되면 로그 위치를 안내하고, 끝내 준비되지 않으면 타임아웃으로 실패한다.
def ready(port,process):
    for _ in range(90):
        if process.poll() is not None: raise RuntimeError(f'Service exited: port {port}; inspect .run/*-gpt-test.log')
        try:
            with urllib.request.urlopen(f'http://127.0.0.1:{port}/actuator/health',timeout=1) as response:
                if response.status==200:return
        except Exception: pass
        time.sleep(1)
    raise TimeoutError(port)

# 기본 모드: 로컬 GPT 대역 + 실제 세 서비스. mock 모드: GPT 대역 없이 스모크 테스트.
def run(mock=False, preview=False):
    # 필요한 포트에 잠시 바인딩해 이미 사용 중이면 서비스 실행 전에 실패하게 한다.
    # 확인 후 소켓을 닫으므로 포트를 계속 예약하는 것은 아니다.
    sockets=[]
    try:
        for port in [CLIENT,AGENT,MCP]+([] if mock else [STUB]):
            s=socket.socket();s.setsockopt(socket.SOL_SOCKET,socket.SO_REUSEADDR,1);s.bind(('127.0.0.1',port));sockets.append(s)
    finally:
        for s in sockets:s.close()
    # 서비스별 표준 출력과 오류는 .run 디렉터리의 로그 파일에 모은다.
    logs=ROOT/'.run';logs.mkdir(exist_ok=True)
    server=None
    # HTTP 요청 처리를 백그라운드 스레드에서 실행해 테스트와 함께 진행한다.
    if not mock:
        server=http.server.ThreadingHTTPServer(('127.0.0.1',STUB),Stub)
        threading.Thread(target=server.serve_forever,daemon=True).start()
    processes=[];streams=[]
    try:
        # 에이전트의 MCP 접속 주소를 테스트 서버로 고정한다.
        # 기본 모드는 OpenAI 주소도 로컬 대역으로 바꾸므로 유료 API를 호출하지 않는다.
        agent_args=[f'--spring.ai.mcp.client.streamable-http.connections.shopping.url=http://127.0.0.1:{MCP}']
        if mock: agent_args+=['--spring.profiles.active=mock']
        else: agent_args += [f'--spring.ai.openai.base-url=http://127.0.0.1:{STUB}', '--spring.ai.openai.api-key=local-stub-key','--spring.profiles.active=default']
        # 의존 순서대로 MCP → 에이전트 → 클라이언트를 실행하고 각각 준비될 때까지 기다린다.
        # JAR 빌드는 이 스크립트가 하지 않으므로 실행 전에 완료되어 있어야 한다.
        for module,port,args in [('shopping-mcp',MCP,[]),('shopping-agent',AGENT,agent_args),('chatbot-client',CLIENT,[f'--agent.url=http://127.0.0.1:{AGENT}'])]:
            stream=open(logs/f'{module}-gpt-test.log','w');streams.append(stream)
            # 상위 셸의 JSON 설정이 테스트용 Spring 설정에 끼어들지 않게 제거한다.
            env=dict(os.environ);env.pop('SPRING_APPLICATION_JSON',None)
            process=subprocess.Popen(['java','-jar',str(ROOT/module/'build/libs'/f'{module}-0.1.0.jar'),f'--server.port={port}',
                                      '--spring.lifecycle.timeout-per-shutdown-phase=2s']+args,cwd=ROOT,stdout=stream,stderr=subprocess.STDOUT,env=env)
            processes.append(process);ready(port,process)
        # 하이픈이 있는 스크립트를 파일 경로로 불러와 HTTP 요청 도우미를 재사용한다.
        import importlib.util
        spec=importlib.util.spec_from_file_location('smoke',Path(__file__).resolve().with_name('smoke-test.py'))
        smoke=importlib.util.module_from_spec(spec);spec.loader.exec_module(smoke)
        # smoke-test.py의 기본 주소 대신 방금 띄운 테스트 클라이언트를 호출한다.
        smoke.BASE=f'http://127.0.0.1:{CLIENT}'
        # --mock은 전체 스모크 시나리오를 실행한다. --preview를 함께 주면 Enter까지 유지한다.
        if mock:
            smoke.run()
            if preview:
                try:
                    input(f'Preview: http://127.0.0.1:{CLIENT} (press Enter to stop)')
                except EOFError:
                    pass
        else:
            client=smoke.new_client()
            # GPT 의도 해석 결과가 무료배송 추천과 후속 카테고리·예산 유지에 반영되는지 확인한다.
            first=smoke.chat(client,'무료배송 추천, 예산 3만원',conversation='gpt',revision=0)
            assert first['task']['constraints']['maxPrice']==30000 and len(first['products'])==3,first
            second=smoke.chat(client,'음료 카테고리만',conversation='gpt',revision=1)
            assert smoke.ids(second)==['P004','P005'] and second['task']['taskId']==first['task']['taskId'],second
            assert second['task']['constraints']['maxPrice']==30000,second
            # 일반 검색은 GPT의 검색 도구 호출 → 실제 MCP 실행 → 상품 선택까지 연결되어야 한다.
            generic=smoke.chat(client,'새로 텀블러 3만원 이하 찾아줘',conversation='gpt',revision=2)
            assert smoke.ids(generic)==['P009'] and generic['products'][0]['shipping'] is None,generic
            assert generic['execution']=='AGENT',generic
            assert [t['tool'] for t in generic['toolCalls']]==['searchProducts'],generic
            # GPT가 잘못된 상품 ID를 선택하면 정해진 워크플로로 대체 실행되어야 한다.
            fallback=smoke.chat(client,'새로 텀블러 3만원 이하 찾아줘. 잘못된 선택 테스트',conversation='gpt',revision=3)
            assert smoke.ids(fallback)==['P009'] and fallback['execution']=='WORKFLOW_FALLBACK',fallback
            # 네 발화의 의도 해석과 두 일반 검색의 도구 호출이 대역 서버에 도달했는지 확인한다.
            assert calls_seen.count('interpret')==4 and calls_seen.count('catalog-tool')==2,calls_seen
            print('PASS: GPT structured intent + task state + scoped tool loop → real MCP; invalid plan falls back once; no paid GPT API called')
    finally:
        # 성공·실패와 관계없이 클라이언트 → 에이전트 → MCP 역순으로 종료한다.
        # 정상 종료를 8초 기다린 뒤 응답이 없으면 강제 종료하고 로그와 대역 서버도 닫는다.
        for process in reversed(processes):
            process.terminate()
            try:process.wait(timeout=8)
            except subprocess.TimeoutExpired:process.kill();process.wait()
        for stream in streams:stream.close()
        if server:server.shutdown();server.server_close()

# --mock: 모의 의도 해석으로 전체 스모크 테스트, --preview: mock 테스트 후 서버 유지.
if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--mock',action='store_true');parser.add_argument('--preview',action='store_true');options=parser.parse_args();run(options.mock,options.preview)

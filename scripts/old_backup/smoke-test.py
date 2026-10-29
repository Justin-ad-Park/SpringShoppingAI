#!/usr/bin/env python3
"""Three-service Mock tests including multi-turn state; no external API calls."""
import concurrent.futures
import http.cookiejar
import json
import os
import urllib.error
import urllib.request

# 이미 실행 중인 클라이언트 서버에 요청한다. 환경변수로 테스트 대상 주소를 바꿀 수 있다.
BASE = os.environ.get('SMOKE_BASE_URL', 'http://127.0.0.1:8080')

# 쿠키를 보관하는 HTTP 클라이언트: 같은 객체를 쓰면 같은 브라우저 세션처럼 동작한다.
def new_client():
    return urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))

# 채팅 API를 호출하고 JSON 응답을 반환한다.
# conversation은 세션 안의 대화 ID, revision은 클라이언트가 알고 있는 대화 버전이다.
# new=True는 기존 검색 조건을 이어받지 않고 새 검색을 시작하라는 요청이다.
def chat(client, message='무료배송 상품을 추천해줘', scenario='standard', conversation='default', revision=None, new=False):
    body = {'message': message, 'scenario': scenario, 'conversationId': conversation, 'newSearch': new}
    if revision is not None:
        body['expectedRevision'] = revision
    req = urllib.request.Request(BASE + '/api/chat', data=json.dumps(body).encode(), headers={'Content-Type': 'application/json'})
    with client.open(req, timeout=180) as response:
        # 프록시 등을 거치며 Transfer-Encoding 헤더가 중복 추가되지 않았는지 확인한다.
        assert not response.headers.get_all('Transfer-Encoding') or len(response.headers.get_all('Transfer-Encoding')) == 1
        return json.load(response)

# 상품 전체 정보 대신 ID 목록으로 추천 결과와 순서를 간단히 비교한다.
def ids(result):
    return [p['id'] for p in result['products']]

# assert가 하나라도 실패하면 테스트가 중단되고, 모두 통과하면 마지막에 PASS를 출력한다.
def run():
    client = new_client()
    # 고정된 테스트 데이터와 예상 결과를 비교하므로 mock 모드인지 먼저 확인한다.
    with client.open(BASE + '/api/status') as response:
        assert json.load(response)['mode'] == 'mock', 'Start services with --mock first'
    # 기본 무료배송 추천: 상품 3개, 도구 호출 7회, 배송비 0원인지 확인한다.
    # 기본 데이터에서 무료배송까지 부족한 8천 원부터 기본 상한 1만 5천 원까지 검색한다.
    first = chat(client, conversation='shipping', revision=0)
    assert ids(first) == ['P003', 'P004', 'P005'], first
    assert len(first['toolCalls']) == 7, first
    assert all(p['shipping']['shippingFee'] == 0 for p in first['products'])
    assert first['details']['minPrice'] == 8000 and first['details']['maxPrice'] == 15000
    # 후속 발화가 같은 추천 작업(taskId)을 이어가며 조건을 누적하는지 확인한다.
    task_id = first['task']['taskId']
    second = chat(client, '음료 카테고리에서만 추천해줘', conversation='shipping', revision=1)
    assert ids(second) == ['P004', 'P005'] and second['task']['taskId'] == task_id, second
    third = chat(client, '예산은 3만원까지', conversation='shipping', revision=2)
    assert third['task']['constraints']['maxPrice'] == 30000, third
    # 쿠폰·적립금 조회는 독립 요청이므로 진행 중인 추천 작업을 교체하지 않아야 한다.
    lookup = chat(client, '쿠폰과 적립금 조회', conversation='shipping', revision=3)
    assert lookup['action'] == 'INDEPENDENT' and lookup['task']['taskId'] == task_id, lookup
    assert lookup['details']['points']['balance'] == 3500
    assert lookup['details']['coupons']['coupons'][0]['discount'] == 2000
    # 카테고리를 바꿔도 앞서 설정한 3만 원 예산은 유지되어야 한다.
    living = chat(client, '생활 카테고리에서만 추천해줘', conversation='shipping', revision=4)
    assert ids(living) == ['P009'] and living['task']['constraints']['maxPrice'] == 30000, living
    # 사용자 예산을 해제하면 기본 상한(1만 5천 원)으로 돌아간다.
    # 생활 카테고리는 유지되므로 이 가격 범위에서는 추천 상품이 없다.
    cleared = chat(client, '예산 해제', conversation='shipping', revision=5)
    assert not cleared['products'] and cleared['details']['maxPrice'] == 15000, cleared
    assert cleared['task']['constraints']['maxPrice'] is None
    # '새로' 검색하면 작업이 교체되고 이전 무료배송·카테고리 조건이 초기화된다.
    # 일반 상품 검색은 배송비 조회 없이 searchProducts 도구만 호출한다.
    fresh = chat(client, '새로 3만원 이하 텀블러 찾아줘', conversation='shipping', revision=6)
    assert ids(fresh) == ['P009'] and fresh['products'][0]['shipping'] is None, fresh
    assert fresh['task']['taskId'] != task_id and fresh['task']['constraints']['category'] is None
    assert [t['tool'] for t in fresh['toolCalls']] == ['searchProducts'], fresh
    assert fresh['toolCalls'][0]['arguments']['query']['shippingGroups'] == []
    # newSearch 플래그로 시작해도 이전 작업의 카테고리·키워드·예산을 물려받지 않아야 한다.
    reset = chat(client, '무료배송 상품 추천해줘', conversation='shipping', revision=7, new=True)
    assert ids(reset) == ['P003','P004','P005'] and reset['task']['constraints']['maxPrice'] is None, reset
    # 일반 키워드 검색과 무료배송 추천의 예외 상황도 확인한다.
    generic = chat(client, '새로 그래놀라 찾아줘', conversation='generic')
    assert ids(generic) == ['P001'], generic
    # 이미 무료배송인 장바구니 / 빈 장바구니 / 조건에 맞는 상품 없음.
    for scenario, expected in [('already-free','이미 무료배송'), ('empty','비어'), ('no-match','없습니다')]:
        result = chat(client, scenario=scenario, conversation=scenario)
        assert not result['products'] and expected in result['message'], result
    # 무료배송까지 필요한 금액보다 예산이 작으면 결과 없음으로 안내해야 한다.
    insufficient = chat(client, '무료배송 상품 추천해줘. 예산은 7000원 이하야', conversation='insufficient')
    assert not insufficient['products'] and insufficient['outcome'] == 'NO_RESULTS', insufficient
    # 배송 정책 질문은 정책 조회 도구만 호출하고 무료배송 추천 맥락을 유지한다.
    policy = chat(client, '무료배송 조건이 뭐야?', conversation='shipping')
    assert [t['tool'] for t in policy['toolCalls']] == ['getShippingPolicy']
    assert policy['task']['capabilityId'] == 'cart.freeShippingRecommendation'
    # 다른 쿠키 세션은 대화 ID가 같아도 기존 작업 상태를 공유하면 안 된다.
    isolated = chat(new_client(), '음료만', conversation='shipping', revision=0)
    assert isolated['outcome'] == 'NEEDS_INPUT' and isolated['task'] is None, isolated
    # 잘못된 시나리오·공백·너무 긴 입력은 400, 오래된 대화 버전은 충돌(409)이어야 한다.
    for kwargs, status in [({'scenario':'invalid'},400), ({'message':' '*3},400),
                           ({'message':'x'*2001},400), ({'conversation':'shipping','revision':0},409)]:
        try:
            chat(client, **kwargs)
            raise AssertionError(f'Expected HTTP {status}')
        except urllib.error.HTTPError as error:
            assert error.code == status, error.code
    # 같은 세션에서 서로 다른 대화를 동시에 요청해도 결과가 섞이지 않는지 확인한다.
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        scenarios = ['standard','already-free','empty','standard']
        results = list(pool.map(lambda item: chat(client,scenario=item[1],conversation='parallel-'+str(item[0])), enumerate(scenarios)))
        assert [len(r['products']) for r in results] == [3,0,0,3]
    print('PASS: generic search, free-shipping workflow, follow-up category/budget, clear/new, independent lookup, cookie isolation, HTTP validation and concurrent conversations')

if __name__ == '__main__':
    run()

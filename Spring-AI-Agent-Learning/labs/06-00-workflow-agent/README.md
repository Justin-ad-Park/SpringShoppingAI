# 06 Workflow·Agent·혼합 방식

[06장 교육 문서](../../docs/lessons/06-workflow-agent.md)를 따라 같은 쇼핑 요청의 처리 방식을 비교합니다.
기본 모듈은 직접 만든 횟수 계수·시간 제한·스레드 실행 코드를 사용하지 않습니다. 금액·카테고리 검증과 결과 없음 처리는 업무 요구사항으로 유지합니다.

## 로컬 검증

저장소 루트에서 실행합니다. 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:test --tests '*WorkflowAgentLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:bootJar
```

## 실제 모델 연결

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다. API 사용량이 발생합니다. IntelliJ의 Working directory도 저장소 루트로 설정합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:bootRun
curl http://127.0.0.1:8096/api/compare/workflow \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '배송비 포함 3만원 이하 음료를 추천해줘.'
./gradlew :Spring-AI-Agent-Learning:labs:06-00-workflow-agent:test --tests '*WorkflowAgentOpenAiLiveTest'
```

경로의 `workflow`를 `agent`, `hybrid`로 바꿔 비교합니다. 응답은 `mode`, `status`, `answer`, `offers`입니다. Agent의 offers는 실제 조회한 금액이며 최종 예산 준수는 보장하지 않습니다. 혼합 방식은 조회 사실을 다시 검증해 확정한 후보만 반환합니다.

모델 호출 수는 로컬 테스트 서버의 요청 목록으로 확인합니다. 자체 RunState나 CountedChatModel은 사용하지 않습니다.

실행 제한은 앞 단원의 보충 실습에서 확인합니다. 각각 별도 모듈입니다.

- [도구 호출 횟수 제한](../05-05-tool-call-limits/README.md)
- [모델 요청 타임아웃과 재시도](../02-05-model-timeout-retry/README.md)

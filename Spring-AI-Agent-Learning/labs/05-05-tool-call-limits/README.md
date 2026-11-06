# 05 보충 5: 도구 호출 횟수 제한

[교육 문서](../../docs/lessons/05-tool-calling/05-tool-call-limits.md)를 먼저 읽습니다. 05장의 도구 호출과 결과 전달을 익힌 뒤 실행하는 독립 모듈입니다.

## 로컬 검증

저장소 루트에서 실행합니다. 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:test --tests '*ToolLimitLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:bootJar
```

## 실제 모델 연결

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다. API 사용량이 발생합니다. IntelliJ의 Working directory도 저장소 루트로 맞춥니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:bootRun
curl http://127.0.0.1:8110/api/tool-limits \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '음료 상품을 소개해줘.'
./gradlew :Spring-AI-Agent-Learning:labs:05-05-tool-call-limits:test --tests '*ToolLimitOpenAiLiveTest'
```

서버를 종료하려면 Ctrl+C를 누릅니다. 로컬 테스트는 고정 HTTP 응답으로 연결 동작을 검증하며, 실제 모델의 문장이나 도구 선택을 재현하는 LLM은 아닙니다.

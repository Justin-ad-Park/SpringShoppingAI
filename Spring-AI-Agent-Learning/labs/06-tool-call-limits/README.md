# 06 확장 1: 도구 호출 횟수 제한

[교육 문서](../../docs/lessons/06-workflow-agent/04-tool-call-limits.md)를 먼저 읽습니다. 기본 Workflow 모듈과 독립 실행되며 별도 프로젝트 의존성이 없습니다.

## 로컬 검증

저장소 루트에서 실행합니다. 실제 외부 API를 호출하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-tool-call-limits:test --tests '*ToolLimitLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:06-tool-call-limits:bootJar
```

## 실제 모델 연결

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다. API 사용량이 발생합니다. IntelliJ의 Working directory도 저장소 루트로 맞춥니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:06-tool-call-limits:bootRun
curl http://127.0.0.1:8110/api/tool-limits \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '음료 상품을 소개해줘.'
./gradlew :Spring-AI-Agent-Learning:labs:06-tool-call-limits:test --tests '*ToolLimitOpenAiLiveTest'
```

서버를 종료하려면 Ctrl+C를 누릅니다. 로컬 테스트는 고정 HTTP 응답으로 연결 동작을 검증하며, 실제 모델의 문장이나 도구 선택을 재현하는 LLM은 아닙니다.

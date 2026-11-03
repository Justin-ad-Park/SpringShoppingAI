# 선택 실습: 두 제공 업체의 ChatClient

OpenAI와 Anthropic 모델을 각각의 이름이 있는 ChatClient로 구성하고 `@Qualifier`로 주입합니다.
다른 실습의 Java 코드에 의존하지 않습니다. Spring AI 버전은 상위 프로젝트의 2.0.2를 사용합니다.

[설명과 전체 코드 읽기](../../docs/lessons/02-first-call/04-multiple-models.md)

저장소 루트에서 로컬 서버를 사용하는 테스트와 실행 JAR 빌드:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test :Spring-AI-Agent-Learning:labs:02-multiple-models:bootJar
```

실제 호출은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL`을 사용합니다.
키를 준비한 뒤 저장소 루트에서 실행하세요. 포트는 8092이며 실제 API 사용 요금이 발생할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:bootRun
```

POST `/api/multi-chat/openai` 또는 `/api/multi-chat/anthropic`에 `Content-Type: text/plain`으로 질문을 보내세요.
테스트는 두 외부 API를 로컬 고정 응답 서버로 대체하며 실제 키나 네트워크 호출이 필요하지 않습니다.

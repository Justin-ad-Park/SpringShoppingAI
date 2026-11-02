# 실습 05: Tool Calling

[교재·실행 예시](../../docs/lessons/05-tool-calling.md)를 먼저 읽습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:test
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:bootRun
```

포트 8095. GET `/api/tools/direct`는 Java 검색, GET `/api/tools/shipping/P02`는 Java 배송비 조회,
POST `/api/tools/chat`은 모델이 도구를 선택하는 경로입니다. POST 본문은 text/plain입니다.
bootRun에는 OPENAI_API_KEY와 OPENAI_CHAT_MODEL이 필요합니다. 도구를 지원하는 모델을 선택하세요.
이 모듈은 다른 실습·쇼핑 서비스와 컴파일 의존성이 없습니다.

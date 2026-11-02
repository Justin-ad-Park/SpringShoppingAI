# 실습 04: 구조화 응답

[교육 문서와 전체 실행 안내](../../docs/lessons/04-structured-output.md)를 먼저 읽습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootRun
```

포트 8094. POST `/api/criteria`는 record, POST `/api/criteria/text`는 원문 텍스트를 반환합니다.
두 API 모두 text/plain 질문을 받습니다. 실제 실행에는 OPENAI_API_KEY, OPENAI_CHAT_MODEL이 필요합니다.
다른 단원·기존 쇼핑 서비스에 대한 프로젝트 의존성은 없습니다.

# 실습 07: 대화 메모리

[교재와 쿠키 실험](../../docs/lessons/07-chat-memory.md)을 먼저 읽습니다.
읽는 순서: MessageView/MemoryReply → MemoryChatService → MemoryController.
이 모듈에는 도구·MCP·RAG·Advisor 연결이 없습니다. ChatMemory에서 읽은 이전 메시지를 직접 프롬프트에 담습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-chat-memory:test
./gradlew :Spring-AI-Agent-Learning:labs:07-chat-memory:bootRun
```

포트 8097. POST `/api/stateless`는 기억 없음, POST `/api/memory/demo`는 세션별 기억을 사용합니다.
GET/DELETE `/api/memory/demo`는 현재 세션의 해당 대화만 조회/삭제합니다. POST 본문은 text/plain입니다.
bootRun에는 OPENAI_API_KEY와 OPENAI_CHAT_MODEL이 필요합니다. 테스트에는 실제 키가 필요 없습니다.
저장은 JVM 메모리뿐이며 서버 재시작 시 사라집니다. 저장 6개, 전송 최대 8개 메시지를 구분하세요.

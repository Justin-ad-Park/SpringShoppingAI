# 실습 07: 대화 메모리

[교재와 쿠키 실험](../../docs/lessons/07-chat-memory.md)을 먼저 읽습니다.
읽는 순서: MessageView/MemoryReply → MemoryChatService → MemoryController.
이 모듈에는 도구·MCP·RAG·Advisor 연결이 없습니다. ChatMemory에서 읽은 이전 메시지를 직접 프롬프트에 담습니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:bootRun
```

포트 8097. POST `/api/stateless`는 기억 없음, POST `/api/memory/demo`는 세션별 기억을 사용합니다.
GET/DELETE `/api/memory/demo`는 현재 세션의 해당 대화만 조회/삭제합니다. POST 본문은 text/plain입니다.
bootRun은 루트 `.env`의 OPENAI_API_KEY와 OPENAI_MODEL을 읽습니다. 로컬 테스트에는 실제 키가 필요 없습니다.
저장은 JVM 메모리뿐이며 서버 재시작 시 사라집니다. 저장 6개, 전송 최대 8개 메시지를 구분하세요.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `MemoryLocalModelTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `MemoryOpenAiLiveTest`: 이전 질문·첫 답변·현재 질문이 다음 요청에 순서대로 포함되는지 확인하고 실제 OpenAI에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 테스트 시작 시 빈 줄과 `===== [ 테스트 메서드명 ] ======`을 한 번 출력하고, 그 아래에서 응답·검증 대상 값을 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:07-00-chat-memory:test --tests com.example.ailearning.l07memory.MemoryOpenAiLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.

# 실습 01: 첫 모델 호출

먼저 [교재 01장](../../docs/lessons/01-setup.md)을 읽습니다.
아래 명령은 모두 **저장소 루트**에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test
```

실제 모델은 같은 터미널에 OPENAI_API_KEY와 OPENAI_CHAT_MODEL을 설정한 뒤 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun
```

다른 터미널에서:

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '의존성 주입이 무엇인지 쉽게 설명해줘.'
```

종료: 서버 터미널 Ctrl+C.

읽는 순서:

1. `src/main/resources/application.yml`: 환경 변수와 모델 설정
2. `FirstChatApplication.java`: 일반 Boot 시작 코드
3. `ChatController.java`: HTTP 본문을 받고 입력 검증
4. `ChatService.java`: ChatClient로 모델 연결
5. 테스트: 관심 있을 때 읽기. 모델 API만 가짜 HTTP 서버로 교체

이 실습은 이전 대화·도구·RAG를 사용하지 않습니다.
테스트의 고정 응답은 실제 AI 답변이 아닙니다. 실제 계정 호출은 별도 실습입니다.
학습용 외부 오류는 기본 500으로 나타날 수 있으며 서버 로그에서 원인을 확인합니다.

[전체 코드와 실행 해설](../../docs/lessons/02-first-call.md) · [프롬프트 변경 실험](../../docs/lessons/03-chatclient-and-prompts.md)

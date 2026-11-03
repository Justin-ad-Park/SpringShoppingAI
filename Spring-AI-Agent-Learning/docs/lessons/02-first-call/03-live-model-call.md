# 02-3. 실제 모델 호출

[01장](../01-setup.md)에서 설명한 저장소 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다.
두 가지 실행 경로 중 하나를 선택하세요.

| 경로 | 확인하는 범위 | 준비 |
|---|---|---|
| 서버 실행 + curl | HTTP 요청 → Controller → Service → 모델 응답 | 서버 터미널과 요청 터미널 |
| Live 테스트 | HTTP Controller를 거치지 않고 Service → 실제 OpenAI 호출 | 테스트 클래스 실행 |

첫 실습에서는 전체 흐름을 확인할 수 있는 1번을 권합니다. 키와 모델 설정은 두 경로 모두 필요하며 실제 API 사용량이 발생합니다.

## 1. 서버를 실행하고 HTTP 요청 보내기

저장소 루트에서 실행하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:bootRun
```

시작 로그에서 애플리케이션이 8091 포트로 시작했는지 확인합니다.
새 터미널을 열고 다음 명령을 실행합니다. 이 터미널에는 API 키를 입력할 필요가 없습니다.
키는 Spring 애플리케이션이 외부 모델을 호출할 때 사용합니다.

```bash
curl -i http://127.0.0.1:8091/api/chat \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw 'Spring의 의존성 주입을 초보자에게 두 문장으로 설명해줘.'
```

`--data-raw`가 본문을 보내므로 POST 요청이 됩니다. `-i`는 상태 코드와 헤더도 보여줍니다.
포트를 바꿨다면 curl 주소도 함께 바꿉니다.

다음은 **응답 형태 예시**이며 실제 모델 실행 결과를 복사한 것이 아닙니다.

```text
HTTP/1.1 200
Content-Type: text/plain;charset=UTF-8
...

의존성 주입은 객체가 필요한 다른 객체를 직접 만들지 않고 외부에서 전달받는 방식입니다.
Spring은 이 연결을 대신 관리해 코드의 결합도를 낮춰줍니다.
```

문장과 응답 시간은 모델·상황에 따라 달라집니다. 정확히 같은 문장이 나오는지가 성공 기준은 아닙니다.
상태 코드 200, 빈 문자열이 아닌 응답, 질문과 관련된 설명인지를 확인하세요.
200이어도 사실이 맞는지는 별도로 확인해야 합니다.

종료는 서버 터미널에서 Ctrl+C를 누릅니다.
## 2. 선택: Live 테스트로 Service에서 바로 호출하기

서버 실행과 curl 대신 `FirstChatOpenAiLiveTest`를 실행할 수도 있습니다.
이 테스트는 HTTP 서버를 띄우지 않고 Spring이 주입한 `ChatService`로 실제 OpenAI를 호출합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-first-chat:test --tests com.example.ailearning.FirstChatOpenAiLiveTest --rerun-tasks
```

`FirstChatOpenAiLiveTest`는 실제 OpenAI에 연결하므로 API 사용량이 발생합니다.
IntelliJ에서는 클래스 또는 메서드의 Run으로 실행합니다. 태그나 별도 허용 변수는 없습니다.
실제 응답이 비어 있지 않은지 확인하고 질문·응답을 출력하며, 고정 문장 일치를 검사하지 않습니다.
질문은 테스트 메서드에서 바꿀 수 있습니다. 키는 출력하지 않습니다.
필터 없이 이 모듈의 `test`를 실행하면 Mock과 Live 두 클래스가 모두 실행됩니다.
`--rerun-tasks`는 이전 실행 결과를 재사용하지 않고 다시 실행하는 옵션입니다.

[이전: 테스트 코드 읽기](02-test-code.md) · [다음: 질문만 바꾸는 실험](../02-first-call.md#5-질문만-바꿔-보기) · [02장 본문](../02-first-call.md)

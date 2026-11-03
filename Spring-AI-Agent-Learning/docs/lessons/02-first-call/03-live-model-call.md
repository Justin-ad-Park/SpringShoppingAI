# 02-3. 실제 모델 호출

[01장](../01-setup.md)에서 설명한 저장소 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다.
저장소 루트에서 실행하세요.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:bootRun
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
실제 모델에 한 번 질문하고 응답만 출력해 보고 싶다면 서버 실행 대신 별도 테스트도 사용할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:01-first-chat:test --tests com.example.ailearning.FirstChatOpenAiLiveTest --rerun-tasks
```

`FirstChatOpenAiLiveTest`는 실제 OpenAI에 연결하므로 API 사용량이 발생합니다.
IntelliJ에서는 클래스 또는 메서드의 Run으로 실행합니다. 별도 허용 변수는 없습니다.
필터 없이 이 모듈의 `test`를 실행하면 두 클래스가 모두 실행됩니다.

[이전: 테스트 코드 읽기](02-test-code.md) · [다음: 질문만 바꾸는 실험](../02-first-call.md#5-질문만-바꿔-보기) · [02장 본문](../02-first-call.md)

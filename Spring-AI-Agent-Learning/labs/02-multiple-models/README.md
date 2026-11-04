# 선택 실습: 두 제공 업체의 ChatClient

OpenAI와 Anthropic 모델을 각각의 이름이 있는 ChatClient로 구성하고 `@Qualifier`로 주입합니다.
다른 실습의 Java 코드에 의존하지 않습니다. Spring AI 버전은 상위 프로젝트의 2.0.2를 사용합니다.

[설명과 전체 코드 읽기](../../docs/lessons/02-first-call/04-multiple-models.md)

저장소 루트에서 로컬 서버를 사용하는 테스트만 선택해 실행하고 JAR을 빌드합니다:

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersContractTest :Spring-AI-Agent-Learning:labs:02-multiple-models:bootJar
```

실제 호출은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `ANTHROPIC_API_KEY`, `ANTHROPIC_MODEL`을 사용합니다.
키를 준비한 뒤 저장소 루트에서 실행하세요. 포트는 8092이며 실제 API 사용 요금이 발생할 수 있습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:bootRun
```

POST `/api/multi-chat/openai` 또는 `/api/multi-chat/anthropic`에 `Content-Type: text/plain`으로 질문을 보내세요.
`MultipleProvidersContractTest`는 두 외부 API를 로컬 고정 응답 서버로 대체하며 실제 키나 외부 네트워크 호출이 필요하지 않습니다.

## 실제 모델 Live 테스트

같은 테스트 패키지의 [MultipleProvidersLiveTest.java](src/test/java/com/example/ailearning/l02multiple/MultipleProvidersLiveTest.java)는
`MultipleModelsService`로 실제 모델을 호출합니다. 현재 OpenAI 테스트가 활성화되어 있고 Anthropic 예제는 주석 처리되어 있습니다. HTTP Controller는 거치지 않습니다.
질문과 업체별 응답을 출력하고 응답이 비어 있지 않은지 검사합니다. 고정 문장 일치나 답변 품질을 판정하는 테스트는 아닙니다.

루트 `.env`에 위의 네 값을 준비한 뒤 IntelliJ에서 클래스 또는 원하는 메서드의 Run으로 실행하세요.
두 클라이언트를 함께 구성하므로 메서드 하나만 실행해도 두 업체의 설정값이 필요합니다.
Gradle로 Live 클래스만 실행하려면 다음 명령을 사용합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersLiveTest --rerun-tasks
```

태그·실행 조건·별도 Live 태스크는 없습니다. **필터 없이 이 모듈의 `test`를 실행하면 로컬 테스트 3개와 활성화된 OpenAI Live 테스트가 실행되며 실제 API 사용량이 발생합니다.**
실제 API의 인증·모델 접근 권한·호출 제한·시간 초과로 Live 테스트가 실패할 수 있습니다.

## IntelliJ에서 Run 아이콘이 보이지 않을 때

저장소 루트의 Gradle 프로젝트를 연 상태에서 **Sync Gradle Changes** 또는 Gradle 창의 **Reload All Gradle Projects**를 실행합니다.
새 모듈을 추가한 뒤 동기화하지 않으면 이 폴더가 일반 디렉터리로만 표시되어 `src/test/java`의 테스트를 인식하지 못할 수 있습니다.
동기화 후 이 클래스가 `02-multiple-models.test` 모듈에 속하는지 확인하고 에디터의 Run으로 실행하세요.
테스트 클래스가 package-private인 것은 JUnit 5에서 정상이며, 실행을 위해 public으로 바꿀 필요는 없습니다.
같은 모듈의 Gradle 테스트를 IDE와 터미널에서 동시에 실행하면 결과 파일 쓰기가 충돌할 수 있으므로 한쪽에서만 실행합니다.

## 테스트 선택과 결과 확인

서버와 결과 출력은 [공통 test-support](../../test-support/README.md)를 사용합니다. 이 의존성은 테스트에만 적용됩니다. 각 케이스에서는 약속한 응답·도구 순서·벡터 값과 검증 내용을 읽으세요.

- `MultipleProvidersContractTest`: 로컬 고정 응답으로 정상·실패 처리를 재현합니다.
- `MultipleProvidersLiveTest`: 실제 OpenAI(Anthropic 예제는 현재 주석 처리되어 있습니다)에 연결합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.
- 콘솔에서 `[한글 테스트 메서드명]`과 응답·검증 대상 값을 함께 확인합니다. 출력과 별도로 `assertThat` 검증도 유지합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersContractTest
./gradlew :Spring-AI-Agent-Learning:labs:02-multiple-models:test --tests com.example.ailearning.l02multiple.MultipleProvidersLiveTest
```

IntelliJ에서는 원하는 테스트 클래스나 한글 메서드의 Run을 선택합니다. 클래스 필터 없이 모듈의 전체 `test`를 실행하면 Live도 포함됩니다.

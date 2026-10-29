# 쇼핑 통합 테스트 실행 안내

Java 21 / JUnit 5로 실제 세 서비스의 연동을 검증합니다.
아래 Gradle 명령은 모두 저장소 루트(`settings.gradle`이 있는 폴더)에서 실행합니다.
API 키와 Python은 필요하지 않으며 유료 OpenAI API를 호출하지 않습니다.

## 빠른 실행

```bash
# 전체 스모크 테스트: 세 서비스 자동 빌드 → 시작 → 검증 → 종료
./gradlew :integration-tests:smokeTest

# 로컬 GPT 대역과 실제 MCP 연결 경로 검증
./gradlew :integration-tests:gptPathTest

# 두 테스트 묶음을 함께 실행
./gradlew :integration-tests:smokeTest :integration-tests:gptPathTest
```

테스트마다 OS가 선택한 빈 포트를 사용해 기존 개발 서버와 분리합니다.
성공·실패와 관계없이 테스트가 실행한 프로세스를 역순으로 종료합니다.
기본 `./gradlew test`는 단위 테스트만 수행하며 이 통합 테스트는 실행하지 않습니다.

## MockIntegrationTest와 GptStubIntegrationTest의 차이

두 테스트의 가장 큰 차이는 **사용자 문장을 해석하고 검색을 실행하는 경로**입니다.
`MockIntegrationTest`는 Java 규칙으로 입력을 해석해 주요 업무 기능을 폭넓게 점검합니다.
`GptStubIntegrationTest`는 실제 GPT 연결 코드를 실행하되, OpenAI 대신 로컬 대역 서버가 정해진 응답을 반환합니다.
그 응답을 애플리케이션이 받아 업무 실행까지 연결하는지 집중적으로 검증합니다.

| 비교 항목 | `MockIntegrationTest` | `GptStubIntegrationTest` |
|---|---|---|
| 확인하려는 질문 | 주요 쇼핑 기능과 대화 상태가 정상 동작하는가? | GPT 요청·응답 처리와 도구 실행 연결이 정상 동작하는가? |
| 사용자 문장 해석 | Java의 규칙 기반 해석기 | Spring AI → 로컬 OpenAI 대역 → 구조화된 해석 결과 |
| 일반 상품 검색 | Java 코드가 MCP 검색을 직접 실행 | 대역의 도구 호출 요청 → 실제 MCP 검색 → 대역의 상품 ID 선택 |
| 실제 실행하는 서버 | 클라이언트·에이전트·MCP | 클라이언트·에이전트·MCP + OpenAI 대역 |
| 실제 OpenAI API 호출 | 없음 | 없음 |
| 검증 범위 | 주요 기능·오류·상태 격리 등 17건 | GPT 연결 경로를 확인하는 핵심 시나리오 3건 |

예를 들어 “새로 텀블러 3만원 이하 찾아줘”라는 같은 요청도 다음처럼 처리합니다.

```text
MockIntegrationTest
  사용자 요청 → Java 규칙으로 조건 해석 → MCP 검색 → 상품 응답

GptStubIntegrationTest
  사용자 요청 → GPT 연결 코드로 해석 요청 → 대역이 텀블러·3만원 조건 반환
  → GPT 연결 코드로 검색 요청 → 대역이 searchProducts 호출 지시
  → 실제 MCP 검색 → 대역이 검색 결과에서 상품 ID 선택 → 상품 응답
```

### MockIntegrationTest: 주요 기능과 상태 관리 점검

- 최초 무료배송 추천, 카테고리 변경, 조건 누적, 혜택 조회 후 검색 유지, 예산 해제, 새 발화 검색, 새 검색 플래그를 각각 독립된 테스트로 검증
- 일반 상품 검색
- 이미 무료배송인 장바구니, 빈 장바구니, 조건에 맞는 상품 없음, 예산 부족
- 배송 정책 조회
- 다른 브라우저 세션 간 상태 격리
- 잘못된 입력의 HTTP 400, 오래된 대화 버전의 HTTP 409
- 같은 세션에서 여러 대화를 동시에 요청했을 때 결과 분리

### GptStubIntegrationTest: GPT 연동 코드 점검

1. **의도 해석·조건 유지:** 대역이 반환한 3만 원 예산을 적용하고, 후속 “음료만” 요청에도 같은 작업과 예산을 유지하는지 확인합니다.
2. **도구 실행 루프:** 대역이 요청한 `searchProducts`를 실제 MCP에서 실행하고, 그 결과에서 선택한 상품을 반환하는지 확인합니다.
3. **잘못된 선택의 복구:** 대역이 일부러 존재하지 않는 상품 ID를 반환하면 이를 거부하고 Java 워크플로로 한 번 대체 검색하는지 확인합니다.

작업·예산 유지가 양쪽에 등장하는 이유는 해석 결과가 만들어지는 경로가 다르기 때문입니다.
`MockIntegrationTest`가 통과해도 GPT API의 요청·응답 형식이나 도구 호출 연결에 문제가 있으면 `GptStubIntegrationTest`는 실패할 수 있습니다.
반대로 GPT 연결 경로의 세 시나리오가 통과해도 세션 격리·입력 오류·동시 대화까지 검증한 것은 아닙니다.
따라서 두 테스트 묶음을 함께 실행합니다.

두 테스트 모두 **실제 GPT가 자연어를 정확하게 이해하는지**는 검증하지 않습니다.
또한 MCP 서버는 실제로 실행하지만 상품·장바구니 데이터는 프로젝트의 고정 테스트 데이터를 사용합니다.

### 테스트 분리 원칙

하나의 긴 대화에 모든 검증을 넣으면 앞 단계 실패로 뒤쪽 검증이 실행되지 않고,
어떤 기능이 실패했는지 파악하기 어렵습니다. 따라서 검증할 동작별로 테스트를 분리합니다.
예산 해제 테스트는 예산이 설정된 대화를 직접 준비하고, 새 검색 테스트는 이전 검색 조건을 직접 준비합니다.
각 테스트 안에서는 **준비 → 검증 대상 요청 → 결과 확인** 흐름으로 읽으면 됩니다.

같은 동작의 결과인 상품 목록·작업 ID·조건 등을 함께 확인하므로 `assertThat`이 여러 개일 수 있습니다.
반면 카테고리 변경과 예산 해제처럼 목적이 다른 동작은 별도 테스트로 나눕니다.
후속 조건 유지처럼 연속 요청 자체가 검증 대상인 경우에는 해당 요청들만 한 테스트에 남깁니다.
각 테스트는 필요한 대화 상태를 직접 준비하므로 다른 테스트의 실행 순서에 의존하지 않습니다.

클래스 이름은 변경했지만 기존 실행 명령인 `smokeTest`, `gptPathTest` 등은 그대로 사용할 수 있습니다.

## 실행 중인 mock 서버에 테스트하기

```bash
# 기본 주소: http://127.0.0.1:8080
./gradlew :integration-tests:smokeExternalTest

# 사용자 지정 주소
./gradlew :integration-tests:smokeExternalTest -PsmokeBaseUrl=http://127.0.0.1:9080
```

서버는 미리 mock 모드로 실행되어 있어야 합니다.
`SMOKE_BASE_URL` 환경변수도 지원하며 `-PsmokeBaseUrl`이 우선합니다.
이 모드는 서버를 빌드하거나 시작·종료하지 않습니다.

## 테스트 후 브라우저에서 확인하기

```bash
./gradlew :integration-tests:smokePreview --console=plain
```

스모크 테스트에 성공하면 출력된 주소에서 같은 서버를 확인할 수 있습니다.
터미널에서 Enter를 누르면 서버를 종료합니다.

## IDE에서 실행하기

1. 프로젝트를 Gradle 프로젝트로 불러오고 JDK 21을 설정합니다.
2. 저장소 루트에서 `./gradlew bootJar`로 세 서비스의 JAR를 빌드합니다.
3. `MockIntegrationTest` 또는 `GptStubIntegrationTest`를 JUnit으로 실행합니다. 개별 메서드도 실행할 수 있습니다.
4. 실행 설정의 작업 디렉터리를 `integration-tests`로 설정합니다.
   다른 디렉터리를 사용하면 VM 옵션에 `-Dshopping.root=<저장소 절대 경로>`를 지정합니다.

IDE에서 Gradle에 위임한 기본 `test` 태스크는 통합 테스트를 제외합니다.
IDE의 JUnit 실행기를 사용하거나 위의 전용 Gradle 태스크를 실행하세요.

## 결과와 오류 확인

- JUnit 결과: 터미널 또는 IDE의 테스트 창
- HTML 보고서: `integration-tests/build/reports/tests/<태스크 이름>/index.html`
- 서비스 로그: 오류 메시지에 표시된 `.run/*-java-test-<포트>.log`

기동 실패 시 서비스 로그를 확인하세요. IDE에서 JAR를 찾지 못하면 `bootJar`를 다시 실행합니다.
`smokePreview`는 JUnit 실행 요약을 터미널에 출력합니다.

## 패키지 구성

```text
com.example.shopping.integration
├── README.md
├── MockIntegrationTest.java
├── GptStubIntegrationTest.java
└── support
    ├── IntegrationSuite.java
    ├── ChatClient.java
    ├── TestMode.java
    ├── TestEnvironment.java
    ├── OpenAiStub.java
    └── SmokePreview.java
```

| 지원 클래스 | 책임 |
|---|---|
| `IntegrationSuite` | 클래스별 환경과 테스트별 쿠키 세션의 준비·정리 |
| `ChatClient` | HTTP 통신, 쿠키 세션, 대화 ID와 버전, 응답 비교 도우미 |
| `TestMode` | MOCK / GPT_STUB / EXTERNAL 실행 전략 |
| `TestEnvironment` | 서비스 기동, 준비 대기, 자원 역순 정리 |
| `OpenAiStub` | OpenAI 호환 고정 응답, 호출 기록, 대역 내부 검증 |
| `SmokePreview` | 스모크 검증 후 브라우저 미리보기와 Enter 종료 |

새 검증은 상위 패키지의 테스트 클래스에 추가합니다.
통신·서버 관리 등 공통 도구는 `support`에 둡니다.
기존 Python 파일은 저장소의 `scripts/old_backup/`에 비교용으로 보관합니다.

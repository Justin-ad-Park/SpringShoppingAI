# 설정 및 실행

## 사용자가 입력할 정보

| 항목 | 필수 여부 | 설정 위치 |
|---|---|---|
| Java 21 이상 | 필수 | `java -version`, 필요하면 `JAVA_HOME` 지정 |
| Python 3 / curl | 기존 서비스 실행 셸 스크립트에 필요(Java 통합 테스트에는 불필요) | `python3 --version`, `curl --version` |
| `OPENAI_API_KEY` | GPT 모드에서 필수 | 루트 `.env` 또는 터미널 환경변수 |
| `OPENAI_MODEL` | 선택 | 기본 `gpt-4.1-mini`. 해당 API 프로젝트에서 사용 가능한 Tool Calling 모델 |
| DB / 쇼핑몰 계정 | 불필요 | 데이터가 코드에 고정되어 있음 |

첫 빌드에는 Gradle 배포 서버와 Maven Central 네트워크 접근이 필요합니다.
GPT 모드 실행 중에는 OpenAI API 네트워크 접근이 필요합니다.
브라우저에 API 키를 입력하거나 전달하지 않습니다. 키는 `shopping-agent`에서만 사용합니다.

## 1. 키 없이 MCP 연결부터 확인

프로젝트 루트에서 실행합니다.

```bash
./scripts/run-all.sh --mock
```

<http://127.0.0.1:8080>을 열고 ‘무료배송 상품 추천’을 선택한 뒤 보내기를 누릅니다.
MCP 도구 호출 기록을 펼쳐 실제 HTTP MCP 호출과 8,000원 부족 금액을 확인합니다.

**Mock 모드는 정해진 예시 표현을 해석하고 Java 업무가 MCP를 호출합니다.**
`음료 카테고리에서만`, `예산은 3만원`, `예산 해제`, `새로 텀블러 찾아줘` 같은 문장은 지원하지만
GPT의 자연어 이해나 자율적인 도구 선택을 검증하지는 않습니다.

## 2. OpenAI API 키 입력

[API Keys](https://platform.openai.com/api-keys)에서 사용할 프로젝트의 API 키를 생성합니다.
API 프로젝트에 사용 가능한 결제/사용량과 모델 접근 권한이 있어야 합니다.
키를 채팅이나 Git에 올리지 마세요.

```bash
cp .env.example .env
```

텍스트 편집기로 `.env`를 열고 다음 값을 입력합니다.

```dotenv
OPENAI_API_KEY=여기에_발급받은_API_키
OPENAI_MODEL=gpt-4.1-mini
```

`.env`는 `.gitignore`에 등록되어 있습니다. 실행 스크립트가 해당 파일을 환경변수로 불러옵니다.
Spring Boot를 IDE나 `bootRun`으로 직접 실행하면 `.env`를 자동 로드하지 않으므로 실행 설정에 직접 환경변수를 추가하세요.
별도 파일 없이 터미널에서 `export OPENAI_API_KEY=...`로 설정해도 됩니다.
`.env`가 있으면 파일의 값이 기존 환경변수를 덮어씁니다.

공식 안내: [환경변수 설정](https://developers.openai.com/api/docs/quickstart),
[서버에서 키를 보관하는 인증 지침](https://developers.openai.com/api/reference/overview).

## 3. GPT 모드 실행

실행 중인 Mock 모드는 먼저 Ctrl+C로 종료합니다.

```bash
./scripts/run-all.sh
```

실행 스크립트는 빌드 → MCP 서버 준비 → 에이전트 준비 → 챗봇 Client 준비 순으로 진행합니다.
완료 후 **세 서비스는 동시에 실행**됩니다. 첫 빌드 후 코드 변경이 없으면 `--skip-build`로 재사용할 수 있습니다.

```bash
./scripts/run-all.sh --skip-build
```

웹 화면 상단이 `GPT · MCP 도구 연결`인지 확인합니다.

추천 질문:

- 내 장바구니를 확인해서 무료배송이 가능하도록 추가 구매하기 적합한 상품을 추천해줘.
- 무료배송을 채울 상품을 추천해줘. 추가 예산은 10,000원 이하야.
- 무료배송을 채울 상품을 추천해줘. 추가 예산은 7,000원 이하야.
- 내 보유 쿠폰과 적립금을 조회해줘.
- 무료배송 조건이 뭐야?
- 음료 카테고리에서만 추천해줘.
- 예산은 3만원까지로 변경해줘.
- 새로 3만원 이하 텀블러 찾아줘.

같은 대화에서 카테고리·예산 조건을 변경하면 기존 검색 목적과 나머지 사용자 조건을 유지합니다.
‘새 검색 · 조건 초기화’ 버튼 또는 ‘새로’라는 명시적 요청은 이전 조건을 상속하지 않습니다.
무료배송 추천은 Java 워크플로우로 실행하며 일반 검색은 GPT 도구 선택 또는 직접 실행을 사용합니다.
일반 검색 카드에는 배송 견적이 없고, 무료배송 카드만 `quoteCart` 검증 결과를 포함합니다.
‘그중 두 번째’ 같은 과거 결과 지칭이나 복합 업무 자동 실행은 아직 지원하지 않습니다.

단기 메모리는 Agent 재시작 시 사라집니다. 만료/버전 충돌(409) 안내가 나오면 새 검색 버튼을 누르세요.
실제 여러 사용자의 로그인 인증은 없으며 세션 쿠키는 익명 데모 대화를 구분하는 용도입니다.

## 4. IDE / 개별 실행

프로젝트 루트의 `settings.gradle`을 Gradle 프로젝트로 엽니다. Java 21을 지정합니다.

| 실행 순서 | Main 클래스 | 환경변수 |
|---|---|---|
| 1 | `com.example.shopping.mcp.McpApplication` | 없음 |
| 2 | `com.example.shopping.agent.AgentApplication` | `OPENAI_API_KEY`, 선택 `OPENAI_MODEL` |
| 3 | `com.example.shopping.client.ClientApplication` | 없음 |

Mock 실행 시 에이전트에만 `SPRING_PROFILES_ACTIVE=mock`을 지정합니다.
또는 세 터미널에서 순서대로 실행합니다.

```bash
./gradlew :shopping-mcp:bootRun
./gradlew :shopping-agent:bootRun
./gradlew :chatbot-client:bootRun
```

위 명령은 각각 별도의 터미널에서 사용해야 합니다.
MCP 서버가 준비되기 전에 에이전트를 실행하면 MCP 초기화에 실패할 수 있습니다.

## 포트 변경

```bash
CLIENT_PORT=9080 AGENT_PORT=9081 MCP_PORT=9082 ./scripts/run-all.sh --mock
```

스크립트가 내부 URL도 함께 맞춥니다. IDE에서는 `AGENT_URL`, `MCP_URL`도 직접 맞추세요.
모든 서버는 기본적으로 `127.0.0.1`에 바인딩합니다.

## 종료와 로그

통합 실행 터미널에서 Ctrl+C를 누르면 스크립트가 시작한 세 Java 프로세스가 종료됩니다.
기존에 실행 중인 다른 프로세스는 종료하지 않습니다.

실행한 터미널을 찾기 어렵다면 프로젝트 루트에서 종료 스크립트를 실행합니다.
Mock/GPT 모드 모두 같은 명령을 사용합니다.

```bash
# 대상만 확인 (실제 종료 없음)
./scripts/stop-all.sh --dry-run

# 이 프로젝트의 세 서비스를 찾아 정상 종료 요청
./scripts/stop-all.sh

# 정상 종료 요청 후 15초가 지나도 남아 있는 서비스 강제 종료
./scripts/stop-all.sh --force
```

PID나 포트를 직접 입력할 필요가 없습니다. `ps`와 `lsof`로 Java 프로세스의 작업 경로와
JAR 경로를 대조하여 이 체크아웃의 `build/libs` 서비스만 선택합니다.
Gradle `bootRun`/IDE 실행도 Main 클래스와 이 프로젝트의 `build/classes` 클래스패스가
확인되면 종료합니다. IDE의 별도 출력 디렉터리나 Java 인자 파일(`@argfile`) 실행은
자동 탐지하지 못할 수 있으므로 해당 IDE의 Stop 버튼을 사용하세요.
포트만 같은 다른 프로그램이나 다른 체크아웃의 서비스는 종료하지 않습니다.
이미 종료되어 있다면 안내 메시지만 출력합니다.

GPT 모드로 전환하는 예:

```bash
./scripts/stop-all.sh
./scripts/run-all.sh
```

- `.run/build.log`: 빌드 로그
- `.run/shopping-mcp.log`: 도구 서버 로그
- `.run/shopping-agent.log`: 에이전트 시작 로그
- `.run/chatbot-client.log`: 웹 Client 로그

## 문제 해결

| 증상 | 확인할 내용 |
|---|---|
| `OPENAI_API_KEY` 안내 후 종료 | `.env` 키 입력 또는 `--mock` 사용 |
| 요청 실패/502 | 서비스 실행 상태, 키, 모델 접근 권한, API 결제/사용량, 응답 형식 확인 |
| 키는 맞는데 GPT 모드 실패 | 선택 모델이 Tool Calling 및 구조화 응답을 지원하는지 확인 |
| MCP 초기화 실패 | 8082 서비스 준비 여부와 `MCP_URL` 확인. MCP 서버부터 시작 |
| 포트 사용 중 | 기존 실행 종료 또는 세 포트 변경 |
| Gradle 다운로드/DNS 실패 | Gradle/Maven Central 접근, 회사 프록시·인증서 설정 확인 |
| Java 버전 오류 | `java -version` 및 `JAVA_HOME`이 JDK 21 이상인지 확인 |
| 이미 무료배송/빈 장바구니에서 카드 없음 | 정상 동작. 화면의 샘플 장바구니 선택 확인 |
| 예상과 다른 GPT 답변 | 도구 호출 내역 확인. 환각 ID는 응답 검증에서 거부. Mock은 고정 흐름 비교용 |

에러 응답에는 원본 OpenAI 오류 본문이나 인증 헤더를 전달하지 않습니다.
실제 GPT 요청은 API 사용료가 발생할 수 있습니다.

## 옵션 없이 사용하는 단축 스크립트

```bash
./scripts/running-list.sh  # stop-all.sh --dry-run: 실행 중인 서비스만 확인
./scripts/mock-run-all.sh  # run-all.sh --mock: Mock 모드로 실행
```

추가 인자는 원래 스크립트에 전달합니다. 예: `./scripts/mock-run-all.sh --skip-build`.
종료는 `./scripts/stop-all.sh`, GPT 모드 실행은 `./scripts/run-all.sh`를 사용합니다.

## 실행 방식과 단기 메모리 설정

Agent의 `application.yml`에서 다음 설정을 조정할 수 있습니다.

```yaml
shopping:
  catalog-execution: agent  # GPT 모드의 일반 검색. workflow면 처음부터 Java 직접 검색
  memory:
    ttl: 30m
    max-conversations: 1000
```

IDE 또는 직접 JAR 실행 시 `--shopping.catalog-execution=workflow` 등으로 덮어쓸 수 있습니다.
Mock 모드는 항상 제한적인 문장 해석 + 직접 실행을 사용하고, 무료배송 추천은 두 모드 모두 워크플로우입니다.

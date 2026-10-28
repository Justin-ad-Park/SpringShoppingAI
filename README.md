# Spring Shopping AI PoC

독립 실행하는 세 개의 Spring Boot 서브 프로젝트로 쇼핑 에이전트를 시연합니다.

| 프로젝트 | 기본 포트 | 역할 |
|---|---|---|
| `chatbot-client` | 8080 | 채팅 화면, 상품 카드, 요청 프록시 |
| `shopping-agent` | 8081 | Spring AI + GPT, MCP 도구 선택/실행, 응답 검증 |
| `shopping-mcp` | 8082 | Mock 쇼핑 데이터 및 7개 MCP 도구 |

Java 21, Spring Boot 3.5.12, Spring AI 1.1.2, Gradle Wrapper 8.13 사용.

```bash
# Java 21+, Python 3, curl 필요. API 키 없이 실제 MCP 연결을 확인
./scripts/run-all.sh --mock

# GPT가 직접 MCP 도구를 선택하고 호출하는 모드
# .env.example을 .env로 복사하고 OPENAI_API_KEY를 입력한 후
./scripts/run-all.sh
```

브라우저에서 <http://127.0.0.1:8080>을 열고 질문합니다.

> 내 장바구니를 확인해서 무료배송이 가능하도록 추가 구매하기 적합한 상품을 추천해줘.

기본 장바구니 42,000원 / 무료배송 기준 50,000원 / 부족 금액 8,000원. 기본 추가 상품 검색 범위는 **8,000~15,000원**입니다.
상품 카드는 한 상품을 1개씩 추가하는 **서로 독립된 대안**입니다.
Mock 모드는 고정 흐름이며 GPT의 자연어 이해/도구 선택을 검증하지 않습니다.
GPT 모드는 매 요청 독립 실행하며 이전 대화를 기억하지 않습니다.

- [설정·키 입력·실행·문제 해결](docs/SETUP.md)
- [서비스 구성·요청 흐름](docs/ARCHITECTURE.md)
- [Mock 데이터·도구·제약](docs/MOCK-DATA.md)
- [테스트·검증 절차](docs/TESTING.md)

```bash
./gradlew test bootJar
# --mock으로 서비스를 띄운 별도 터미널에서
python3 scripts/smoke-test.py
```

브라우저 검증 화면: [PoC 미리보기](docs/images/poc-preview.png)

서비스 종료 (Mock/GPT 공통):

```bash
./scripts/stop-all.sh --dry-run  # 대상 확인
./scripts/stop-all.sh            # 정상 종료
```

자주 사용하는 옵션은 아래 스크립트로도 실행할 수 있습니다.

```bash
./scripts/running-list.sh  # 실행 중인 서비스 확인 (종료하지 않음)
./scripts/mock-run-all.sh  # Mock 모드로 세 서비스 실행
./scripts/mock-run-all.sh --skip-build  # 기존 빌드 사용
```
# SpringShoppingAI

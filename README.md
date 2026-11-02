# Spring Shopping AI PoC

입문 교육 자료: [Spring-AI-Agent-Learning](Spring-AI-Agent-Learning/README.md) — 기초 개념·첫 호출·프롬프트를 순서대로 학습합니다.

기존 실습 프로젝트: [Spring-AI-Tutor 실행·테스트](Spring-AI-Tutor/README.md) · [15개 단원과 58개 케이스](Spring-AI-Tutor/docs/02-curriculum.md).

세 개의 Spring Boot 서비스를 실행해 일반 상품 검색, 장바구니 무료배송 추천, 후속 조건 변경을 시연합니다.

| 서브 프로젝트 | 포트 | 역할 |
|---|---|---|
| chatbot-client | 8080 | 채팅·상품 카드·검색 상태·MCP 호출 기록 |
| shopping-agent | 8081 | GPT 의도 해석, 업무별 실행, 단기 조건 메모리 |
| shopping-mcp | 8082 | 범용 상품 검색·장바구니·배송 견적·혜택 Mock 도구 |

Java 21 / Spring Boot 4.0.9 / Spring AI 2.0.2 / Gradle Wrapper 8.14.5.

```bash
# API 키 없이 예시 표현으로 시연
./scripts/mock-run-all.sh

# .env.example을 .env로 복사해 OPENAI_API_KEY 입력 후 GPT 실행
./scripts/run-all.sh
```

브라우저: <http://127.0.0.1:8080>

다음 순서로 질문해 보세요.

1. `무료배송을 채울 상품을 추천해줘.` → 기본 8,000~15,000원에서 추천.
2. `음료 카테고리에서만 추천해줘.` → 무료배송 목적은 유지하고 카테고리 변경.
3. `예산은 3만원까지로 변경해줘.` → 명시 예산 30,000원 유지.
4. `생활 카테고리에서만 추천해줘.` → 텀블러 후보, 무료배송 견적 확인.
5. `새로 3만원 이하 텀블러 찾아줘.` → 이전 조건을 상속하지 않는 일반 검색.

‘새 검색 · 조건 초기화’ 버튼도 이전 조건을 상속하지 않는 검색을 시작합니다.
전체 대화 대신 활성 업무와 사용자 조건만 단기 보관합니다(기본 비활성 30분).
Mock은 정해진 표현만 지원하며 GPT의 언어 이해 품질을 검증하는 모드는 아닙니다.

```bash
./scripts/running-list.sh
./scripts/stop-all.sh
./scripts/run-all.sh --skip-build  # 코드 변경이 없을 때만
```

문서:

- [설정·API 키·실행](docs/SETUP.md)
- [구성과 책임·멀티턴 처리](docs/ARCHITECTURE.md)
- [Mock 데이터·MCP 계약](docs/MOCK-DATA.md)
- [테스트와 한계](docs/TESTING.md)
- [Spring AI 2.0.2 통일·교육 프로젝트 이동과 검증 결과](docs/SPRING-AI-2-MIGRATION.md)
- [SOLID·패턴 적용과 구현 결정](doc_tasks/2026-10-28-solid-and-implementation.md)

```bash
./gradlew test bootJar
./gradlew :integration-tests:smokeTest    # Java/JUnit: 세 서비스 자동 실행/종료
./gradlew :integration-tests:gptPathTest  # 로컬 GPT 대역, API 키/비용 없이 연결 검증
```

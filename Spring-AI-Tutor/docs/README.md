# Spring AI Tutor 설계 문서

작성일: 2026-10-29 · 상태: 구현 완료, 선택 인프라는 설정 후 실행

## 목적과 이번 산출물

초급 Java/Spring 개발자가 **쇼핑몰 AI 쇼핑 어시스턴트**를 조금씩 완성하면서 Spring AI의 기능, 호출 흐름, 한계와 테스트 방법을 코드로 익히는 하위 프로젝트다. 각 실습은 하나의 클래스 또는 명확한 메소드와 대응 테스트를 갖는다.

초기 설계 이후 구현을 진행해 `settings.gradle`에 모듈을 등록했다. **실행 방법은 [프로젝트 README](../README.md)**, 설계와 실제 구현의 차이는 [구현 현황](05-implementation.md)을 기준으로 확인한다. 01~04 문서의 예정 코드 구조·태스크 중 구체화하면서 달라진 사항이 있다.

## 문서 탐색

| 문서 | 내용 |
|---|---|
| [프로젝트 구성과 분리 결정](01-architecture.md) | 현재 코드 분석, 클라이언트 선택, 디렉터리, 의존성과 실행 모드 |
| [커리큘럼과 기능별 케이스](02-curriculum.md) | 학습 순서, 클래스·메소드, 질문, 정상·실패 케이스와 테스트 매핑 |
| [자동화 테스트 설계](03-testing.md) | 무비용 테스트, 실제 모델 평가, fixture, 통과 기준과 CI |
| [웹 학습실과 구현 순서](04-web-and-roadmap.md) | 대화형 실습 UX, API/SSE 계약, 단계별 완료 조건 |
| [구현 현황과 차이](05-implementation.md) | 실제 패키지·태스크·선택 기능·검증 범위 |
| [학습 구조 개선안](06-learning-structure-proposal.md) | Java 단원별 패키지, support 배치, 주석·코드·테스트 재구성 제안 |
| [프런트엔드 코드 읽기](07-frontend-code-guide.md) | 역할별 JS 모듈, 함수명과 질문 처리 흐름 |

## 권장 구성

같은 저장소에 `Spring-AI-Tutor`를 독립 실행 가능한 Gradle 하위 프로젝트로 둔다. 학습 전용 웹클라이언트는 이 프로젝트의 정적 리소스로 제공한다. 처음에는 Tutor 서버 하나로 시작하고, MCP 단원에서만 기존 `shopping-mcp`를 선택적으로 연결한다.

기존 `chatbot-client`를 그대로 함께 쓰면 상품 카드 표현은 재사용하기 쉽지만, 수업 선택·프롬프트 비교·메모리·검색 근거·스트리밍을 관찰하는 학습 도구가 별도로 필요하다. 따라서 **학습 실행 경로는 분리하고, 기존 화면의 작은 표현 함수와 디자인을 참고**한다. 별도 Git 저장소까지 나누는 것은 지금 필요하지 않다.

## 학습 여정

```mermaid
flowchart LR
    A[텍스트로 답하기] --> B[조건을 객체로 추출]
    B --> C[대화 기억과 Advisor]
    C --> D[상품과 배송 도구]
    D --> E[문서 검색과 RAG]
    E --> F[MCP로 외부 연결]
    F --> G[평가와 관측]
    G --> H[쇼핑 어시스턴트 통합]
    H --> I[선택: 이미지·음성·생성]
```

기본 과정은 00~12단원, 통합 실습은 13단원, 선택 확장은 14단원이다. 실습마다 `문제 → 최소 코드 → 호출 흐름 관찰 → 테스트 → 실패 실험 → 쇼핑 적용` 순서를 반복한다. 모든 기능을 처음부터 하나의 거대한 챗봇에 넣지 않는다.

## 참고 자료를 활용하는 방법

[참고 글](https://imdeepskyblue.tistory.com/84)은 ChatClient, Advisor, ChatMemory, VectorStore, ETL, Tool Calling, Structured Output, Embedding, MCP, Evaluation을 개괄한다. 이 문서는 그 설명 순서나 예제를 복제하지 않고, 각 기능을 쇼핑 질문과 검증 가능한 실습으로 재구성한다. 프롬프트, 스트리밍, 관측, 장애 처리, 멀티모달도 학습 범위에 추가한다.

기능 관계는 다음처럼 정확히 구분한다.

- 모델은 도구 호출을 요청하고 애플리케이션이 도구를 실행한다. [공식 Tool Calling 문서](https://docs.spring.io/spring-ai/reference/api/tools.html)
- ChatClient의 동기 응답, 구조화 변환, 스트리밍은 별도 실습으로 비교한다. [공식 ChatClient 문서](https://docs.spring.io/spring-ai/reference/api/chatclient.html)
- 모델을 이용한 평가는 보조 품질 신호다. 사실성 전체를 증명하는 테스트로 취급하지 않는다. [공식 평가 문서](https://docs.spring.io/spring-ai/reference/api/testing.html)

## 버전 원칙

현재 저장소는 Java 21 / Spring Boot 4.0.9 / Spring AI BOM 2.0.2 / Gradle 8.14.5를 공통 사용합니다. 입문 자료와 기존 쇼핑·MCP 예제의 버전을 맞추기 위해 2026-11-02에 마이그레이션했습니다. 버전은 저장소 루트 build.gradle과 Gradle Wrapper에서 관리합니다. 1.x 예제의 옵션·OpenAI 어댑터 생성·도구 루프 코드를 그대로 섞지 않고, 2.0.2 API와 테스트를 기준으로 확인합니다.

공식 기능 범위 확인: [Spring AI Reference](https://docs.spring.io/spring-ai/reference/). 이 설계의 클래스명·API 경로·데이터·태스크는 프로젝트 제안이며 Spring AI가 제공하는 내장 기능 이름과 구분한다.

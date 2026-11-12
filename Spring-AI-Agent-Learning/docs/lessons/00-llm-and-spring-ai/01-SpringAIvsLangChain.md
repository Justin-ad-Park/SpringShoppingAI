# Spring AI와 LangChain: 어떤 기준으로 선택할까?

00장 선택 읽기입니다. [00장](../00-llm-and-spring-ai.md)에서 모델과 애플리케이션의 역할을 구분한 뒤 읽는 것을 권장합니다.
예상 시간은 20~30분이며, 코드 실행이나 API 키는 필요 없습니다.

## 이번 수업의 목표

같은 LLM 기능을 만들 때 Spring AI와 LangChain이 각각 무엇을 제공하는지 설명합니다.
팀의 언어·기존 서비스·처리 흐름·운영 조건을 기준으로 하나를 선택하거나, 두 기술을 나누어 사용하는 이유를 제시합니다.

**확인 기준일: 2026-11-12.** Spring AI는 이 저장소의 [build.gradle](../../../../build.gradle)에 선언된 2.0.2와 공식 참조 문서를 기준으로 설명합니다.
LangChain은 Python·JavaScript/TypeScript 공식 문서와 공개 API의 릴리스 정책을 참고합니다.
통합 대상과 API는 달라질 수 있으므로 도입 시 사용할 버전의 지원 기능을 다시 확인합니다.

## 1. 같은 문제에서 출발하기

쇼핑몰에 “반품 규정을 찾아 설명하고, 주문 상태를 확인하는 상담 기능”을 추가한다고 생각해 봅시다.
다음 작업이 필요합니다.

1. 사용자의 질문과 답변 지침을 모델에 보냅니다.
2. 반품 규정 문서에서 질문에 관련된 내용을 검색합니다.
3. 필요하면 주문 조회 API를 호출합니다.
4. 조회 결과를 모델에 전달해 답변을 만들고, 호출 시간과 실패 원인을 기록합니다.

두 프레임워크 모두 이런 애플리케이션을 만드는 데 사용할 수 있습니다.
모델은 입력을 해석하고 답변이나 도구 호출 요청을 생성합니다. 문서를 검색하고 주문 API를 실제 실행하는 주체는 애플리케이션입니다.
어떤 프레임워크를 선택하더라도 주문 접근 권한과 업무 규칙은 애플리케이션에서 검사해야 합니다.

먼저 아래 용어를 구분합니다.

| 용어 | 이 문서에서의 의미 |
|---|---|
| 프롬프트 | 모델에 전달할 질문·지침·참고 자료 등을 담은 입력 |
| 파서 | 응답 문자열을 읽어 원하는 값이나 객체로 변환하는 구성요소 |
| RAG (Retrieval-Augmented Generation, 검색 증강 생성) | 관련 자료를 검색해 모델 입력에 함께 넣고, 그 자료를 바탕으로 답변을 생성하는 방식 |
| 임베딩 | 텍스트 등의 의미를 검색에 사용할 수치 벡터로 표현하는 것 |
| 벡터스토어 | 벡터와 관련 문서를 저장하고 유사도 기반 검색을 제공하는 저장소 |
| 리트리버 | 질문에 관련된 문서를 가져오는 구성요소. 벡터 검색 외의 방식도 사용할 수 있음 |
| 도구 호출 | 모델이 함수 이름과 인자를 요청하면 애플리케이션이 해당 함수를 실행하고 결과를 돌려주는 방식 |
| Workflow / Agent | 코드가 실행 순서를 정하는 흐름 / 모델이 다음 도구나 행동을 선택하는 흐름 |
| 오케스트레이션 | 여러 단계의 실행 순서·분기·반복·상태를 조정하는 것 |
| 트레이싱 / 평가 / 모니터링 | 요청의 실행 경로 추적 / 정해진 기준으로 결과 검사 / 운영 중 지표와 오류 관찰 |

## 2. 각각의 용도와 주요 기능

### 2.1. LangChain: 모델·데이터·도구를 조합하는 프레임워크

**LangChain(랭체인)**은 LLM 기반 애플리케이션과 에이전트를 구성하는 오픈소스 프레임워크입니다.
Python과 JavaScript/TypeScript를 지원하며, 챗봇·RAG·에이전트·문서 요약 등에 사용할 수 있습니다.
현재 LangChain의 에이전트는 LangGraph를 기반으로 동작합니다. [Python 개요](https://docs.langchain.com/oss/python/langchain/overview), [JS/TS 개요](https://docs.langchain.com/oss/javascript/langchain/overview)

LangChain은 널리 알려진 선택지 중 하나입니다. 다만 “AI 생태계의 사실상 표준”이라고 단정할 공통 기준은 없습니다.
직접 모델 SDK를 사용하거나 다른 프레임워크를 선택하는 방법도 있습니다.

| 기능 | 역할과 사용 예 |
|---|---|
| 체인과 LCEL | 프롬프트 → 모델 → 파서를 연결해 입력부터 결과 변환까지 구성합니다. LCEL(LangChain Expression Language)은 실행 가능한 구성요소인 Runnable을 조합하는 방식입니다. 예를 들어 반품 규정 요약 요청을 만들고 응답을 문자열로 꺼내는 흐름을 연결합니다. |
| RAG 구성요소 | 문서 로더로 자료를 읽고, 텍스트를 분할하고, 임베딩을 만들어 벡터스토어에 저장합니다. 리트리버가 질문에 관련된 문서를 가져오면 이를 모델 입력에 포함합니다. |
| 에이전트와 도구 호출 | 모델이 사용할 도구와 인자를 선택하고 애플리케이션이 실행합니다. 예를 들어 주문 조회 결과에 따라 배송 조회가 추가로 필요한지 모델이 판단하게 구성할 수 있습니다. |
| LangGraph | 상태를 가진 실행을 그래프로 구성하는 별도 오케스트레이션 프레임워크·런타임입니다. 분기·루프·상태 저장·중단 후 재개·사람의 확인 단계를 구성하며, 여러 에이전트가 협력하는 흐름에도 사용할 수 있습니다. |
| LangSmith | 요청 경로 추적, 결과 평가, 운영 모니터링을 제공하는 별도 플랫폼입니다. 어느 단계의 검색이나 모델 응답이 잘못됐는지 조사할 때 사용합니다. |
| 다양한 통합 | 모델, 벡터DB, 문서 로더, 외부 도구 등의 연결을 제공합니다. 공식 Python 통합 목록은 전체 범주를 합쳐 1,000개 이상으로 소개하지만, 모델만 1,000개라는 뜻은 아니며 Python과 JS/TS의 지원 범위도 같지 않습니다. |

LCEL과 검색 구성요소는 [Runnable API](https://reference.langchain.com/python/langchain-core/runnables)와 [통합 목록](https://docs.langchain.com/oss/python/integrations/providers/overview)에서 확인할 수 있습니다.
LCEL로 파이프라인을 조합하는 방식과, 에이전트의 도구 선택 루프는 구분해서 이해해야 합니다.

LangGraph는 단일 에이전트나 코드로 정한 Workflow에도 사용할 수 있고, LangChain 없이 직접 사용할 수도 있습니다.
LangSmith 역시 LangChain 전용은 아니며 여러 프레임워크와 연동됩니다. LangChain의 오픈소스 패키지와 별도로 이용 조건·데이터 전송·보관·요금 조건을 확인해야 합니다.
[LangGraph 개요](https://docs.langchain.com/oss/python/langgraph/overview), [LangSmith 관측](https://docs.langchain.com/langsmith/observability), [LangSmith 평가](https://docs.langchain.com/langsmith/evaluation)

### 2.2. Spring AI: Spring 애플리케이션에 모델과 데이터를 연결하는 프레임워크

**Spring AI**는 Spring 프로젝트의 AI 애플리케이션 프레임워크입니다.
Java API를 제공하며 Kotlin에서도 사용할 수 있습니다. 기존 Spring Boot 서비스에 모델 호출·검색·도구 연결을 추가할 때,
Spring의 추상화·자동 설정·DI(필요한 객체를 컨테이너가 전달하는 의존성 주입) 방식을 활용합니다.
LangChain의 Java 포팅이 아니라 Spring 환경에 맞게 설계한 프로젝트입니다. [Spring AI 소개](https://docs.spring.io/spring-ai/reference/)

| 기능 | 역할과 사용 예 |
|---|---|
| ChatClient | 메서드를 이어 쓰는 Fluent API로 프롬프트와 모델 호출을 구성합니다. 공통 API로 제공 업체 교체 시 호출부 변경을 줄입니다. |
| 프롬프트 템플릿과 구조화된 출력 | 템플릿에 질문·조건을 넣고, 모델 응답을 일반 Java 객체(POJO)나 record에 매핑할 수 있습니다. 예를 들어 자연어 요청에서 가격 상한과 상품 분류를 추출합니다. |
| VectorStore 추상화 | 벡터 저장·유사도 검색을 공통 인터페이스로 사용합니다. 서로 다른 벡터DB 구현을 연결할 수 있습니다. |
| ETL 파이프라인과 RAG Advisor | ETL은 자료를 읽고(Extract), 변환하고(Transform), 저장하는(Load) 흐름입니다. 문서 수집·분할과 벡터스토어를 통한 임베딩·저장을 구성합니다. RAG Advisor는 모델 호출 과정에 관련 문서 검색과 문맥 추가를 연결합니다. |
| Tool Calling, Chat Memory, MCP | Java 함수를 도구로 연결하고, 이전 메시지를 저장·조회해 다음 요청에 포함할 수 있습니다. MCP(Model Context Protocol)는 도구·자료를 제공하는 서버와 연결하거나 Spring 기능을 서버로 노출하는 공통 프로토콜입니다. |
| Spring Boot Starter와 관측성 | Starter로 모델·벡터스토어의 자동 설정을 사용합니다. Micrometer 기반 지표·트레이싱을 Spring Boot의 Actuator 및 관측 도구와 연결할 수 있습니다. |

`ChatClient`의 사용 방식과 객체 매핑은 [ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html), [출력 변환](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html)을 참고하세요.
객체로 변환됐다고 값의 사실성이나 업무 조건까지 보장되지는 않습니다. 프롬프트 기반 형식 지시는 실패할 수 있으며, 지원 모델의 구조화 출력 기능과 별도 검증을 함께 검토합니다.

ETL과 RAG Advisor는 각각 자료 준비와 질문 시점 검색에 사용됩니다. 문서를 나눠 저장하는 것과 질문에 맞는 자료를 모델에 전달하는 것은 서로 다른 단계입니다.
[ETL 파이프라인](https://docs.spring.io/spring-ai/reference/api/etl-pipeline.html), [RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html)

Spring AI 2.0의 도구 호출은 `ToolCallingAdvisor`가 반복 흐름을 담당하며, 모델은 도구를 요청하고 애플리케이션이 실행합니다.
대화 메모리는 이전 메시지를 요청에 다시 포함하는 방식이며, MCP 연결만으로 권한이나 도구 실행 정책이 완성되지는 않습니다.
[도구 호출](https://docs.spring.io/spring-ai/reference/api/tools.html), [Spring AI 기능 개요](https://docs.spring.io/spring-ai/reference/)

관측성을 사용하려면 필요한 의존성과 지표·트레이싱 설정, 수집 도구를 구성해야 합니다.
Actuator를 추가하는 것만으로 모든 모델 호출 내용과 품질 평가가 자동 제공되는 것은 아닙니다.
[관측성](https://docs.spring.io/spring-ai/reference/observability/index.html)

## 3. 유사 기능과 제공 범위 비교

같은 목적의 기능을 한 행에 놓았습니다. 이름이 같지 않아도 유사한 역할이면 대응시켰습니다.
왼쪽은 **LangChain + LangGraph + LangSmith 생태계**, 오른쪽은 **Spring AI 2.0.2와 Spring Boot 연동**입니다.
LangGraph와 LangSmith는 별도 구성요소이며, LangSmith는 플랫폼 이용 조건도 별도로 확인해야 합니다.
LangChain 쪽은 Python 공식 문서를 중심으로 비교하므로 JS/TS의 지원 범위는 별도 확인이 필요합니다.

### 비교 기호 읽는 법

| 기호 | 의미: 왼쪽 LangChain 생태계와 오른쪽 Spring AI 비교 |
|---|---|
| `=` | 해당 목적의 핵심 기능을 양쪽 모두 제공. 성능·세부 기능·API가 동일하다는 뜻은 아님 |
| `>` / `>>` | 왼쪽이 해당 기준에서 유리 / 전용 기능이나 도구 지원에서 차이가 큼 |
| `<` / `<<` | 오른쪽이 해당 기준에서 유리 / 기존 Spring 통합이나 전용 지원에서 차이가 큼 |
| `n/a` | 비교 범위에 해당 역할의 기본 제공 구성요소가 없음. 직접 구현하거나 외부 도구로 구현할 수 없다는 뜻은 아님 |

비교 기호는 **기본 제공 범위·연결 대상의 폭·해당 환경에서의 구현 부담**에 대한 판단입니다.
공식 제품 등급이나 성능 벤치마크 결과가 아닙니다. 각 행의 마지막 열에 어떤 기준으로 판단했는지 적었습니다.

### 기능별 대응표

| 기능·비교 대상 | LangChain 생태계 | 비교 | Spring AI | 판단 기준과 차이 |
|---|---|:---:|---|---|
| 모델 호출과 제공 업체 교체 | 공통 Chat Model 인터페이스, 제공 업체별 패키지 | `=` | ChatModel, ChatClient, 제공 업체별 Starter | 공통 API로 호출부 변경을 줄이는 핵심 기능은 양쪽 모두 제공. 모델별 지원 기능은 별도 확인 |
| 프롬프트 템플릿 | PromptTemplate, ChatPromptTemplate | `=` | PromptTemplate, ChatClient의 메시지·파라미터 구성 | 질문·지침에 변수를 넣어 모델 입력 구성 |
| 문자열·객체 응답 처리 | 출력 파서, 스키마 기반 구조화 출력 | `=` | StructuredOutputConverter, ChatClient의 객체 변환·구조화 출력 | 원하는 형식으로 변환하는 기능은 모두 제공. 값의 사실성은 별도 검증 |
| 스트리밍 | 모델·에이전트의 스트리밍 API | `=` | ChatClient의 스트리밍 API | 모델 응답을 순차 전달하는 기본 기능 비교. 상태 이벤트 종류나 전달 방식까지 동일하지는 않음 |
| 고정 파이프라인 조합 | LCEL/Runnable로 순차·병렬 실행 조합 | `>` | Java 코드로 단계 연결, ChatClient와 Advisor 조합 | 재사용 가능한 범용 파이프라인 조합 API의 범위에서는 LCEL이 유리. Advisor는 주로 모델 호출 전후 처리에 초점 |
| 문서 읽기·분할·적재 | Document Loaders, Text Splitters, Vector Stores | `>` | DocumentReader, DocumentTransformer, DocumentWriter 기반 ETL | 다양한 문서 원천과 연결의 폭에서는 LangChain 카탈로그가 유리. 필요한 형식이 양쪽에 있으면 실제 차이는 줄어듦 |
| 임베딩·벡터 검색 | Embeddings, VectorStore, Retriever | `=` | EmbeddingModel, VectorStore, RAG 검색 구성요소 | 벡터 생성·저장·유사도 검색의 기본 역할은 모두 제공 |
| RAG 구성 | 검색 구성요소와 Runnable·에이전트로 검색·생성 연결 | `=` | QuestionAnswerAdvisor, RetrievalAugmentationAdvisor 또는 직접 구성 | 검색 문맥을 모델에 전달하는 기본 RAG 구현 가능. 검색 전략·DB 기능은 별도 비교 |
| 도구 정의·호출 | 도구 스키마·함수 정의와 에이전트 실행 루프 | `=` | @Tool, ToolCallback, ToolCallingAdvisor | 모델의 호출 요청을 받아 애플리케이션이 함수를 실행하는 기본 기능 제공 |
| 대화 문맥 유지 | 에이전트 상태와 LangGraph checkpointer 등 | `=` | ChatMemory, ChatMemoryRepository, 메모리 Advisor | 이전 메시지를 다음 요청에 포함하는 목적에서는 대응. 그래프 전체 상태의 복구는 아래 별도 행에서 비교 |
| 모델 호출 전후 처리 | 에이전트 middleware, Runnable 감싸기 등 | `=` | Advisor 체인 | 입력·출력 변환과 호출 제어라는 목적이 유사. 적용 위치·순서·실행 계약은 다름 |
| MCP 서버의 도구 사용 | MCP 연동 구성요소로 원격 도구를 LangChain 도구에 연결 | `=` | MCP Client Starter와 도구 연결 | 원격 MCP 도구를 사용하는 기능 제공. 프로토콜·인증·버전별 지원은 별도 확인 |
| 명시적인 그래프·멀티 에이전트 실행 | LangGraph의 상태·노드·간선·하위 그래프 | `>>` | `n/a` — Spring AI 자체의 대응 그래프 실행 엔진 없음 | 그래프 전용 런타임 제공 여부 비교. Spring AI에서도 Java 코드로 여러 모델·도구의 흐름을 직접 구성할 수 있음 |
| 실행 단계의 체크포인트·중단 후 재개 | LangGraph persistence/checkpointer | `>>` | `n/a` — 그래프 실행 상태 전체를 저장·재개하는 전용 런타임 없음 | ChatMemory의 메시지 저장과 실행 단계 복구는 다른 기능. 저장소와 실행 정책은 LangGraph에서도 구성 필요 |
| 사람의 확인을 기다린 뒤 실행 재개 | LangGraph interrupt와 재개 기능 | `>>` | `n/a` — 실행 중단·저장·재개를 묶은 대응 런타임 없음 | 확인 UI·승인 상태를 Java로 구현할 수는 있지만, 지속 가능한 실행 재개까지 기본 제공되는지는 구분 |
| 응답 평가와 실험 관리 | LangSmith의 데이터셋·Evaluator·실험·온라인 평가 | `>` | Evaluator와 테스트 코드, 별도 결과 관리 | 개별 평가 함수는 모두 구성 가능. 데이터셋·실험 결과를 한 플랫폼에서 관리하는 범위에서는 LangSmith가 유리 |
| AI 추적·평가 통합 관리 화면 | LangSmith 플랫폼 | `>>` | `n/a` — Spring AI 자체에 대응 독립 플랫폼·관리 UI 없음 | Spring AI의 관측 기능 부재를 뜻하지 않음. 별도 관측·평가 플랫폼과 연동 가능 |
| 기존 Spring 운영 도구와 통합 | 외부 서비스 호출과 추적 정보 전달 등으로 연결 | `<` | Micrometer 관측, Actuator와 기존 Spring 관측 체계 연계 | 기존 Spring 서비스 안에서 지표·트레이싱을 일관되게 관리하는 조건에서 Spring AI가 유리 |
| Spring Boot의 모델·벡터스토어 Bean 자동 설정 | `n/a` — LangChain 자체에 대응 Spring Boot Starter 없음 | `<<` | 공식 Starter·자동 설정·생성자 주입 | 기존 Java/Spring 애플리케이션 안에서 객체와 설정을 구성하는 직접 지원 비교 |
| 기존 Java 업무 기능을 같은 프로세스의 도구로 재사용 | `n/a` — Python/JS 런타임에서 Spring Bean을 직접 주입하는 기능 없음 | `<<` | Spring Bean을 호출하는 @Tool/ToolCallback | HTTP·MCP로 Java 서비스를 연결하는 것은 LangChain도 가능. 여기서는 별도 언어·프로세스 경계 없이 재사용하는 조건 비교 |

기능 확인 근거:

- 기본 기능과 통합 범위: [LangChain 통합 목록](https://docs.langchain.com/oss/python/integrations/providers/overview), [Runnable API](https://reference.langchain.com/python/langchain-core/runnables), [Spring AI 기능 개요](https://docs.spring.io/spring-ai/reference/)
- 모델 호출·도구·문맥: [Spring AI ChatClient](https://docs.spring.io/spring-ai/reference/api/chatclient.html), [도구 호출](https://docs.spring.io/spring-ai/reference/api/tools.html), [대화 메모리](https://docs.spring.io/spring-ai/reference/api/chat-memory.html), [LangChain MCP](https://docs.langchain.com/oss/python/langchain/mcp)
- 그래프·실행 복구: [LangGraph 개요](https://docs.langchain.com/oss/python/langgraph/overview), [상태 저장](https://docs.langchain.com/oss/python/langgraph/persistence), [중단·재개](https://docs.langchain.com/oss/python/langgraph/interrupts)
- 평가·관측: [LangSmith 평가](https://docs.langchain.com/langsmith/evaluation), [Spring AI 평가](https://docs.spring.io/spring-ai/reference/api/testing.html), [Spring AI 관측성](https://docs.spring.io/spring-ai/reference/observability/index.html)

이 표에서 `>>`가 많은 기술을 무조건 선택하지는 않습니다.
기존 Spring 쇼핑몰에서 조회 도구와 RAG를 연결하는 요구에는 Spring 통합의 이점이 클 수 있고,
오래 실행되는 그래프의 상태 복구가 핵심이면 LangGraph의 이점이 클 수 있습니다.
필요한 행을 먼저 고르고, 그 행의 비교 기준이 자신의 요구와 같은지 확인하세요.

## 4. 핵심 차이점

아래 표는 기능의 우열 점수가 아니라 도입 시 비교할 기준입니다.
LangChain의 핵심 API와, LangGraph·LangSmith를 함께 사용하는 생태계 전체를 구분해서 비교합니다.

| 구분 | LangChain과 관련 생태계 | Spring AI |
|---|---|---|
| 주 언어 | Python, JavaScript/TypeScript | Java API, Kotlin에서도 사용 |
| 기반 생태계 | Python의 AI·데이터 처리, JS/TS의 웹·서버 개발과 연결 | Spring Boot의 서비스·설정·DI·운영 도구와 연결 |
| 설계의 초점 | 모델·도구의 공통 인터페이스와 풍부한 구성요소 조합, 에이전트 구성 | 모델·벡터스토어 추상화와 Spring 애플리케이션 통합 |
| 에이전트·오케스트레이션 | LangChain 에이전트와 LangGraph의 명시적 상태·분기·반복·재개 기능 | 도구 호출 루프·Advisor·메모리 제공. 장기 실행 상태·복구·복잡한 분기는 요구사항에 맞춰 별도 설계 검토 |
| 통합 범위 | 폭넓은 통합 카탈로그. 언어·패키지·관리 주체별 지원 차이 확인 필요 | 주요 모델·벡터DB·MCP 지원. 원하는 기능의 제공 업체별 지원 여부 확인 필요 |
| 관측·평가 | LangSmith 등과 연결해 추적·평가·모니터링 구성 | Micrometer·Actuator와 기존 관측 도구 연결, 평가 기능 활용 |
| 성숙도·버전 | 오래된 예제와 현재 API가 혼재할 수 있음. 주요 패키지에는 1.0 이후 공개 API 호환성 정책이 있음 | 상대적으로 나중에 시작했지만 안정 버전을 제공. 이 과정은 2.0.2 사용. Spring Boot·AI 버전 호환성과 마이그레이션 확인 필요 |

LangGraph의 상태·실행 제어는 [공식 개요](https://docs.langchain.com/oss/python/langgraph/overview),
LangChain의 공개 API 호환성은 [릴리스 정책](https://docs.langchain.com/oss/python/release-policy)에서 확인할 수 있습니다.
Spring AI는 [공식 소개](https://docs.spring.io/spring-ai/reference/)에 안정 버전과 지원 기능을 안내합니다.

“LangChain은 실험용이고 Spring AI는 운영용”처럼 나눌 수는 없습니다.
두 기술 모두 운영에 사용할 수 있으며, 안정성은 선택한 버전·외부 모델·실패 처리·평가·팀의 운영 방식에 영향을 받습니다.

## 5. 장점과 도입 시 부담

이 절은 앞서 확인한 기능과 생태계를 바탕으로 한 선택 관점입니다. 커뮤니티 규모나 기능 출시 속도의 순위를 측정한 결과는 아닙니다.

### 5.1. LangChain

**장점**

- **다양한 통합과 참고 자료:** 여러 모델·저장소·도구를 연결하는 공식 문서와 예제가 있어 조합을 실험하기 좋습니다. 다만 새 기법이 항상 가장 먼저 제공된다고 보장할 수는 없습니다.
- **복잡한 에이전트 흐름:** LangGraph를 함께 사용하면 여러 에이전트, 조건 분기, 반복, 사람의 확인, 상태 저장과 재개를 명시적으로 구성할 수 있습니다.
- **디버깅과 평가:** LangSmith를 사용하면 실행 경로 조사와 데이터셋 기반 평가를 한 플랫폼에서 구성하기 편리합니다. 평가 기준과 데이터셋은 팀이 준비해야 합니다.
- **Python 데이터·ML 도구와 결합:** 데이터 전처리나 머신러닝 라이브러리를 이미 사용하는 팀은 같은 언어 안에서 AI 기능을 구성하기 쉽습니다. JS/TS 팀도 해당 언어용 패키지를 선택할 수 있습니다.

**부담과 주의점**

- **버전 업그레이드와 예제 혼재:** 구버전 체인·메모리·에이전트 예제를 현재 코드에 그대로 적용하면 맞지 않을 수 있습니다. 다만 “공개 API가 매번 깨진다”는 설명은 부정확합니다. 주요 패키지는 1.0 이후 시맨틱 버저닝을 따르며 공개 API의 호환성을 깨는 변경을 메이저 버전에서 하도록 정하고 있습니다. 커뮤니티·제공 업체 패키지는 각 정책을 따로 확인합니다. [릴리스 정책](https://docs.langchain.com/oss/python/release-policy)
- **추상화와 디버깅 비용:** Runnable·에이전트·그래프·외부 API가 여러 단계로 연결되면 입력과 상태가 어디서 바뀌는지 추적해야 합니다.
- **작은 기능의 복잡도:** 한 번 호출해 문자열을 받는 기능에 에이전트와 그래프까지 넣으면 학습·운영 부담이 커질 수 있습니다. 필요한 구성요소만 사용하거나 직접 SDK 호출을 검토합니다.
- **Java 조직의 추가 운영 부담:** Python 구현을 선택해 Spring과 분리하면 Python 실행 환경·배포·API 계약을 추가로 관리합니다. 그러나 Python 서비스가 필수인 것은 아닙니다. JS/TS 구현도 가능하며, 별도 서비스 운영 여부는 아키텍처 선택입니다.

### 5.2. Spring AI

**장점**

- **기존 Spring 백엔드와 통합:** 보안·설정·DI·트랜잭션·모니터링과 같은 기존 서비스 구조 안에서 AI 기능을 개발하기 쉽습니다. 다만 외부 모델 HTTP 호출이 DB 트랜잭션과 함께 롤백되는 것은 아닙니다.
- **모델·벡터DB 교체 부담 감소:** 공통 인터페이스를 사용하면 호출부 변경을 줄일 수 있습니다. 모델별 옵션·지원 기능·응답 품질 차이, 벡터DB별 검색 기능과 임베딩 차원·데이터 재색인 여부는 별도로 확인합니다.
- **Java 객체와 타입 중심 개발:** 구조화된 출력을 POJO·record로 변환하고 기존 Java 검증 로직에 연결할 수 있습니다. 타입이 맞는 것과 내용이 정확한 것은 별도로 검사합니다.
- **기존 운영 역량 활용:** Spring Boot 배포·설정·관측 체계를 이미 갖춘 팀은 같은 도구와 경험을 활용할 수 있습니다. 이는 팀 환경에 따른 이점이며 제품 자체의 무조건적인 안정성 보장은 아닙니다.

**부담과 주의점**

- **통합 대상과 참고 자료 확인:** LangChain의 넓은 통합 카탈로그와 비교해 원하는 Java 연결이나 사례가 없을 수 있습니다. 커뮤니티 자료가 항상 더 적다고 단정하기보다 필요한 제공 업체·기능·버전의 공식 지원과 예제를 비교합니다.
- **복잡한 오케스트레이션 설계:** 도구 호출 루프는 제공되지만, 업무 단계별 상태 저장·중단 후 재개·여러 에이전트의 협력까지 모두 자동 구성되지는 않습니다. 이런 요구는 직접 설계하거나 별도 도구와 조합할 수 있습니다.
- **기능 반영 시점과 API 변화:** 원하는 신규 모델 기능의 지원이 제공 업체 SDK보다 늦을 수 있고, 메이저 버전 이동 시 코드 변경이 필요할 수 있습니다. 모든 기능에서 LangChain보다 느리다고 일반화하지 말고 해당 기능의 지원 버전과 변경 내역을 확인합니다.
- **Python 중심 도구와의 연결:** 데이터 과학·모델 실험 분야에는 Python을 우선 지원하는 도구가 많습니다. 필요한 도구가 Java API를 제공하지 않으면 별도 연동을 고려해야 합니다. 모든 최신 AI 도구가 Python 전용인 것은 아닙니다.

## 6. 선택 가이드

### 6.1. 요구사항으로 먼저 판단하기

| 팀과 요구사항 | 먼저 검토할 선택 | 선택 이유와 추가 확인 |
|---|---|---|
| 기존 Java/Spring 서비스에 검색·챗봇·RAG를 추가하고 기존 운영 체계를 활용하려 함 | Spring AI | 기존 코드·보안·설정·관측을 활용하기 쉽습니다. 필요한 모델·벡터DB 기능과 호출 비용·실패 처리를 확인합니다. |
| Python에 익숙하고 데이터·ML 도구를 결합해 빠르게 프로토타입을 만들려 함 | LangChain | 관련 라이브러리와 다양한 연결을 조합하기 좋습니다. 필수 구성요소만 선택하고 사용할 버전을 고정합니다. |
| 상태를 저장하는 복잡한 에이전트·멀티 에이전트·분기·루프·재개가 핵심 | LangChain과 LangGraph 조합 또는 LangGraph 직접 사용 | 제공되는 상태·흐름 제어 기능을 검토합니다. 실제로 모델 판단이 필요한 단계와 코드로 정할 단계를 구분합니다. |
| JS/TS 팀에서 AI 기능을 개발하려 함 | LangChain JS/TS | Python 서비스 없이 같은 언어로 구성할 수 있습니다. Python용 예제와 기능이 동일한지는 확인합니다. |
| 단순한 모델 요청 한 번만 필요함 | Spring AI·LangChain의 최소 구성 또는 제공 업체 SDK | 언어와 기존 의존성에 맞춰 선택합니다. 복잡한 오케스트레이션 도구부터 도입할 필요는 없습니다. |

새 기법을 빠르게 실험하는 것이 중요하다면 Python 생태계와 LangChain 통합을 살펴볼 이유가 있습니다.
다만 “최신”이라는 이유만으로 선택하기보다 필요한 기능이 실제로 지원되는지, 기존 팀이 운영할 수 있는지 확인합니다.

### 6.2. Spring과 Python AI 서비스를 나누는 구성

두 기술을 나누어 사용하는 것도 가능한 실무 구조입니다. 다음은 실행 코드가 아닌 역할 배치 예시입니다.

```text
사용자 → Spring 백엔드: 로그인 확인·주문 접근 권한·업무 규칙
                     → HTTP API → Python AI 서비스: LangChain/LangGraph로 검색·모델·도구 흐름 구성
                     ← 구조화된 처리 결과
       ← Spring 백엔드: 결과 검증·사용자 응답
```

예를 들어 기존 주문·회원 기능은 Spring에 두고, AI 팀이 Python으로 문서 검색과 복잡한 에이전트 흐름을 개발할 수 있습니다.
Spring 백엔드가 Python 서비스를 HTTP로 호출하기만 한다면 Spring AI를 반드시 사용할 필요는 없습니다.
Spring에서도 별도의 모델 호출이나 Java 도구 연결을 담당할 때 두 프레임워크를 각각 사용할 수 있습니다.

서비스를 나누면 두 팀의 언어·배포 주기를 분리할 수 있지만 네트워크 지연, 타임아웃, 재시도에 따른 중복 실행,
인증 정보 전달, 요청 추적, API 변경 관리도 함께 설계해야 합니다.
팀과 업무 경계가 분명할 때 검토할 구조이며, 혼합 구성이 항상 더 좋거나 얼마나 흔한지는 이 문서에서 단정하지 않습니다.

### 6.3. 이 과정에서 Spring AI를 사용하는 이유

이 과정의 독자는 Spring Controller·Service·생성자 주입을 사용하는 개발자입니다.
따라서 기존 Java 서비스에서 모델을 호출하고, Java 업무 기능을 도구로 연결하는 흐름을 Spring AI로 배웁니다.
LangChain보다 모든 면에서 우월해서 선택한 것이 아니라 **독자의 기존 기술과 이번 학습 목표에 맞춘 선택**입니다.

실제 도입을 결정할 때는 위 표에서 자신의 상황에 맞는 행을 고른 뒤,
같은 질문·같은 데이터·같은 성공 기준으로 작은 기능을 만들어 품질·응답 시간·비용·실패 처리를 비교하세요.
이 문서는 기능과 선택 기준의 비교이며 두 프레임워크의 성능을 실행 측정한 결과는 아닙니다.

## 확인 질문

1. Java/Spring 주문 서비스에 반품 규정 RAG를 추가하려 합니다. 두 프레임워크 모두 가능한데도 Spring AI를 먼저 검토할 이유는 무엇인가요?
2. LangChain, LangGraph, LangSmith는 각각 어떤 역할을 맡나요? LangChain을 사용하면 반드시 Python 서비스를 별도로 운영해야 하나요?
3. Spring AI의 공통 API를 사용하면 모델·벡터DB를 설정만 바꿔 언제나 동일하게 동작시킬 수 있나요? 객체 변환 성공이 내용의 정확성도 보장하나요?
4. Spring 백엔드가 Python AI 서비스를 호출하는 구조에서 얻는 이점과 추가로 관리할 책임을 각각 설명해 보세요.

<details>
<summary>해설</summary>

1. 기존 Spring 코드·DI·설정·운영 도구를 활용할 수 있기 때문입니다. 필요한 모델과 벡터DB 지원을 확인한다는 전제에서, 새 언어와 별도 서비스 도입 부담을 줄일 수 있습니다. 두 기술의 RAG 가능 여부만으로 결정하는 것은 아닙니다.
2. LangChain은 모델·도구 연결과 에이전트 구성을, LangGraph는 상태와 실행 흐름의 조정을, LangSmith는 추적·평가·모니터링을 담당합니다. Python 서비스는 필수가 아닙니다. JS/TS 구현도 있고, 서비스를 나눌지는 팀의 언어와 시스템 구조에 따라 결정합니다.
3. 항상 동일하게 동작하지는 않습니다. 공통 API는 변경 부담을 줄이지만 제공 업체별 옵션·도구 호출·검색 기능·임베딩·답변 품질이 다릅니다. 객체 변환 역시 형식을 다루므로 실제 가격·재고·권한 같은 내용은 별도로 검증해야 합니다.
4. 기존 Spring 업무 기능을 유지하면서 AI 로직을 Python 도구로 개발하고 배포 주기를 나눌 수 있습니다. 대신 HTTP 통신의 실패·지연·중복 실행, 서비스 간 인증·권한, 추적 정보와 API 계약을 함께 관리해야 합니다.

</details>

[00. LLM과 Spring AI로 돌아가기](../00-llm-and-spring-ai.md) · [01. 환경과 프로젝트 준비](../01-setup.md) · [학습 지도](../../curriculum.md)

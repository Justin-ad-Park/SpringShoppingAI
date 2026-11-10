# 레퍼런스: 무엇을 언제 읽을까?

검토 갱신: 2026-11-03. 이 문서는 링크 모음보다 읽는 순서와 용도에 초점을 둡니다.
한국어 교재를 먼저 읽고 막힌 개념에 해당하는 절만 찾아보세요.

## 첫 묶음에서 읽을 자료

| 순서 | 자료 | 먼저 읽을 부분 | 검토 의견과 주의점 |
|---|---|---|---|
| 1 | [Spring AI Introduction](https://docs.spring.io/spring-ai/reference/) | 소개와 기능 목록 중 Chat 관련 항목 | 프레임워크의 범위를 파악하기 좋음. 전체 기능을 지금 배울 필요 없음 |
| 2 | [Getting Started](https://docs.spring.io/spring-ai/reference/getting-started.html) | 지원 Boot 버전, Maven Central, BOM | 시작 설정의 기준. AI 기초와 코드 해설은 자체 교재로 보완 |
| 3 | [Chat Client API](https://docs.spring.io/spring-ai/reference/api/chatclient.html) | Creating a ChatClient, autoconfigured Builder 예제 | 첫 호출 API를 이해하기 좋음. 다중 모델·고급 Advisor는 나중에 읽기 |
| 4 | [OpenAI Chat](https://docs.spring.io/spring-ai/reference/api/chat/openai-chat.html) | Prerequisites, Auto-configuration, 설정 표 | 실제 어댑터 설정 확인용. 고급 모델 옵션을 처음부터 복사하지 않기 |
| 5 | [Boot Gradle 시작 안내](https://docs.spring.io/spring-boot/4.0/gradle-plugin/getting-started.html) | 플러그인 적용 예제 | 교육용 Boot 4.0 계열의 빌드 설정 확인 |

## 이후 단원에서 읽을 자료

| 단원 | 자료 | 추천 이유 / 읽는 방법 |
|---|---|---|
| 04 | [Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output.html) | 출력 형식과 변환을 이해하는 기준. 형식 보장이 업무 값의 정확성을 보장하는 것은 아님 |
| 05 | [Tool Calling](https://docs.spring.io/spring-ai/reference/api/tools.html) | 모델의 요청과 애플리케이션 실행 책임을 구분하는 데 적합. Defining Tools부터 작은 예제로 확인 |
| 06 | [Anthropic: Building Effective Agents](https://www.anthropic.com/research/building-effective-agents) | Workflow/Agent 구분과 단순한 설계 선택에 유용. Spring API 교재가 아니므로 개념을 읽기 |
| 06 심화 | [Building Effective Agents — Spring AI Reference](https://docs.spring.io/spring-ai/reference/api/effective-agents.html) | Routing·Chaining 등 패턴 구현 비교. 한꺼번에 모든 패턴을 구현하지 않기 |
| 07 | [Chat Memory](https://docs.spring.io/spring-ai/reference/api/chat-memory.html) | 대화 ID·저장·메시지 창을 이해한 뒤 필요한 Repository만 선택 |
| 08 | [Spring 공식 블로그: Tool Calling in Spring AI 2.0](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling/) | 도구 루프와 Advisor 관계의 설명이 좋음. 처음에는 앞부분만 읽고 고급 확장은 생략 |
| 09~10 | [Retrieval Augmented Generation](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html) | 검색과 답변 생성을 연결하는 기준. 임베딩·검색 개념을 배운 후 읽기 |
| 11 | [Evaluation Testing](https://docs.spring.io/spring-ai/reference/api/testing.html), [Observability](https://docs.spring.io/spring-ai/reference/observability/index.html) | 품질 평가와 호출 관측을 분리해서 학습. LLM 평가자의 점수도 절대적인 정답 판정은 아님 |
| 11장 | [MCP Overview](https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html) | 11장 연결·직접 호출을 학습할 때 원격 도구의 역할 확인 |

## 블로그·강좌·예제 검토

- [Spring 공식 블로그: Building Effective Agents with Spring AI (2025-01-21)](https://spring.io/blog/2025/01/21/spring-ai-agentic-patterns/): 패턴의 목적과 흐름을 이해하는 데 유용합니다. 2025년 예제이므로 현재 2.0 API에 그대로 붙여 넣기보다 개념 자료로 사용합니다.
- [Spring 공식 블로그: Tool Calling in Spring AI 2.0 (2026-06-15)](https://spring.io/blog/2026/06/15/spring-ai-composable-tool-calling/): 1.x와 2.0 구조 차이를 설명하며 현재 교육 버전과 연결하기 좋습니다. Tool Calling을 직접 해본 뒤 읽는 보충 자료로 채택했습니다.
- [공식 spring-ai-examples](https://github.com/spring-projects/spring-ai-examples): 구현 비교용으로 채택합니다. 단원 완료 후 관련 예제만 열고 해당 예제의 build 파일과 README에서 버전을 확인하세요. main 브랜치는 계속 바뀝니다.
- [Spring Academy 입문 과정](https://spring.academy/courses/spring-ai-intro): 과정 후보로 확인했습니다. 이번에는 강의 본문 전체를 읽지 못해 세부 품질·접근 조건·버전 호환성 검토를 보류했습니다. 필수 자료로 지정하지 않습니다.

입문 흐름 전체를 하나의 블로그에 맡기기보다, 자체 한국어 교재가 선수 지식을 연결하고 공식 자료로 사실을 확인하는 방식을 택했습니다. 블로그의 코드가 짧다는 이유만으로 초보자에게 쉬운 것은 아닙니다.

## 버전과 자료를 읽는 기준

현재 확인한 공식 기본 레퍼런스는 Spring AI 2.0.2입니다. `/reference/` URL은 향후 다른 버전을 가리킬 수 있습니다.
읽을 때 페이지의 버전 표시를 확인하고 실습의 `build.gradle`과 비교하세요.
실습과 기존 서비스는 공통 빌드에서 Maven Central에 배포된 2.0.2와 Boot 4.0.9로 통일했습니다.
SNAPSHOT·Milestone 예제, 1.x Tool Calling 내부 구조, 별도 community 유틸리티를 기본 기능처럼 혼합하지 않습니다.
자료를 업데이트할 때는 버전 확인 → 컴파일 → 로컬 계약 테스트 → 필요한 경우 실제 모델 평가 순으로 확인합니다.

## 13-5 의도 분류의 참고 자료

확인일: 2026-11-10. [13-5 교재](lessons/13-shopping-agent/05-intent-classification.md)와 [실습](../labs/13-05-intent-classification/README.md)의 구현 근거입니다.

| 자료 | 읽을 부분 | 참고한 내용과 적용 범위 |
|---|---|---|
| AWS 공식 블로그: Information extraction with LLMs using Amazon SageMaker JumpStart — [한국어 경로](https://aws.amazon.com/ko/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/), [영문 원문](https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/) | Classification using prompt engineering → Sub-intents and intent trees | main_intent/sub_intents 트리, JSON 삽입, #### 구분 표시, 출력 예시, 한 문의에서 대분류·소분류를 함께 받는 방식. 내용은 영문 원문으로 확인했습니다. |
| [Spring AI Output Converters](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html) | 구조화 응답 변환의 역할과 한계 | .entity()로 모델 응답을 record로 받는 구현 근거. 형식 변환과 코드 조합의 업무 검증을 구분합니다. |

AWS 글은 2024-05-07 게시된 가상 보험 상담 기술 예제입니다. 특정 상용 쇼핑몰의 도입 실적이나 이 저장소 샘플의 인증 근거가 아닙니다. 보험 분류를 쇼핑 분류로 바꾸고 한글 설명·보류 정책·JSON 반환·Java 검증을 추가한 자체 실습으로 사용합니다. SageMaker 배포와 파인튜닝 절차는 이번 구현 범위에 넣지 않습니다.

## 13-6 OpenAI 모델 품질 비교 참고 자료

확인일: 2026-11-10. [13-6 교재](lessons/13-shopping-agent/06-intent-quality-evaluation.md)와 [Live 실습](../labs/13-06-intent-quality-evaluation/README.md)의 모델 ID·표준 가격·용도 설명을 확인한 자료입니다.

| 자료 | 확인한 내용 | 이 실습에서의 사용 |
|---|---|---|
| [GPT-6 Luna](https://developers.openai.com/api/docs/models/gpt-6-luna) | 비용 민감·대량 처리 용도, Chat Completions·구조화 응답 지원, 입력 $0.10·출력 $0.50/백만 토큰 | 저가 기준 모델 |
| [GPT-5.6 Luna](https://developers.openai.com/api/docs/models/gpt-5.6-luna) | 비용 민감·대량 처리 용도, 입력 $0.20·출력 $1.20/백만 토큰 | 이전 세대 저비용 모델 |
| [GPT-5.4 mini](https://developers.openai.com/api/docs/models/gpt-5.4-mini) | mini 모델, 입력 $0.75·출력 $4.50/백만 토큰 | mini 등급 비교 모델 |
| [OpenAI API 가격표](https://developers.openai.com/api/docs/pricing) | 표준·캐시·배치 등 가격 구분 | 실습 표는 표준 입력·출력 가격만 발췌 |

가격·모델 접근 권한은 바뀔 수 있습니다. 가격표는 예상 단가 비교용이며 테스트 실행 비용을 계산하지 않습니다. 평가 결과는 교육용 다섯 문항의 정답 일치율이지 OpenAI가 보증한 모델 품질 순위나 전체 서비스 정확도가 아닙니다.

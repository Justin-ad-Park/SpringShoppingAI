# 05. Tool Calling: 모델이 요청하고 Java가 실행한다


선수 지식: 04장의 구조화 데이터, 기존 Controller/Service, 프롬프트.
이번에는 **이미 작성한 Java 기능을 모델이 요청할 수 있게 연결**합니다.
[공통 다이어그램 기준](../diagram-guide.md): 클래스는 책임, 시퀀스는 호출 순서를 보여줍니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [모델 없이 상품 서비스를 호출하기](05-tool-calling/01-java.md) | 가격과 배송비의 출처를 일반 Java 코드에서 찾습니다. |
| 2 | [Java 메서드를 도구로 제공하기](05-tool-calling/02-tools.md) | 도구 설명 등록과 실제 메서드 실행을 구분합니다. |
| 3 | [도구 결과가 모델로 돌아가는 과정](05-tool-calling/03-loop.md) | 한 번의 `.call().content()` 실행에서 여러 모델 요청이 생기는 이유를 설명합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/05-tool-calling의 실행 안내](../../labs/05-tool-calling/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:test --tests com.example.ailearning.l05tools.ToolCallingLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:05-tool-calling:bootRun
```

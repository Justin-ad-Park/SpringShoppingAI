# 08. Advisor: 호출 전후 처리를 조합하기

## 개념 먼저 이해하기

**Advisor(어드바이서)**는 모델 호출의 앞뒤에 필요한 처리를 연결하는 Spring AI의 기능입니다. 이전 대화를 질문에 추가하거나 호출 기록을 남기는 등, 여러 AI 기능에서 반복되는 처리를 일관되게 적용할 때 사용합니다.

예를 들어 상담 서비스에서는 모델에 질문하기 전에 이전 대화를 함께 준비하고, 응답을 받은 뒤 대화를 저장해야 합니다. Spring AI가 제공하는 메모리 Advisor를 연결하면 이런 처리를 각 질문마다 직접 작성하는 부담을 줄일 수 있습니다.

| 용어 | 쉬운 설명 | 예 |
|---|---|---|
| Advisor | 모델과 주고받는 요청·응답의 처리 과정에 참여하는 기능 | 메모리 연결, 호출 기록 |
| 요청 전 처리 | 모델에 보내기 전에 입력을 준비하거나 보완하는 작업 | 이전 대화 추가 |
| 응답 후 처리 | 받은 응답을 저장하거나 후속 작업에 사용하는 처리 | 모델 답변을 대화 메모리에 저장 |
| Advisor 체인 | 여러 Advisor를 정해진 순서로 연결한 구성 | 대화 준비와 호출 기록을 함께 적용 |

Advisor는 애플리케이션 안에서 실행되는 처리 코드입니다. 이름 때문에 또 다른 AI가 답변에 조언하는 기능으로 생각하기 쉽지만, 별도의 모델이 있어야 하는 것은 아닙니다.

어떤 정보를 모델에 보낼지, 무엇을 언제 저장할지에 따라 서비스의 동작이 달라집니다. 개발팀은 제공되는 Advisor와 직접 구현한 처리를 조합합니다. 이 단원에서는 연결 순서에 따라 실행 기록과 대화 저장이 어떻게 달라지는지 확인합니다.

개념 참고: [Spring AI의 Advisors API](https://docs.spring.io/spring-ai/reference/api/advisors.html).

## 실습 안내

선수 지식: 07의 직접 메시지 조회·저장, 05의 도구 호출.
이번에는 **반복되는 호출 전후 작업을 Advisor로 옮기는 것**을 배웁니다.
[그림 기준](../diagram-guide.md): 시퀀스는 되돌아오는 순서, 플로우차트는 도구 루프와 처리 위치를 보여줍니다.


## 작은 수업으로 나누어 읽기

처음에는 아래 순서로 하나씩 학습합니다. 전체 코드를 한 번에 이해하거나 보조 코드를 직접 작성할 필요는 없습니다.

| 순서 | 소수업 | 완료 기준 |
|---|---|---|
| 1 | [호출 전후를 감싸는 Advisor](08-advisors/01-chain.md) | 여러 Advisor의 요청 전·응답 후 처리 순서를 이해합니다. |
| 2 | [메모리와 도구 루프의 위치](08-advisors/02-memory-loop.md) | 앞에서 직접 만든 문맥 전달을 Advisor와 연결합니다. |
| 3 | [순서와 호출 횟수 실험](08-advisors/03-observe.md) | Advisor의 위치에 따라 실행 순서와 반복 횟수가 달라지는 이유를 설명하고, 대화가 중복 저장되지 않는지 확인합니다. |

각 소수업은 이 단원의 실습 모듈을 함께 사용합니다. 표의 순서대로 개념과 실행 코드를 연결하세요.
[공통 학습 방법](../learning-guide.md) · [전체 교육 계획](../curriculum.md)

## 실행 코드와 테스트

[labs/08-advisors의 실행 안내](../../labs/08-advisors/README.md)를 확인합니다.
이 단원의 기본 구현은 다음 명령으로 테스트·빌드·실행합니다. 명령은 저장소 루트 기준입니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:test --tests com.example.ailearning.l08advisors.AdvisorLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:bootJar
./gradlew :Spring-AI-Agent-Learning:labs:08-advisors:bootRun
```

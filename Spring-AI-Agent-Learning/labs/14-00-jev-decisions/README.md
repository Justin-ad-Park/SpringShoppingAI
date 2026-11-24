# 실습 14: Jev의 Choice·Score·Noul

[14장 교재](../../docs/lessons/14-jev.md)의 세 소주제를 한 앱에서 실행합니다.<br>
Spring AI 본체의 ChatModel 대신 **Spring AI Community의 TypeSafe Starter 0.4.0**이 만든 `TypeSafeClient`를 사용합니다.<br>
기존 Java 21·Boot 4.2.0·Spring AI BOM 2.1.0를 유지하며 OpenAI API는 호출하지 않습니다.

## 코드 읽는 순서

1. [application.yml](src/main/resources/application.yml): 키·주소·모델·타임아웃·재시도 설정.
2. [ShoppingDecisionController](src/main/java/com/example/ailearning/l14jev/ShoppingDecisionController.java): text/plain 요청을 세 Service에 전달.
3. [IntentClassificationService](src/main/java/com/example/ailearning/l14jev/IntentClassificationService.java), [SupportPriorityService](src/main/java/com/example/ailearning/l14jev/SupportPriorityService.java), [HumanSupportRequestService](src/main/java/com/example/ailearning/l14jev/HumanSupportRequestService.java): 질문 정의 → 요청 생성 → HTTP 호출 → 타입별 응답 조회.
4. [SupportRoutingPolicy](src/main/java/com/example/ailearning/l14jev/domain/SupportRoutingPolicy.java): 분류 결과를 서비스 처리 기준으로 변환.<br>
   상태 변경·업무 도구 실행은 없습니다.

Starter가 `spring.ai.typesafe.*`를 읽어 TypeSafeClient 빈을 등록하고 세 서비스 생성자에 주입합니다.<br>
직접 Client 빈이나 ChatClient.Builder를 만들 필요가 없습니다.<br>
`state`는 고객 문장, `questions`는 판단 기준이며 `systemOne()`에서 API를 호출합니다.

## 세 기능의 반환 타입

| 기능 | SDK에서 읽는 결과 | 서비스와 Live 테스트에서 받는 결과 |
|---|---|---|
| Choice | `ChoiceAnswer`: 선택 항목·confidence·항목별 확률 | `IntentClassificationResult`: 예측 의도·confidence·확률·확인 질문 필요 여부 |
| Score | `ScoreAnswer`: 점수·confidence·단계별 확률 | `SupportPriorityResult`: 점수·confidence·확률·상담 우선순위 |
| Noul | `NoulAnswer`: 참일 확률 | `HumanSupportRequestResult`: 상담원 연결 요청 확률·처리 경로 |

세 결과 record는 각각 해당 Service 안에 선언합니다.<br>
SDK 응답과 Java 정책을 적용한 서비스 결과를 구분하며, Live 테스트에서는 명시적인 타입으로 변수를 선언합니다.<br>
요청·SDK 응답·결과를 읽을 때 타입 추론을 사용하지 않습니다.

## 키 없이 로컬 검증

모든 Gradle 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLocalModelTest' --tests '*SupportRoutingPolicyUnitTest' :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:bootJar
```

테스트는 application-test.yml에서 루트 .env 읽기를 끄고 더미 키·모델을 사용합니다.<br>
동적으로 정해지는 대역 서버 주소만 DynamicPropertySource로 덮어씁니다.<br>
고정 응답 서버는 이 모듈의 테스트 전용 JevStubServer이며 공통 OpenAI 대역을 Jev API인 것처럼 재사용하지 않습니다.

| 테스트 | 확인 범위 |
|---|---|
| JevLocalModelTest | 실제 SDK·Starter·Controller·정책 연결, 요청 state·criteria·모델·인증 헤더, 응답 변환, 공백 입력 400, 모델 장애·타입 오류 전파 |
| SupportRoutingPolicyUnitTest | confidence 0.7, score 0.5·1.5, Noul 0.2·0.8의 경계값 |
| JevLiveTest | 상품 추천·배송 문의의 의도 구분, 일반·긴급 문의의 Score 비교, 상담원 요청·비요청의 Noul 비교. 소수 예제이며 품질 통계는 측정하지 않음 |

테스트별 시작 이름과 HTTP 결과를 출력하며 assertion으로 검증합니다.<br>
고정 응답 테스트가 한국어 의미 판단을 검증하는 것은 아닙니다.<br>
각 소주제에 세부 테스트 선택 명령도 있습니다.

## 실제 Jev 호출

TypeSafe 계정에서 API 키를 준비하고, 저장소 루트 `.env`에 아래 변수만 추가합니다.<br>
기존 OpenAI 설정은 변경하지 않습니다.<br>
실제 키를 문서·소스에 적지 않습니다.

```properties
TYPESAFE_API_KEY=발급받은_키
TYPESAFE_DEFAULT_MODEL=jev-latest
```

별칭인 jev-latest의 실제 모델은 바뀔 수 있습니다.<br>
비교 평가에서는 계정에서 사용 가능한 고정 모델 ID를 지정하고 입력·모델 버전을 함께 기록합니다.<br>
한국어 정확도는 대표 고객 요청으로 별도 평가해야 합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:bootRun
```

기본 포트는 8114입니다.<br>
아래 각 요청은 **실제 Jev API 한 번을 호출**하며 사용량이 발생합니다.

```bash
curl -X POST http://localhost:8114/api/jev/intent -H 'Content-Type: text/plain; charset=UTF-8' --data '3만원 이하의 텀블러를 추천해 주세요.'
curl -X POST http://localhost:8114/api/jev/priority -H 'Content-Type: text/plain; charset=UTF-8' --data '주문 취소 마감이 오늘인데 아직 처리가 안 됐어요.'
curl -X POST http://localhost:8114/api/jev/human-support -H 'Content-Type: text/plain; charset=UTF-8' --data '자동 응답 말고 사람 상담원과 이야기하고 싶어요.'
```

응답은 각각 predictedIntent·confidence·probabilities·clarificationRequired, score·confidence·probabilities·priority, yesProbability·action을 포함합니다.<br>
값의 의미와 예시 JSON은 세 소주제에서 확인합니다.

실제 모델 테스트는 아래 명령으로 따로 실행합니다.<br>
세 테스트가 각각 두 번 호출하여 전체 실행 시 API 요청은 총 여섯 번입니다.<br>
로컬 검증용 클래스 필터를 빼면 Live도 실행되므로 주의합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLiveTest'
```

### Live 기능별 선택 실행

```bash
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLiveTest.Choice*'
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLiveTest.Score*'
./gradlew :Spring-AI-Agent-Learning:labs:14-00-jev-decisions:test --tests '*JevLiveTest.Noul*'
```

각 기능을 선택하면 두 고객 요청과 결과를 출력합니다.<br>
Choice는 명확한 두 요청의 예상 의도를 검증하고, Score·Noul은 대조 입력 간 점수·확률의 상대적인 차이를 검증합니다.<br>
모델 버전이나 한국어 판단 품질에 따라 실패할 수 있습니다.<br>
정확한 소수 값은 고정하지 않으며, 서비스 임계값은 별도 단위 테스트에서 확인합니다.

## 실패와 구현 범위

- 공백·2000자 초과 요청은 Controller 검증으로 400입니다.
- timeout은 10초, 재시도는 0으로 설정했습니다.<br>
  API 장애나 응답 타입 오류를 정상 분류로 숨기지 않으며 별도 예외 변환 없이 전파합니다.<br>
  로컬 테스트에서 해당 오류의 앱 HTTP 응답은 500입니다.<br>
  Jev의 원래 HTTP 코드를 그대로 중계하는 API는 아닙니다.
- confidence·임계값은 정확도나 상담 처리 보장이 아닙니다.<br>
  이번 코드는 판단 결과와 Java 정책의 연결을 보여 줍니다.<br>
  인증·권한·실제 주문 확인·업무 실행은 별도 구성입니다.
- 한 요청에 여러 질문을 묶을 수 있지만 각각 같은 state를 독립적으로 평가합니다.<br>
  앞 질문의 답을 뒤 질문이 참고하는 순차 Workflow로 해석하지 않습니다.<br>
  이번 기본 예제는 기능별로 한 질문만 보냅니다.

[Choice](../../docs/lessons/14-jev/01-choice-intent.md) · [Score](../../docs/lessons/14-jev/02-score-priority.md) · [Noul](../../docs/lessons/14-jev/03-noul-human-support.md)

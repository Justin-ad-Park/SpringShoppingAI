# 13-5. AWS 예제를 참고해 대분류·소분류 의도 분류하기

**실행 코드·설정·테스트:** [labs/13-05-intent-classification](../../../labs/13-05-intent-classification/README.md).

**이번 목표:** 고객이 원하는 일을 상담 업무로 구분하고, 대분류 10개·소분류 총 40개인 의도 트리를 모델에 전달해 대분류·소분류 조합을 한 번에 받습니다. 추가 확인이 필요한 문의와 잘못된 코드 조합도 구분합니다.

**읽는 순서:** 기획자는 1~3절과 5절에서 용어·흐름·보류 정책을 먼저 읽습니다. 개발자는 4절의 코드와 6절의 실행을 따라갑니다. 상용 사례·설계 비교·평가는 7~9절에서 읽습니다.

**선수 지식:** [04장 구조화 응답](../04-structured-output.md), [13-2 의도 추출](02-intent.md). 기본 상담 모듈과 분리된 선택 보충입니다.

### 이 수업이 참고한 공식 자료

AWS의 **Information extraction with LLMs using Amazon SageMaker JumpStart** 중 **Sub-intents and intent trees** 절을 참고했습니다. [한국어 경로](https://aws.amazon.com/ko/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/) · [확인한 영문 원문](https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/)

AWS의 가상 보험 상담 예제를 쇼핑 업무와 Spring AI에 맞게 재구성한 자체 교육 예제입니다. [Spring AI의 구조화 응답](https://docs.spring.io/spring-ai/reference/api/structured-output/converters.html)을 이용하며, 어떤 부분을 바꾸었는지는 3절에서 설명합니다. 전체 참고 목록은 [레퍼런스](../../references.md#13-5-의도-분류의-참고-자료)에서 확인합니다.

## 1. Intent는 고객이 어떤 일을 원하는지 구분하는 것

**의도(Intent)**는 고객이 문의에서 요청하는 도움이나 처리입니다. **의도 분류(Intent classification)**는 문의를 읽고, 미리 정한 업무 유형 중 어떤 것에 해당하는지 판단하는 과정입니다.

상담 접수 담당자가 “이 문의는 반품 배송비 안내이고, 저 문의는 반품 접수구나”라고 나누는 일을 생각해 보세요.

| 고객 문의 | 고객이 원하는 일 | 분류 코드 |
|---|---|---|
| 반품 배송비는 얼마인가요? | 반품 배송비 안내 | RETURN:FEE |
| 상품을 반품하고 싶어요. | 반품 접수 | RETURN:REQUEST |
| 접수한 반품을 취소할게요. | 반품 접수 철회 | RETURN:WITHDRAW |

세 문장에 모두 **반품**이라는 단어가 있지만 원하는 처리는 다릅니다. 따라서 단어가 있는지만 확인해서는 업무를 정확히 구분하기 어렵습니다. 모델은 문장에서 원하는 일을 추정하며, 추정이 틀릴 가능성도 있습니다.

### 의도·추출 정보·업무 실행은 각각 무엇일까요?

“주문 1234의 반품 배송비를 알려줘”라는 문의를 나눠 봅니다.

| 구분 | 확인하는 내용 | 예시 |
|---|---|---|
| 의도 분류 | 어떤 도움을 원하는가? | 반품 배송비 문의 |
| 정보 추출 | 처리에 필요한 구체적인 값은 무엇인가? | 주문번호 1234 |
| 업무 실행 | 그 요청을 실제로 어떻게 처리하는가? | 주문·정책을 조회해 배송비 안내 |

이 샘플은 첫 번째인 **의도 분류**를 구현합니다. 주문번호 추출과 실제 조회·접수는 이후 기능에서 구현할 책임입니다. `RETURN:FEE`를 골랐다는 사실만으로 배송비가 계산되거나 반품이 접수된 것은 아닙니다.

기획자는 먼저 업무 이름과 경계를 정합니다. 예를 들어 “반품 접수”와 “반품 접수 철회”를 어떤 문의로 구분할지, 정보가 부족하면 무엇을 되물을지 정합니다. 개발자는 그 분류표를 모델에 전달하고 반환된 코드를 검사합니다.

## 2. 분류표의 필드와 교육 용어를 연결하기

| 분류표 필드 | 한글 의미 | 예시 |
|---|---|---|
| main_intent | 대분류: 어떤 큰 업무인가? | RETURN: 반품 |
| sub_intents | 해당 대분류의 세부 업무 목록 | 접수·배송비·수거·반품 철회 |
| sub_intent | 소분류 코드: 어떤 세부 업무인가? | FEE: 반품 배송비 문의 |
| description | 업무의 의미와 포함·제외 기준 | 반품 배송비 문의 |

상하위 업무를 나눈 목록을 **의도 트리(intent tree)** 또는 **상담 업무 분류표(taxonomy)**라고 합니다. 계층형 분류는 이런 상하위 관계를 이용한 분류입니다. 모델을 반드시 두 번 호출해야 한다는 뜻은 아닙니다.

다음은 [intent-catalog.json](../../../labs/13-05-intent-classification/src/main/resources/intent-catalog.json)에서 RETURN 대분류만 남긴 발췌입니다. 실제 `intents` 목록에는 대분류 10개와 각각 4개의 소분류가 있습니다.

```json
{
  "version": "shopping-intents-v2",
  "intents": [
    {
      "main_intent": "RETURN",
      "description": "받은 상품의 반품 접수·배송비·수거·철회. 환불 입금은 REFUND",
      "sub_intents": [
        {
          "sub_intent": "REQUEST",
          "description": "받은 상품을 돌려보내는 반품 접수"
        },
        {
          "sub_intent": "FEE",
          "description": "반품 배송비 문의"
        },
        {
          "sub_intent": "PICKUP",
          "description": "반품 상품 수거 일정 문의"
        },
        {
          "sub_intent": "WITHDRAW",
          "description": "이미 접수한 반품 철회. 주문 취소는 ORDER:CANCEL"
        }
      ]
    }
  ]
}
```

AWS 원문의 `sub_intents`는 이름 문자열 목록입니다. 이 실습은 업무 경계를 한글로 설명하려고 각 항목을 `sub_intent`와 `description`을 가진 객체로 확장했습니다. `RETURN:FEE`는 두 코드를 읽기 쉽게 붙인 표기이며, 응답 JSON에서는 두 필드로 나눕니다.

같은 FEE가 반품과 교환에 모두 있을 수 있으므로 소분류 이름만으로 식별하지 않습니다. 반품 RETURN과 환불 REFUND도 구분합니다. 여기의 대분류는 상담 업무이고, 기본 상담 예제 `Intent.category`의 음료·생활 같은 상품 종류와는 다릅니다.

`version`은 분류표 버전입니다. 코드와 응답 구조가 바뀌어 현재는 `shopping-intents-v2`를 사용합니다. v1의 `majors`·`children`, `majorCode`·`minorCode` 대신 명시적인 의도 필드를 사용합니다.

## 3. AWS 방식과 Spring AI 구현의 연결

AWS 예제는 전체 의도 트리를 JSON으로 만들고 프롬프트에 넣은 뒤, 한 문의마다 대분류와 소분류를 함께 반환하도록 요청합니다. **각 분류 후보는 모델이 선택할 수 있도록 제공한 업무 항목**입니다.

| AWS 예제의 구성 | 이 실습의 대응 |
|---|---|
| INTENTS의 main_intent/sub_intents | 쇼핑 업무 10개·소분류 40개를 담은 JSON |
| json.dumps(INTENTS, indent=4) | ObjectMapper가 전체 intents 목록을 JSON 문자열로 변환 |
| system.format(intents=intents_json) | 프롬프트의 `{intents_json}` 자리에 JSON을 넣음 |
| #### 사이에 의도 트리 삽입 | 같은 구분 표시로 트리 범위를 표시 |
| OUTPUT EXAMPLE로 출력 모양 제시 | 한국어 문의·JSON 응답 예시를 함께 제공 |
| 한 문의에 main-intent:sub-intent 반환 | 한 번의 Spring AI 요청으로 두 필드를 가진 객체를 받음 |

SageMaker 배포·Python·보험 업무·문자열 출력은 이 실습에서 바꾸었습니다. **JSON 객체 변환·보류 판단·Java의 코드 조합 검증은 이 샘플에서 추가한 처리**입니다. AWS 글의 프롬프트 예제를 그대로 Spring AI 공식 샘플이라고 표현하지 않습니다.

같은 문의를 따라가 봅니다. 다음은 기대한 분류가 나온 경우이며 실제 모델 선택은 달라질 수 있습니다.

| 순서 | 담당 | 입력·처리 | 결과 예시 |
|---|---|---|---|
| 1 | Java | 전체 의도 트리·분류 규칙·출력 예시 준비 | 프롬프트 구성 |
| 2 | Java → 모델 | 고객 문의 “반품 배송비 부담 기준이 궁금합니다.” 전달 | 모델 요청 한 번 |
| 3 | 모델 → Spring AI | 대분류 RETURN과 그 아래 FEE를 함께 응답 | `{"decision":"MATCH","main_intent":"RETURN","sub_intent":"FEE"}` |
| 4 | Java | RETURN 아래에 FEE가 실제 등록되어 있는지 검사 | 코드 조합 검증 |
| 5 | Java | 안내 문구·분류표 버전을 추가 | API 응답 |

모델은 프롬프트에 들어간 전체 트리를 읽고 조합을 고릅니다. Java가 대분류만 먼저 골라 두 번째 모델 요청으로 넘기는 이전 방식과 다릅니다. 그래서 `stage`, `Option`, 단계별 `choose()` 호출을 제거했습니다.

## 4. 개발자 코드 읽기

### 4-1. 파일 읽기와 모델 학습을 구분합니다

[Application](../../../labs/13-05-intent-classification/src/main/java/com/example/ailearning/l13intent/IntentClassificationApplication.java)은 시작할 때 JSON 파일을 [IntentCatalog](../../../labs/13-05-intent-classification/src/main/java/com/example/ailearning/l13intent/domain/IntentCatalog.java)로 읽습니다. Service는 생성자 주입으로 이 객체를 받습니다. 모델을 다시 학습시키는 작업은 아닙니다.

Java는 `mainIntent`·`subIntents`처럼 이름을 쓰고 JSON은 `main_intent`·`sub_intents`를 씁니다. `@JsonProperty("main_intent")` 같은 Jackson 애너테이션이 둘을 연결합니다. 단어의 번역이나 상하위 관계를 모델이 알아서 추측하도록 맡기는 설정은 아닙니다.

### 4-2. JSON 의도 트리를 실제 프롬프트에 넣습니다

[프롬프트 파일](../../../labs/13-05-intent-classification/src/main/resources/prompts/intent-classification.txt)에 업무 구조의 뜻, 보류 규칙, 문의·출력 예시를 적었습니다. 마지막 부분은 다음과 같습니다.

```text
상담 의도 트리:
####
{intents_json}
####
```

다음은 Service 생성자의 실제 구문 발췌입니다.

```java
        String intentsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalog.intents());
        String template = new ClassPathResource("prompts/intent-classification.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        // JSON 중괄호를 템플릿 문법으로 해석하지 않고 이 자리표시자 하나만 치환한다.
        this.classificationInstruction = template.replace("{intents_json}", intentsJson);
```

`writeValueAsString()`은 파일에서 읽은 전체 `catalog.intents()` 목록을 JSON 문자열로 만듭니다. 프롬프트의 자리표시자 `{intents_json}`를 이 문자열로 바꿉니다. 버전은 API 응답용으로 보관하고, 모델에는 업무 목록을 전달합니다.

`####`는 분류표의 범위를 읽기 쉽게 표시한 문자열입니다. Spring AI의 특수 문법이나 보안 격리 장치가 아닙니다. JSON과 프롬프트는 시작 시 한 번 읽으므로 변경 뒤 서버를 재시작합니다.

### 4-3. 한 번의 요청으로 두 코드를 받습니다

[Service.classify()](../../../labs/13-05-intent-classification/src/main/java/com/example/ailearning/l13intent/IntentClassificationService.java)의 실제 메서드 발췌입니다.

```java
    public Result classify(String question) {
        Prediction prediction;
        try {
            // 전체 의도 트리와 출력 예시는 system, 고객 문의는 user에 넣고 한 번 요청한다.
            prediction = client
                    .prompt()
                    .system(classificationInstruction)
                    .user(question)
                    .call()
                    .entity(Prediction.class);
            validatePrediction(prediction);
        } catch (RuntimeException error) {
            // 문의·응답 원문 대신 실패 위치와 예외 타입을 기록하고 원인 예외를 보존한다.
            log.warn("Intent classification failed: stage=CLASSIFY, errorType={}", error.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 분류 응답을 처리하지 못했습니다.", error);
        }
        return createResult(prediction);
    }
```

`.system(...)`은 트리·규칙·출력 예시, `.user(question)`은 고객 원문입니다. `.call().entity(Prediction.class)`로 요청을 실행하고 JSON 응답을 Java record인 Prediction으로 변환합니다. record는 여러 값을 묶는 Java 자료형입니다.

| 타입 | 역할 |
|---|---|
| MainIntent·SubIntent | JSON 분류표의 대분류와 그 아래 소분류 |
| Prediction | 한 번의 모델 응답: 판단·대분류 코드·소분류 코드 |
| Result | API 응답: Prediction의 값에 안내 문구·분류표 버전 추가 |

프롬프트의 입력·출력 예시는 모델을 재학습시키는 데이터가 아니라, 이번 요청에서 해야 할 일을 설명하는 자료입니다. 예시를 준다고 모델이 항상 같은 판단을 한다고 보장할 수는 없습니다.

### 4-4. 응답 형식과 분류 코드의 유효성을 나눠 검사합니다

JSON 변환 후 `validatePrediction()`은 다음을 검사합니다.

- 판단이 있는가?
- MATCH라면 두 코드가 **같은 대분류 아래에 등록된 조합**인가?
- 미확정 판단이라면 두 코드가 모두 null인가?

조합 검사 자체는 Spring이나 모델 없이 사용할 수 있는 `IntentCatalog.containsPair()`가 담당합니다. 다음은 실제 메서드입니다.

```java
    public boolean containsPair(String mainIntent, String subIntent) {
        return intents.stream()
                .anyMatch(main -> main.mainIntent().equals(mainIntent)
                        && main.subIntents().stream()
                                .anyMatch(sub -> sub.subIntent().equals(subIntent)));
    }
```

예를 들어 RETURN은 반품 대분류이고 TIMING은 환불 입금 기간 소분류입니다. 두 코드가 각각 존재해도 RETURN:TIMING은 등록된 조합이 아니므로 거부합니다. RETURN:FEE와 EXCHANGE:FEE는 각각 등록된 조합이어서 둘 다 사용할 수 있습니다.

`createResult()`는 검증한 분류 값과 안내 문구·버전을 묶습니다. MATCH는 코드 조합 검사 통과를 뜻하며, 고객 의도를 정확히 이해했거나 반품 업무를 완료했다는 보장은 아닙니다.

## 5. 보류 판단과 모델 오류

Decision은 네 가지 판단을 나열한 Java enum입니다.

| 판단 | 한글 의미 | 처리 예시 |
|---|---|---|
| MATCH | 한 업무 선택 | 반품 배송비 문의를 RETURN:FEE로 선택하고 조합 검사 통과 |
| CLARIFY | 문의 내용 추가 확인 필요 | “반품 관련해서 도와주세요.” → 세부 내용을 질문 |
| OUT_OF_SCOPE | 제공하는 업무 범위 밖 | “내일 날씨 알려줘.” → 지원 범위 밖 안내 |
| MULTIPLE | 여러 업무를 함께 요청 | “배송 위치와 포인트 잔액을 알려줘.” → 먼저 처리할 요청 확인 |

현재는 전체 문의를 한 번에 분류하므로 미확정 판단이면 **main_intent와 sub_intent가 모두 null**입니다. 이전 구현처럼 대분류만 먼저 반환하지 않습니다. 이는 샘플에서 정한 보류 정책입니다.

API는 message 안내 문구를 반환합니다. 화면에 질문을 표시하고 다음 답변을 받아 앞 문의와 합쳐 재분류하는 대화 처리는 호출하는 애플리케이션이 구현해야 합니다. 이 샘플에는 대화 저장이나 업무 실행 도구가 없습니다.

| HTTP 상태 | 의미 |
|---|---|
| 200 | 모델 응답을 처리함. 분류 선택뿐 아니라 보류일 수도 있음 |
| 400 | 공백·2000자 초과 입력. 모델 요청 전 거부 |
| 502 | 통신 실패·JSON 변환 실패·잘못된 코드 조합·판단과 코드의 모순 |

502는 샘플의 오류 정책이며, 원인 예외를 보존하고 실패 위치와 예외 타입을 로그에 남깁니다. 고객 원문이나 모델 응답을 로그에 넣어 진단하도록 요구하지 않습니다.

## 6. 실행과 확인

명령은 모두 **SpringShoppingAI 저장소 루트**에서 실행합니다. 먼저 API 키 없이 단위 테스트와 로컬 고정 응답 테스트를 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentCatalogUnitTest' --tests '*IntentClassificationLocalModelTest' :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootJar
```

실제 실행에는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`을 사용합니다. 임베딩은 사용하지 않습니다. 준비 방법은 [실행 안내](../../run-guide.md)를 따릅니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootRun
curl -sS http://127.0.0.1:8107/api/intents/catalog
curl -sS http://127.0.0.1:8107/api/intents \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '미개봉 상품의 반품 배송비 부담 기준이 궁금합니다.'
```

응답 형태 예시입니다. 실제 모델의 선택은 달라질 수 있습니다.

```json
{"decision":"MATCH","main_intent":"RETURN","sub_intent":"FEE","message":"분류 후보를 반환합니다.","taxonomyVersion":"shopping-intents-v2"}
```

SDK 자동 재시도는 0회입니다. 유효한 입력을 정상 처리하면 모델 요청은 한 번이며, 빈 질문은 요청 전에 거부합니다. 실제 실행·Live에는 외부 API 사용량이 발생합니다. 종료는 Ctrl+C입니다.

[단위 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/domain/IntentCatalogUnitTest.java)는 코드 조합 검사를 확인합니다. [로컬 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/IntentClassificationLocalModelTest.java)는 실제 Spring AI 요청에 JSON 트리·출력 예시가 들어가는지, 요청이 한 번인지, 변환·보류·오류 처리가 맞는지 검사합니다. 고정 모델 응답을 사용하므로 실제 한국어 분류 정확도 검사는 아닙니다.

## 7. 상용 사례: 업무를 나누고 담당 처리로 연결하기

아래는 2026-11-10에 확인한 제공 업체의 공개 자료입니다. 고객 도입 사례와 제품 설명·가상 예제를 구분합니다.

| 자료 | 공개된 처리 방식 | 이번 실습에 반영할 점 |
|---|---|---|
| NatWest의 Amazon Connect·Lex 도입 사례 | 1,600개가 넘는 의도를 입구 역할의 Gateway 봇이 큰 업무로 나누고 하위 봇에서 상세 의도를 확인. 처음 발화를 하위 봇에 전달 | 업무를 나누는 상용 비교 사례. 이번 단일 호출 구현과는 구분 |
| PEBA의 Dynamics 365 Customer Service 도입 사례 | 2026-04-23 운영 시작. 문의를 의료·치과·가입 등 업무 유형으로 분류해 담당 처리로 연결 | 분류 결과를 담당 처리로 연결하는 것을 라우팅(routing)이라고 함. 이 자료가 2단계 LLM 분류나 10×4 분류표를 사용했다고 공개한 것은 아님 |
| AWS의 LLM 정보 추출 기술 예제 | 가상 보험 상담에서 정해진 대분류·소분류 목록과 출력 예시를 프롬프트에 제공 | 자유로운 이름 생성 대신 관리되는 코드에서 선택. 실제 고객 도입 사례는 아님 |

출처: [NatWest 도입 사례](https://aws.amazon.com/blogs/contact-center/simplifying-banking-self-service-at-natwest-using-amazon-connect-and-amazon-lex/), [PEBA 도입 사례](https://www.microsoft.com/en/customers/story/27179-public-employee-benefits-administrators-dynamics-365-customer-service), [AWS의 분류·하위 의도 예제](https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/).

NatWest의 Lex 봇 분리는 **대규모 의도를 업무별로 나누는 근거**로 참고합니다. 이를 그대로 옮겨 40개 의도에도 여러 봇이나 여러 Agent가 필수라고 해석하지 않습니다. 현재 실습은 AWS의 의도 트리 예제처럼 전체 목록으로 한 번 요청하며, NatWest 사례를 그대로 구현한 것은 아닙니다.

상용 제품의 보류 처리도 참고할 수 있습니다. Dialogflow CX는 후보 점수가 정해 둔 최소 점수(임계값)에 못 미치면 일치하는 의도 없음(no-match)으로 처리합니다. 이는 문장의 뜻을 분석하는 해당 제품의 모델(NLU)이 제공하는 점수 체계이며, LLM이 스스로 출력한 `confidence=0.9`를 같은 의미로 사용할 근거는 아닙니다. [Dialogflow CX의 의도 매칭](https://docs.cloud.google.com/dialogflow/cx/docs/concept/intent)

## 8. 상용 설계: 단일 분류와 2단계를 비교하기

| 방식 | 장점 | 주의할 점 |
|---|---|---|
| 전체 의도 트리를 한 번에 제시 — 현재 구현 | 모델 요청 한 번. 소분류 간 직접 비교 가능 | 설명과 예시가 길어지고 유사한 업무의 경계가 흐려질 수 있음 |
| 대분류 → 해당 소분류 | 각 단계의 후보가 적고 업무별 설명을 관리하기 쉬움 | 두 번의 순차 호출. 대분류를 틀리면 정답 소분류가 다음 후보에서 사라짐 |
| 검색으로 후보를 찾은 뒤 분류 | 더 큰 분류표에서 프롬프트에 넣을 후보 수를 줄일 수 있음 | 후보 검색에서 정답을 놓치면 분류로 복구하기 어려움. 실제 정답 코드가 검색 후보에 포함됐는지도 평가해야 함 |

**40개 정도면 한 번에 분류하는 방식을 비교 기준으로 먼저 평가할 수 있습니다.** 후보 수만으로 2단계가 더 정확하거나 저렴하다고 단정할 수 없습니다. 이번 샘플은 AWS 예제를 참고해 전체 트리를 한 번에 전달합니다. 이는 구현을 단순하게 하는 선택이며, 정확도나 총 토큰 비용이 더 낫다는 검증 결과는 아닙니다. 한 요청에 전체 목록을 넣으므로 입력 토큰이 늘 수도 있습니다.

## 9. 상용 품질 평가와 업무 연결

[로컬 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/IntentClassificationLocalModelTest.java)는 후보 축소·코드 소속 검사·보류 처리·입력/모델 오류를 확인합니다. 모델이 어떤 응답을 반환할지 미리 정한 테스트이므로 실제 한국어 문의를 올바르게 분류했는지 평가하지는 않습니다.

[Live 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/IntentClassificationOpenAiLiveTest.java)는 반품 수거 일정과 환불 입금 시점 문의 두 사례의 대분류·소분류를 확인합니다. 전체 40개 의도의 품질 검증은 아닙니다. 같은 질문 세트로 여러 모델을 비교하는 실습은 [13-6](06-intent-quality-evaluation.md)에서 별도로 다룹니다.

```bash
# 실제 OpenAI 호출이며 API 사용량이 발생합니다.
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentClassificationOpenAiLiveTest'
```

### 분류 기준과 평가 질문 준비하기

업무 담당자가 실제 문의에서 개인정보를 제거하고 대분류·소분류·보류 정답을 붙입니다. 프롬프트에 넣은 예시와 평가할 문장은 분리합니다. 전체 소분류뿐 아니라 주문 취소/반품 철회, 반품/환불 같은 혼동 쌍, 범위 밖, 같은/다른 대분류의 복수 요청도 포함합니다.

상품 가격·주문번호 같은 값은 별도 정보로 추출하고, 값이 바뀔 때마다 새 의도를 만들지는 않습니다. 어떤 문의를 포함하고 제외할지 먼저 합의해야 평가의 정답도 일관되게 정할 수 있습니다.

### 정밀도·재현율을 쉬운 말로 읽기

| 평가 항목 | 확인하는 내용 |
|---|---|
| 대분류 정확도 | 정답이 있는 문의에서 큰 업무를 올바르게 골랐는가? |
| 최종 소분류 정답 비율 | 평가 대상 전체 문의 중 마지막 세부 업무까지 맞힌 비율은 얼마인가? |
| 코드별 정밀도(precision) | 이 코드라고 분류한 문의 중 실제로 맞은 비율 |
| 코드별 재현율(recall) | 실제로 이 코드에 해당하는 문의 중 올바르게 찾아낸 비율 |
| 보류·오분류 비율 | 필요한 추가 확인을 했는가, 답할 수 있는 문의를 지나치게 보류하거나 잘못 분류하지 않았는가? |
| 토큰·지연 | 입출력 텍스트를 나누어 처리하는 단위인 토큰의 수와 응답까지 걸린 시간은 얼마인가? |

가상의 평가 질문 100개 중 실제 반품 배송비 문의가 40개라고 가정합니다. 모델이 RETURN:FEE로 고른 문의는 50개이고, 그중 정답이 30개라면 정밀도는 **30/50=60%**, 재현율은 **30/40=75%**입니다. 잘못 골라 넣은 문의와 찾아내지 못한 문의를 나누어 보는 지표입니다. 이 숫자는 샘플을 실행해 얻은 결과가 아닌 설명용 가정입니다.

같은 평가 질문으로 단일 호출과 2단계를 비교합니다. 2단계 방식을 비교할 때 소분류 호출까지 도달한 문의만 평가하면 상위 오분류나 보류를 놓칠 수 있습니다. 범위 밖·복수 요청도 기대한 보류 판단과 따로 비교하고, 보류한 문의를 빼서 정확도가 높아 보이게 하지 마세요.

모델이 스스로 낸 confidence 점수를 정답 확률로 취급하지 않습니다. 보류 판단 역시 추정이므로 평가해야 합니다. 재질문이 반복되면 상담원에게 연결하는 정책도 정합니다.

### 분류 결과를 실제 업무에 연결하기

검증한 코드를 허용된 처리기로 연결하는 것이 라우팅입니다. 예를 들어 RETURN:FEE는 반품 배송비 안내 처리로 연결할 수 있습니다. 모델이 만든 메서드명이나 URL을 직접 실행하지 않습니다.

취소·환불 같은 변경 작업은 주문 소유자·상태·금액·사용자 의사를 업무 서버에서 재확인합니다. 분류 결과가 권한이나 실행 승인을 대신하지는 않습니다.

실제 상용 데이터로 단일/2단계 품질·비용을 비교하는 자동 평가 실행기는 포함하지 않습니다. 분류표와 코드를 분리했으므로 소분류 수를 3개 또는 5개로 바꿔도 서비스 분기를 추가할 필요는 없습니다.

## 확인 질문

1. “주문 1234의 반품 배송비를 알려줘”에서 의도 분류·정보 추출·업무 실행은 각각 무엇인가요?
2. 의도 트리를 JSON으로 전달하면 대분류·소분류를 반드시 두 번의 모델 요청으로 나눠야 하나요?
3. 이 샘플은 AWS의 의도 분류 예제에서 무엇을 참고했고, Spring AI에 맞춰 무엇을 바꿨나요?
4. RETURN과 TIMING이 각각 등록된 코드여도 RETURN:TIMING을 거부하는 이유는 무엇인가요?
5. 분류표를 수정하면 모델을 새로 학습시키는 것인가요? 코드 검증 통과만으로 실제 의도가 맞다고 할 수 있나요?

<details>
<summary>해설</summary>

1. 의도는 반품 배송비 문의, 추출 정보는 주문번호 1234, 업무 실행은 해당 주문·정책을 조회해 배송비를 안내하는 처리입니다. 이 샘플은 의도 분류를 구현합니다.
2. 아닙니다. 계층은 업무의 상하위 관계이고 호출 횟수와는 별개입니다. 이번에는 전체 트리를 한 번에 제공해 두 코드를 함께 받습니다. 요청을 나누는 방식과의 품질·비용 차이는 실제 문의로 비교해야 합니다.
3. 전체 의도 트리·JSON 삽입·구분 표시·출력 예시를 참고했습니다. 보험 업무를 쇼핑 업무로, SageMaker 호출을 Spring AI 호출로, 문자열 응답을 구조화 JSON 응답으로 바꾸었습니다. 보류 정책과 Java 조합 검증도 추가했습니다.
4. TIMING은 REFUND 아래의 소분류이기 때문입니다. 전체 코드의 존재 여부뿐 아니라 선택한 대분류 안에 속하는지도 확인해야 합니다.
5. 아닙니다. 파일을 읽어 프롬프트에 후보 설명을 넣는 것이므로 재학습이 아니며 변경 후 재시작합니다. 유효한 조합도 고객의 뜻과 다를 수 있으므로 실제 문의의 정답과 비교해야 합니다.

</details>

[의도 추출 기본 수업으로](02-intent.md) · [13장 목차](../13-shopping-agent.md) · [교육 계획](../../curriculum.md)

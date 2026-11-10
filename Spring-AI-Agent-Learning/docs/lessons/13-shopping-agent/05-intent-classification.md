# 13-5. 대분류·소분류로 상담 의도 분류하기

**실행 코드·설정·테스트:** [labs/13-05-intent-classification](../../../labs/13-05-intent-classification/README.md).

**이번 목표:** 대분류 10개·소분류 총 40개인 상담 분류표에서 후보를 단계적으로 좁히고, 애매한 문의를 확정하지 않는 방법을 익힙니다.

**권장 시점:** [13-2 의도 추출](02-intent.md)을 읽은 뒤 선택해서 학습합니다. 04장의 구조화 응답을 사용합니다. 기존 `CounselWorkflow`를 수정하지 않는 독립 실습입니다.

## 1. 분류가 많아지면 무엇을 바꿀까요?

기본 예제는 상품 추천 `SHOPPING`과 정책 질문 `POLICY`를 구분합니다. 상용 상담은 주문·배송·반품·환불처럼 업무를 나누고, 반품 안에서도 접수·배송비·수거·철회를 구분해야 합니다.

이렇게 상위 업무에서 하위 업무로 내려가는 방식을 **계층형 의도 분류**라고 합니다. 업무별 이름과 포함 관계를 정리한 목록을 **분류표(taxonomy)**라고 부릅니다. 여기의 대분류는 상담 업무이며, 기본 예제 `Intent.category`의 음료·생활 같은 상품 카테고리와 다릅니다.

입력은 고객 문의 한 문장, 출력은 `RETURN` → `RETURN.FEE` 같은 업무 코드입니다. 예산·주문번호 등 세부 값 추출과 실제 업무 실행은 이후 단계의 책임입니다.

## 2. 공개된 상용 사례에서 참고할 점

아래는 2026-11-10에 확인한 제공 업체의 공개 자료입니다. 고객 도입 사례와 제품 설명·가상 예제를 구분합니다.

| 자료 | 공개된 처리 방식 | 이번 실습에 반영할 점 |
|---|---|---|
| NatWest의 Amazon Connect·Lex 도입 사례 | 1,600개가 넘는 의도를 Gateway가 큰 업무로 나누고 하위 봇에서 상세 의도를 확인. 처음 발화를 하위 봇에 전달 | 상위 분류 후 후보 범위를 좁히되 원문을 다음 단계에도 전달 |
| PEBA의 Dynamics 365 Customer Service 도입 사례 | 2026-04-23 운영 시작. 문의를 의료·치과·가입 등 업무 유형으로 분류해 라우팅 | 분류는 업무 담당 흐름을 고르는 입구. 이 자료가 2단계 LLM 분류나 10×4 분류표를 사용했다고 공개한 것은 아님 |
| AWS의 LLM 정보 추출 기술 예제 | 가상 보험 상담에서 정해진 대분류·소분류 목록과 출력 예시를 프롬프트에 제공 | 자유로운 이름 생성 대신 관리되는 코드에서 선택. 실제 고객 도입 사례는 아님 |

출처: [NatWest 도입 사례](https://aws.amazon.com/blogs/contact-center/simplifying-banking-self-service-at-natwest-using-amazon-connect-and-amazon-lex/), [PEBA 도입 사례](https://www.microsoft.com/en/customers/story/27179-public-employee-benefits-administrators-dynamics-365-customer-service), [AWS의 분류·하위 의도 예제](https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/).

NatWest의 Lex 봇 분리는 **대규모 의도를 업무별로 나누는 근거**로 참고합니다. 이를 그대로 옮겨 40개 의도에도 여러 봇이나 여러 Agent가 필수라고 해석하지 않습니다. 이 실습은 같은 ChatClient로 두 번 요청합니다.

상용 제품의 보류 처리도 참고할 수 있습니다. Dialogflow CX는 후보 점수가 임계값에 못 미치면 no-match로 처리합니다. 이는 해당 NLU 제품의 점수 체계이며, LLM이 스스로 출력한 `confidence=0.9`를 같은 의미로 사용할 근거는 아닙니다. [Dialogflow CX의 의도 매칭](https://docs.cloud.google.com/dialogflow/cx/docs/concept/intent)

## 3. 30~50개 후보라면 어떤 방식이 적절할까요?

| 방식 | 장점 | 주의할 점 |
|---|---|---|
| 전체 소분류를 한 번에 제시 | 모델 요청 한 번. 소분류 간 직접 비교 가능 | 설명과 예시가 길어지고 유사한 업무의 경계가 흐려질 수 있음 |
| 대분류 → 해당 소분류 | 각 단계의 후보가 적고 업무별 설명을 관리하기 쉬움 | 두 번의 순차 호출. 대분류를 틀리면 정답 소분류가 다음 후보에서 사라짐 |
| 검색으로 후보를 찾은 뒤 분류 | 더 큰 분류표에서 프롬프트에 넣을 후보 수를 줄일 수 있음 | 후보 검색에서 정답을 놓치면 분류로 복구하기 어려움. 후보 포함률도 평가해야 함 |

**40개 정도면 한 번에 분류하는 방식을 비교 기준으로 먼저 평가할 수 있습니다.** 후보 수만으로 2단계가 더 정확하거나 저렴하다고 단정할 수 없습니다. 이번 샘플은 업무별 경계와 후보 제한을 배우기 위해 2단계를 선택합니다. 두 번 호출하므로 토큰·지연이 늘 수도 있습니다.

## 4. 실습의 분류표

전체 코드·설명은 [intent-catalog.json](../../../labs/13-05-intent-classification/src/main/resources/intent-catalog.json)에 있습니다. 대분류마다 소분류 4개, 총 40개인 학습용 목록입니다. 실제 쇼핑몰의 전체 업무 분류표는 아닙니다.

| 대분류 | 소분류 코드의 뒷부분과 의미 |
|---|---|
| PRODUCT 상품 | RECOMMEND 추천 / COMPARE 비교 / STOCK 재고 / DETAIL 상세 |
| ORDER 주문 | CREATE 생성 / STATUS 상태 / CANCEL 취소 / CHANGE 변경 |
| PAYMENT 결제 | METHOD 수단 / ERROR 오류 / RECEIPT 영수증 / DUPLICATE 중복 |
| DELIVERY 배송 | TRACK 추적 / ADDRESS 배송지 / DELAY 지연 / LOST 분실 |
| RETURN 반품 | REQUEST 접수 / FEE 배송비 / PICKUP 수거 / WITHDRAW 반품 철회 |
| EXCHANGE 교환 | REQUEST 접수 / STOCK 교환 재고 / FEE 배송비 / STATUS 상태 |
| REFUND 환불 | STATUS 상태 / TIMING 입금 기간 / AMOUNT 금액 / METHOD 수단 |
| BENEFIT 혜택 | COUPON 쿠폰 / POINT 포인트 / MEMBERSHIP 등급 / EVENT 행사 |
| ACCOUNT 계정 | LOGIN 로그인 / PROFILE 정보 / CLOSE 탈퇴 / PRIVACY 개인정보 |
| SUPPORT 상담 | DEFECT 불량 증상 / COMPLAINT 불만 / HUMAN 상담원 / DOCUMENT 증빙 |

실제 소분류 코드는 `RETURN.FEE`처럼 대분류를 포함합니다. 상품을 돌려보내는 **반품**과 돈을 돌려받는 **환불**, 주문 취소와 반품 접수 철회를 구분하는 설명을 분류표에 넣었습니다. JSON을 바꾼 뒤에는 서버를 재시작해야 반영됩니다.

코드는 외부에 노출되는 계약으로 관리하고, 설명을 다듬는 것과 코드 삭제·통합을 구분하세요. 분류표의 `version`을 함께 반환하면 어떤 기준으로 분류했는지 기록할 수 있습니다.

## 5. 코드 읽기: 모델은 선택하고 Java는 검증하기

먼저 [IntentClassificationService.classify()](../../../labs/13-05-intent-classification/src/main/java/com/example/ailearning/l13intent/IntentClassificationService.java)를 읽습니다. 다음은 실제 메서드 발췌입니다.

```java
    public Result classify(String question) {
        Choice major = choose(question, "대분류", catalog.majorOptions());
        if (major.decision() != Decision.MATCH) {
            return createResult(major.decision(), null, null);
        }

        // 두 번째 요청에는 선택한 대분류의 소분류만 제공한다.
        List<Option> children = catalog.minorOptions(major.code());
        Choice minor = choose(question, major.code() + "의 소분류", children);
        if (minor.decision() != Decision.MATCH) {
            return createResult(minor.decision(), major.code(), null);
        }
        return createResult(Decision.MATCH, major.code(), minor.code());
    }
```

`choose()`는 단계 이름·후보 코드·업무 설명을 system에, 고객 원문을 user에 넣습니다. `.entity(Choice.class)`가 JSON을 `Choice`로 변환합니다. 첫 요청에는 대분류 10개, 두 번째 요청에는 선택한 대분류의 소분류 4개만 제공합니다. 도구는 등록하지 않습니다. 사용자 문장을 지시가 아닌 분류 대상으로 다루도록 요청하지만, 프롬프트만으로 모델의 준수를 보장하지는 않습니다.

변환 뒤에는 `validateChoice()`가 다음 계약을 검사합니다.

- `MATCH`라면 코드가 **이번 요청의 후보 목록**에 있어야 합니다.
- 나머지 판단이라면 코드는 null이어야 합니다.
- `RETURN` 뒤에 `REFUND.TIMING`을 반환하면 실제 등록된 코드여도 거부합니다. 전체 목록에 존재하는지만 검사해서는 부모·자식 관계를 보장할 수 없습니다.

`Decision`은 Java enum으로 허용한 판단을 나열한 타입입니다. `MATCH`는 모델이 하나를 선택하고 Java의 코드·소속 검사를 통과했다는 뜻입니다. **고객의 실제 의도를 정확히 이해했거나 업무를 완료했다는 보장은 아닙니다.**

## 6. 애매함·범위 밖·복수 요청 처리

| 판단 | 예시와 처리 | 호출·출력 |
|---|---|---|
| MATCH | “미개봉 상품 반품 배송비 부담 기준이 궁금합니다.” → RETURN / RETURN.FEE | 정상 경로는 모델 요청 2회 |
| CLARIFY | “반품 관련해서 도와주세요.” → 세부 문의를 추가 확인 | 대분류만 선택했다면 majorCode만 남고 minorCode=null |
| OUT_OF_SCOPE | “내일 날씨 알려줘.” → 현재 업무 범위 밖으로 안내 | 대분류에서 보류하면 모델 요청 1회 |
| MULTIPLE | “배송 위치도 조회하고 포인트 잔액도 알려줘.” → 먼저 처리할 요청 확인 | 하나의 코드로 억지로 합치지 않음 |

표는 실제 모델에서 평가할 기대 사례입니다. 고정 응답 테스트는 이 판단을 주입해 분기·호출 수를 검사하며, 한국어 의미를 판별하는 능력을 검사하지 않습니다.

한 대분류 안의 복수 요청은 두 번째 단계에서 `MULTIPLE`이 될 수도 있습니다. 실제 모델이 복수 요청을 놓칠 수 있으므로, 상용 평가에는 같은 대분류·서로 다른 대분류의 복수 요청을 모두 넣습니다. 이 샘플은 요청 목록 분해와 병렬 실행을 추가하지 않고 먼저 처리할 일을 묻는 정책입니다.

HTTP 200의 보류 판단과 HTTP 502의 모델 오류도 구분합니다. 통신 실패·깨진 JSON·후보 밖 코드·코드와 판단의 모순은 502, 공백·2000자 초과 입력은 모델 요청 전 400입니다. 임의 분류로 오류를 덮지 않습니다. 로그에는 분류 단계와 예외 타입을 남깁니다.

## 7. 실행과 결과 확인

모든 명령은 **SpringShoppingAI 저장소 루트**에서 실행합니다. 먼저 API 키 없이 로컬 테스트와 빌드를 수행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentClassificationLocalModelTest' :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootJar
```

실제 실행에는 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`이 필요합니다. [공통 설정 안내](../../run-guide.md)를 따르며 임베딩 모델은 사용하지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:bootRun
curl -sS http://127.0.0.1:8107/api/intents/catalog
curl -sS http://127.0.0.1:8107/api/intents \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '미개봉 상품 반품 배송비 부담 기준이 궁금합니다.'
```

응답 형태 예시입니다. 실제 모델의 선택 결과는 달라질 수 있습니다.

```json
{"decision":"MATCH","majorCode":"RETURN","minorCode":"RETURN.FEE","message":"분류 후보를 반환합니다.","taxonomyVersion":"shopping-intents-v1"}
```

SDK 자동 재시도는 0회입니다. 성공한 2단계 분류는 Chat 요청 2회이며, 미확정 대분류나 오류에서 더 일찍 종료할 수 있습니다. 실제 실행·Live는 외부 API 사용량이 발생합니다. 서버 종료는 Ctrl+C입니다.

## 8. 테스트와 상용 적용 시 확인할 것

[로컬 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/IntentClassificationLocalModelTest.java)는 후보 축소·코드 소속 검사·미확정 처리·입력/모델 오류를 검증합니다. [Live 테스트](../../../labs/13-05-intent-classification/src/test/java/com/example/ailearning/l13intent/IntentClassificationOpenAiLiveTest.java)는 실제 모델의 반품 배송비 문의 한 사례를 확인합니다. 분류표 전체 품질을 평가한 결과는 아닙니다.

```bash
# 실제 OpenAI 호출: 별도 선택해서 실행합니다.
./gradlew :Spring-AI-Agent-Learning:labs:13-05-intent-classification:test --tests '*IntentClassificationOpenAiLiveTest'
```

상용 적용에서는 다음을 검토합니다.

1. **분류 경계:** 주문 취소/반품 철회, 반품/환불, 불량 상담/교환 접수처럼 혼동하기 쉬운 쌍에 포함·제외 기준과 대표 문장을 정합니다. “가격 3만원” 같은 값마다 새 의도를 만들기보다 별도 조건으로 추출합니다.
2. **정답이 있는 평가:** 업무 담당자가 실제 문의를 비식별화해 대분류·소분류·보류 정답을 붙입니다. 프롬프트 예시와 평가 문장을 분리하고, 40개 소분류와 혼동 쌍·범위 밖·복수 요청을 포함합니다.
3. **비교 지표:** 같은 평가 세트로 단일 호출과 2단계를 비교합니다. 대분류 정확도, 전체 문의 중 최종 소분류 정답 비율, 코드별 precision/recall, 보류·오분류 비율, 토큰·지연을 기록합니다. 소분류 호출까지 도달한 문의만 평가하면 상위 오분류를 놓칩니다. 보류를 제외한 정확도만 높이는 방식도 피합니다.
4. **확신과 재질문:** 모델이 스스로 낸 점수를 정답 확률로 사용하지 않습니다. 보류 판단도 모델의 추정이므로 정답 세트로 평가합니다. 재질문이 반복되면 상담원 연결 정책을 둡니다. 후속 답변은 앞 문의와 함께 해석해야 하며 이 샘플의 단일 문장 API에는 대화 저장이 없습니다.
5. **업무 실행:** 검증한 코드에서 허용된 처리기로 연결합니다. 모델이 만든 메서드명·URL을 직접 실행하지 않습니다. 취소·환불 같은 변경은 주문 소유자·상태·금액·사용자 의사를 업무 서버에서 재확인합니다. 분류 결과로 권한을 부여하지 않습니다.

실제 상용 데이터로 단일/2단계 품질·비용을 비교하는 자동 평가 실행기는 이 실습에 포함하지 않습니다. 코드와 분류표는 분리되어 있으므로, 소분류 수를 3개 또는 5개로 바꿔도 서비스 분기를 추가할 필요는 없습니다.

## 확인 질문

1. 대분류 10개·소분류 총 40개라고 해서 2단계 분류가 항상 단일 호출보다 정확하거나 저렴할까요?
2. 유효한 소분류 코드인데도 선택한 대분류 아래에 없으면 거부해야 하는 이유는 무엇인가요?
3. 구조화 응답과 코드 소속 검사를 통과한 결과에도 실제 문의의 정답 평가가 필요한 이유는 무엇인가요?

<details>
<summary>해설</summary>

1. 아닙니다. 2단계는 후보를 줄이지만 순차 호출 비용·지연이 생기고 상위 오분류가 전파됩니다. 같은 정답 세트에서 최종 분류 품질과 보류·호출 비용을 비교해 선택해야 합니다.
2. 단계별 후보 제한을 어긴 결과이기 때문입니다. 전체 등록 여부만 검사하면 RETURN을 고른 뒤 환불 기간 코드를 반환하는 모순을 통과시킬 수 있습니다. 코드 소속 검사와 고객 의도 정답 검사는 서로 다른 검증입니다.
3. 형식과 분류표 안의 조합이 올바르다는 것만 확인했기 때문입니다. 모델이 뜻을 잘못 이해해 다른 유효 코드를 고를 수 있으므로 실제 문의의 정답과 비교해야 합니다. 미확정 문의를 보류하는 판단도 함께 평가합니다.

</details>

[의도 추출 기본 수업으로](02-intent.md) · [13장 목차](../13-shopping-agent.md) · [교육 계획](../../curriculum.md)

# 04-3. 형식 오류와 값 오류 관찰

**실행 코드·설정·테스트:** [labs/04-structured-output](../../../labs/04-structured-output/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 읽기와 실습을 합친 시간이며, 단원 전체 예상 시간에 포함됩니다.

**이번 목표:** 변환 성공 후에도 Java 검증이 필요한 이유를 확인합니다.

**코드 읽기 안내:** 변환된 객체의 필드가 허용된 값인지 검사하는 코드와, 실패를 HTTP 응답으로 바꾸는 부분을 읽습니다.

## 생략한 조건과 숫자 0 구분하기

이 예제는 사용자가 말하지 않은 필드를 `null`로 받도록 요청합니다. 가격을 정하지 않은 경우와 0원을 지정한 경우는 검색 조건이 다릅니다.

| 사용자 입력 | `maxPrice` | 해석 |
|---|---|---|
| “음료를 추천해줘” | `null` | 가격 조건을 정하지 않음 |
| “0원 이하 음료를 찾아줘” | `0` | 가격 상한을 0원으로 지정함 |

말하지 않은 가격을 0으로 채우면 가격 제한이 없던 요청을 0원 이하로 제한하게 됩니다.

## 변환 뒤 값 검사하기

[SearchCriteria.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/SearchCriteria.java)의 전체 코드입니다.

```java
package com.example.ailearning.l04structured;

import java.util.Set;

/** null은 사용자가 조건을 말하지 않았다는 뜻이다. 0이나 기본 예산으로 채우지 않는다. */
public record SearchCriteria(String category, Integer maxPrice) {
    public void validate() {
        if (category != null && !Set.of("음료", "생활").contains(category)) {
            throw new IllegalArgumentException("category는 음료/생활 또는 null이어야 합니다.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw new IllegalArgumentException("maxPrice는 0 이상 또는 null이어야 합니다.");
        }
    }
}
```

객체 변환에 성공했더라도 카테고리나 가격이 허용 범위를 벗어날 수 있으므로 `validate()`로 검사합니다.
다음은 [CriteriaService.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaService.java)의 `extract()` 메서드 발췌입니다.

```java
    public SearchCriteria extract(String question) {
        SearchCriteria criteria;
        try {
            // Spring AI가 형식 지시를 추가하고 응답을 record로 변환한다.
            criteria = client.prompt().system(INSTRUCTION).user(question)
                    .call().entity(SearchCriteria.class);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "모델 응답을 조건 객체로 변환하지 못했습니다.", error);
        }
        if (criteria == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 응답이 비었습니다.");
        }
        try {
            // Java 객체로 바뀌었어도 업무 규칙은 별도로 검사한다.
            criteria.validate();
        } catch (IllegalArgumentException error) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
        }
        return criteria;
    }
```

`try/catch`는 실패를 HTTP 상태로 연결하는 Java/Spring 코드입니다. 모델 호출·형식 변환 문제는 502,
변환은 됐지만 허용되지 않는 값이면 422로 구분합니다. 이 실습은 형식 오류를 모델에 다시 고치게 하는 재시도나 제공 업체의 별도 구조 강제 설정을 추가하지 않습니다.

## 실행

명령은 모두 **SpringShoppingAI 저장소 루트**에서 실행합니다.
실제 모델을 사용할 때는 [01장 루트 .env 설정](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)을 먼저 합니다.
키나 모델 이름을 소스 파일에 넣지 않습니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:test
./gradlew :Spring-AI-Agent-Learning:labs:04-structured-output:bootRun
```

첫 명령은 실제 키 없이 로컬 고정 응답 서버를 사용합니다. 두 번째 명령은 실제 API를 사용하므로 비용이 발생할 수 있습니다.
다른 터미널에서:

```bash
curl -i http://127.0.0.1:8094/api/criteria \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '음료 중 1만5천원 이하 선물을 찾아줘.'
```

응답 **형태 예시**:

```json
{"category":"음료","maxPrice":15000}
```

원문 응답과 비교하려면 같은 요청을 `/api/criteria/text`에 보내세요. 자연어 응답은 실행마다 달라질 수 있습니다.
종료는 서버 터미널 Ctrl+C입니다. 포트 충돌 시 `LEARNING_PORT=...`를 지정하고 curl 주소도 바꿉니다.

## 실험과 실패 구분

| 입력/실험 | 확인할 것 |
|---|---|
| “선물 추천해줘” | 명시하지 않은 조건은 null. HTTP JSON에서는 null 필드가 생략될 수 있음 |
| “0원 이하 음료” | maxPrice=0과 null의 차이 |
| 모델이 음수 예산/허용되지 않은 카테고리를 반환 | Java 값 검증으로 422 |
| 모델이 JSON 대신 설명 문장을 반환 | 형식 변환 실패로 502 |
| 공백 질문 | Controller에서 400, 모델 호출 없음 |

실제 모델에게 잘못된 JSON을 요청해도 실패를 항상 재현할 수는 없습니다.
[StructuredOutputContractTest](../../../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)는 실제 Spring AI와 로컬 모델 API를 연결해 정상·누락·형식 오류·허용 범위를 벗어난 값를 확정적으로 재현합니다.
이는 자연어 이해 성능 평가가 아닙니다. 잘못 추출한 **양수** 예산은 값 검증을 통과할 수 있으므로 사용자 의도와 맞는지도 확인해야 합니다.

## 확인 질문

1. JSON 텍스트, SearchCriteria 객체, 원래 사용자 문장은 어떻게 다른가요?
2. `.entity()`가 성공했는데 왜 validate()를 호출하나요?
3. 사용자가 가격을 말하지 않았을 때 0으로 채우면 어떤 문제가 생길까요?

<details><summary>해설</summary>

JSON은 데이터 표현 문자열, record는 Java 코드가 접근할 객체, 사용자 문장은 해석할 입력입니다.
파싱 성공은 허용 값·의도 일치를 보장하지 않습니다. 0은 가격 조건이므로 누락과 구분해야 합니다.
문자열 응답과 객체 응답을 직접 비교하고, 테스트에서 실패 유형을 구분하면 완료입니다.
</details>

보충: [공식 Structured Output](https://docs.spring.io/spring-ai/reference/api/structured-output.html).

## 여기서 멈춰 확인하기

형식 변환 실패와 허용되지 않는 값은 각각 어떤 응답으로 구분하나요?

[이전](02-convert.md) · [다음](../04-structured-output.md) · [단원 목차](../04-structured-output.md) · [학습 방법](../../learning-guide.md)

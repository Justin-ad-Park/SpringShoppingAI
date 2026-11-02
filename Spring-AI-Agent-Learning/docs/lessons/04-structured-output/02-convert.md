# 04-2. 응답을 record로 받기

**실행 코드·설정·테스트:** [labs/04-structured-output](../../../labs/04-structured-output/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 단원 전체 시간은 목차의 기존 예상 시간에 포함됩니다.

**이번 목표:** content()와 entity()가 반환하는 값을 비교합니다.

**코드 읽기 안내:** 먼저 GiftCriteria의 필드, 다음 Service의 변환 호출을 읽으세요. Controller와 오류 응답의 세부 구현은 그 뒤에 읽습니다.

## 코드 읽는 순서

모듈: `labs/04-structured-output`, 패키지: `com.example.ailearning.l04structured`.
아래 파일은 교육 폴더 기준이며 실제 파일을 그대로 읽을 수 있습니다.

1. [SearchCriteria.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/SearchCriteria.java): 결과 형태와 값 검증
2. [CriteriaService.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaService.java): 모델 요청과 변환
3. [CriteriaController.java](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaController.java): HTTP 입력 연결
4. [application.yml](../../../labs/04-structured-output/src/main/resources/application.yml), [build.gradle](../../../labs/04-structured-output/build.gradle): 01장과 같은 설정, 포트만 8094

### 결과를 담는 record 전체 코드

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

`validate()`는 객체를 만든 뒤 서비스에서 호출합니다. 검증을 모델 프롬프트에만 맡기지 않습니다.

### 호출을 담당하는 서비스 전체 코드

```java
package com.example.ailearning.l04structured;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CriteriaService {
    static final String INSTRUCTION = """
            사용자 요청에서 상품 카테고리(category)와 상품 가격 상한(maxPrice)을 추출하세요.
            category는 음료/생활 중 사용자가 명시한 것만 쓰세요.
            가격은 원 단위 정수입니다. 말하지 않은 필드는 null로 두세요.
            배송비는 계산하지 마세요. 실제 상품을 검색하는 작업도 하지 않습니다.
            """;
    private final ChatClient client;

    public CriteriaService(ChatClient.Builder builder) {
        client = builder.build();
    }

    public String text(String question) {
        return client.prompt().system(INSTRUCTION).user(question).call().content();
    }

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
}
```

`.content()`는 문자열을 반환하고, `.entity(SearchCriteria.class)`는 형식 지시·파싱·타입 변환을 수행합니다.
두 경로는 모델에 전달되는 형식 지시가 다릅니다. 이 실습은 provider-native schema 강제나 자동 교정 재시도를 추가하지 않습니다.
502는 모델 연결/형식 변환 문제, 422는 파싱은 됐지만 업무 값이 허용되지 않는 경우로 나눴습니다.
`try/catch`는 실패를 HTTP 상태로 연결하기 위한 일반 Java/Spring 코드입니다.


## 여기서 멈춰 확인하기

코드를 닫고 “이번 입력 → 새로 하는 처리 → 확인할 결과”를 한 문장으로 적어 보세요.
설명하기 어렵다면 위 핵심 메서드만 다시 읽습니다. 테스트용 HTTP 서버 구현을 이해해야 다음으로 갈 수 있는 것은 아닙니다.

[이전](01-format.md) · [다음](03-validate.md) · [단원 목차](../04-structured-output.md) · [학습 방법](../../learning-guide.md)

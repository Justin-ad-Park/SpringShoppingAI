# 2. validate()의 예외가 HTTP 422가 되는 과정

이 사례는 JSON을 `SearchCriteria`로 변환한 **다음**에 발생합니다. JSON 형식이 맞아도 애플리케이션에서 허용하는 값인지 별도로 확인합니다. 웹 요청의 역할 구분은 [00 보충 문서](00-web-request-basics.md)에 있습니다.

## 1. 값 검증 실패를 재현하기

다음 모델 응답은 JSON 형식과 필드 타입이 맞으므로 `entity()` 변환에 성공합니다.

```json
{"category":"음료","maxPrice":-1}
```

생성된 record의 가격은 `-1`입니다. [SearchCriteria](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/SearchCriteria.java)의 `validate()`는 이 값을 거부합니다. 실제 코드 발췌입니다.

```java
if (maxPrice != null && maxPrice < 0) {
    throw new IllegalArgumentException("maxPrice는 0 이상 또는 null이어야 합니다.");
}
```

이 `throw`가 **최초 예외 발생 위치**입니다. `validate()`는 개발자가 작성한 일반 Java 메서드이며, `IllegalArgumentException` 자체에는 HTTP 상태가 없습니다. 같은 메서드의 카테고리 검사도 허용하지 않는 값을 발견하면 같은 종류의 예외를 던집니다.

## 2. 서비스에서 422 지정하기

[CriteriaService](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaService.java)는 변환에 성공하고 결과가 `null`이 아닌 것을 확인한 뒤 다음 코드를 실행합니다. 실제 코드 발췌입니다.

```java
try {
    criteria.validate();
} catch (IllegalArgumentException error) {
    throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, error.getMessage());
}
return criteria;
```

`validate()`가 던진 예외가 이 `catch`에 잡히면, 서비스는 **새 `ResponseStatusException`에 `HttpStatus.UNPROCESSABLE_ENTITY`를 지정**합니다. 이 상수의 숫자 값이 **422**입니다.

### 왜 UNPROCESSABLE_ENTITY를 사용할까?

**422**는 요청 내용의 형식과 문법은 이해했지만, 그 내용에 담긴 지시를 처리할 수 없는 경우를 나타냅니다. 현재 HTTP 표준의 이름은 **Unprocessable Content**이며, 실습 코드의 `UNPROCESSABLE_ENTITY`도 같은 숫자 422를 나타냅니다. [HTTP 표준의 422 설명](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.5.21)

이 실습은 **변환된 검색 조건의 값 검증 실패**를 별도로 구분하기 위해 422를 선택했습니다. 예를 들어 `{"category":"음료","maxPrice":-1}`은 JSON으로 읽을 수 있어 record 생성에는 성공하지만, 음수 가격은 `validate()`의 규칙을 만족하지 못합니다. 500으로 처리하는 JSON 변환 실패와 구분하여, ‘객체는 만들었지만 이 조건으로 처리를 진행할 수 없다’는 결과를 표현합니다.

다만 HTTP 표준의 422는 **요청 내용**에 관한 상태이고, 이 실습에서 검증하는 값은 **모델이 생성한 결과**입니다. 따라서 이 매핑은 학습용 API의 선택이며, 모델이 잘못 생성한 값까지 사용자 요청 오류로 보아야 한다는 뜻은 아닙니다. 실제 API에서는 실패가 사용자 조건 때문인지 모델 응답 때문인지에 따라 422 또는 500 등의 정책을 정해야 합니다. `IllegalArgumentException` 자체가 422를 결정하지는 않습니다.

`criteria.validate()`는 모델 호출·변환을 감싼 첫 번째 `try` 바깥에 있습니다. 따라서 여기서 발생한 예외는 변환 실패를 처리하는 첫 번째 `catch`가 아니라, 위의 별도 `catch`에서 422로 바뀝니다.

## 3. HTTP 응답과 테스트 결과 확인하기

현재 기본 구성에서 Spring MVC가 `ResponseStatusException`에 담긴 422를 HTTP 응답에 적용합니다. **record 생성 성공 → validate() 실패 → 서비스가 422 지정 → MVC가 HTTP 422 응답 처리**의 순서입니다.

[StructuredOutputContractTest](../../../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)의 `변환에_성공해도_허용되지_않는_값은_거부한다()`에서 확인하는 첫 번째 사례입니다. 실제 테스트 코드 발췌입니다.

```java
MODEL
        .chat()
        .setResponseText("{\"category\":\"음료\",\"maxPrice\":-1}");
assertThat(postQuestion("음료 추천").statusCode()).isEqualTo(422);
```

같은 테스트는 허용하지 않는 카테고리인 `"전자"`도 HTTP 422로 처리하는지 확인합니다. 두 경우 모두 모델 호출과 객체 변환은 이미 끝난 상태입니다.

## 선택: 예외 생성자와 MVC 내부 처리 확인하기

<details>
<summary>예외 메시지 전달과 HTTP 응답 적용 코드</summary>

현재 코드의 이름을 그대로 설명했으며, Spring Framework 7에서는 같은 422를 나타내는 `UNPROCESSABLE_CONTENT`가 권장 이름입니다.

현재 생성자는 원래 예외의 `getMessage()`를 이유로 전달합니다. 500 처리 코드와 달리 원래 예외 객체를 세 번째 인자로 넘기지는 않습니다. 예외 메시지의 내용이 상태 숫자를 결정하는 것도 아닙니다. **첫 번째 인자의 `HttpStatus`가 상태를 결정합니다.**

서비스가 던진 `ResponseStatusException`은 Controller를 지나 `DispatcherServlet`으로 전파됩니다. 현재 실습의 기본 MVC 예외 처리에서 `ResponseStatusExceptionResolver.resolveResponseStatusException()`이 `exception.getStatusCode().value()`를 읽습니다.

그 값은 422이며, `applyStatusAndReason()`이 `response.sendError(422, reason)`에 해당하는 처리를 수행합니다. **서비스에서 상태를 정하고, Spring MVC가 그 상태를 HTTP 응답에 적용하는 구조**입니다. 서비스만 직접 호출하면 HTTP 응답 대신 예외가 호출자에게 전달됩니다.

</details>

[이전: entity() 변환 실패와 500](01-entity-conversion-500.md) · [다음: 입력 검증 실패와 400](03-input-validation-400.md) · [04장 본문으로](../04-structured-output.md#5-실패는-테스트로-확인하기)

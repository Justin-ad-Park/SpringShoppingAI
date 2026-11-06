# 1. entity() 변환 실패가 HTTP 500가 되는 과정

04장 본문에서 정상 변환을 확인한 뒤 읽는 보충 설명입니다. HTTP 요청과 Controller의 관계가 낯설면 [웹 요청의 기본 흐름](00-web-request-basics.md)을 먼저 읽으세요.

## 1. 실패를 재현하기

로컬 테스트는 모델 응답을 JSON이 아닌 문자열로 고정합니다. 테스트 코드 발췌입니다.

```java
MODEL
        .chat()
        .setResponseText("이것은 JSON이 아닙니다.");
```

모델 응답은 받았지만 `.entity(SearchCriteria.class)`가 이 문자열을 record로 변환하지 못합니다. Spring AI가 사용하는 JSON 변환기 내부에서 예외가 발생하고, 서비스의 `catch (RuntimeException error)`까지 전달됩니다.

이 입력에서 발생하는 `StreamReadException`은 `RuntimeException`의 하위 클래스라서 해당 `catch`에 잡힙니다. 변환에 실패했으므로 `criteria.validate()`는 실행되지 않습니다.

## 2. 서비스에서 500 지정하기

[CriteriaService](../../../labs/04-00-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaService.java)의 실제 코드 발췌입니다.

```java
try {
    criteria = client
            .prompt()
            .system(SYSTEM_INSTRUCTION)
            .user(question)
            .call()
            .entity(SearchCriteria.class);
} catch (RuntimeException error) {
    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
            "모델 응답을 조건 객체로 변환하지 못했습니다.", error);
}
```

`catch`는 원래 변환 예외를 잡고 **새 `ResponseStatusException`을 던집니다.** 생성자에 전달한 첫 인자 `HttpStatus.INTERNAL_SERVER_ERROR`가 상태를 결정합니다. 이 상수의 숫자 값이 **500**입니다. 세 번째 인자 `error`는 원래 예외를 원인으로 보존합니다.

### 왜 INTERNAL_SERVER_ERROR를 사용할까?

**500 Internal Server Error**는 서버에서 예상하지 못한 상황이 발생해 요청을 처리하지 못했음을 의미합니다. [HTTP 표준의 500 설명](https://www.rfc-editor.org/rfc/rfc9110.html#section-15.6.1)

사용자는 챗봇에 정상적인 질문을 보냈지만, 서비스는 모델 응답을 검색 조건 객체로 변환하지 못했습니다. 이 API는 조건 추출 기능을 제공하는 하나의 서비스이므로, **서비스 내부에서 요청을 완수하지 못한 실패를 500으로 표현**합니다. 외부 모델이 HTTP 500을 반환했다는 뜻은 아닙니다. 모델 응답을 받은 뒤 우리 서비스가 500을 지정합니다.

이것은 이 API의 오류 처리 정책입니다. `entity()`가 자동으로 HTTP 500을 만드는 것은 아닙니다. 현재 `try`는 모델 호출·변환을 함께 감싸므로 다른 `RuntimeException`도 같은 `catch`에서 500으로 처리됩니다. 현재 테스트는 그중 잘못된 JSON 응답을 재현합니다.

## 3. HTTP 응답과 테스트 결과 확인하기

현재 기본 구성에서 Spring MVC는 서비스가 던진 `ResponseStatusException`의 상태를 읽어 HTTP 응답에 적용합니다. 즉, **JSON 변환 실패 → 서비스가 500 지정 → MVC가 HTTP 500 응답 처리**의 순서입니다.

서비스만 Java 코드로 직접 호출하면 예외가 전달됩니다. HTTP 상태를 확인하려면 아래 테스트처럼 HTTP 요청으로 호출해야 합니다.

[StructuredOutputLocalModelTest](../../../labs/04-00-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputLocalModelTest.java)의 `잘못된_JSON_응답은_변환_실패로_처리한다()`는 고정 응답을 설정한 뒤 다음을 확인합니다.

```java
assertThat(postQuestion("음료 추천").statusCode()).isEqualTo(500);
```

## 선택: 라이브러리 내부에서 예외 발생 지점 확인하기

기본 실습은 위 코드와 테스트로 이해할 수 있습니다. 정확한 클래스·메서드가 궁금할 때 아래 내용을 펼쳐 보세요.

<details>
<summary>Spring AI·Jackson의 호출 경로와 RuntimeException 상속 관계</summary>

아래 내용은 Spring AI 2.0.2와 Jackson 3.1.7에서 확인한 내부 구현입니다.

| 단계 | 실행하는 코드 | 역할 |
|---|---|---|
| 1 | `CriteriaService.extract()` → `.entity(SearchCriteria.class)` | 모델 응답을 `SearchCriteria`로 요청 |
| 2 | `DefaultChatClient.DefaultCallResponseSpec.entity()` | `BeanOutputConverter`를 만들고 모델 호출·변환 처리로 전달 |
| 3 | `doSingleWithBeanOutputConverter()` → `outputConverter.convert(stringResponse)` | 모델 응답 문자열을 변환기에 전달 |
| 4 | `BeanOutputConverter.convert()` → `jsonMapper.readValue(...)` | Jackson으로 JSON을 읽어 record 생성. 잘못된 JSON이면 여기서 파싱 예외 발생 |

### 실제 예외 발생 지점: Jackson의 JSON 파서

`CallResponseSpec`은 인터페이스입니다. 구현체인 `DefaultChatClient.DefaultCallResponseSpec.entity()`는 `BeanOutputConverter`를 만들어 변환 처리를 맡깁니다. Spring AI 2.0.2의 `BeanOutputConverter.convert()` 구현은 다음과 같습니다. **라이브러리 소스의 메서드 본문이며, 실습에 추가할 코드는 아닙니다.**

```java
public T convert(String text) {
    // 응답에 붙은 코드 블록 표시 등을 정리한다.
    text = this.textCleaner.clean(text);

    // 이 호출 안에서 Jackson이 JSON을 읽다가 예외를 던진다.
    return (T) this.jsonMapper.readValue(text, this.jsonMapper.constructType(this.type));
}
```

여기서 `jsonMapper`는 Jackson의 `JsonMapper` 객체이고, `readValue(String, JavaType)` 구현은 상위 클래스인 **`tools.jackson.databind.ObjectMapper`**에 있습니다. `BeanOutputConverter` 자체에 `throw new RuntimeException(...)`이 있는 것이 아니라, **호출한 Jackson 메서드 내부에서 발생한 예외가 밖으로 전파됩니다.**

현재 사용하는 Jackson **3.1.7**에서 `"이것은 JSON이 아닙니다."`를 읽도록 직접 실행해 확인한 경로는 다음과 같습니다. 아래는 주요 호출을 순서대로 정리한 것이며 일부 내부 호출은 생략했습니다.

```text
BeanOutputConverter.convert(text)
  → ObjectMapper.readValue(text, JavaType)
    → ObjectMapper._readMapAndClose(...)
      → ObjectMapper._initForReading(...)
        → ReaderBasedJsonParser.nextToken()
          → ReaderBasedJsonParser._handleOddValue(...)
            → ReaderBasedJsonParser._reportInvalidToken(...)
              → StreamReadException을 만들어 throw
```

**실제로 예외를 던지는 메서드는 `tools.jackson.core.json.ReaderBasedJsonParser._reportInvalidToken(String, String)`입니다.** 이 입력의 첫 단어인 `이것은`을 JSON의 시작으로 해석할 수 없어 `StreamReadException`을 던집니다. 내부의 `JsonParser._constructReadException(...)`은 예외 객체를 만드는 메서드이고, `_reportInvalidToken(...)`이 그 객체를 던집니다.

재현 시 예외 메시지의 첫 부분은 다음과 같습니다.

```text
tools.jackson.core.exc.StreamReadException: Unrecognized token '이것은'
```

이 내부 경로는 **현재 버전과 위 입력**에서 확인한 것입니다. 다른 형태의 잘못된 응답은 다른 Jackson 메서드나 예외로 실패할 수 있습니다.

### 왜 catch (RuntimeException error)에 잡힐까?

현재 Jackson 예외의 상속 관계는 다음과 같습니다.

```text
java.lang.RuntimeException
  └─ tools.jackson.core.JacksonException
       └─ tools.jackson.core.exc.StreamReadException
```

Java의 `catch`는 지정한 타입의 하위 클래스도 잡습니다. 따라서 `StreamReadException` 객체도 `catch (RuntimeException error)`에 잡힙니다. **이름이 정확히 `RuntimeException`인 객체를 새로 던져야 하는 것은 아닙니다.**

이 경로의 `BeanOutputConverter.convert()`와 Spring AI의 `entity()` 처리는 이 파싱 예외를 잡아 다른 예외로 바꾸지 않습니다. 예외가 호출 경로를 거슬러 `CriteriaService.extract()`의 `catch`까지 전파됩니다. 그곳의 `error`가 이 `StreamReadException` 객체를 가리킵니다.

즉, **Jackson이 파싱 예외를 던지고 → Spring AI 호출 밖으로 전파되고 → 서비스가 잡아 HTTP 상태가 있는 예외로 바꿉니다.** `entity()`가 HTTP 500를 직접 만들지는 않습니다.

</details>

<details>
<summary>MVC가 예외의 상태를 HTTP 응답에 적용하는 코드</summary>

서비스에서 던진 예외는 Controller를 지나 Spring MVC의 `DispatcherServlet`으로 전파됩니다. 현재 실습의 기본 MVC 예외 처리에서 `ResponseStatusExceptionResolver`가 이 예외를 처리합니다.

내부 처리를 읽기 쉽게 요약하면 다음과 같습니다. **직접 추가할 코드가 아니라 프레임워크 처리의 요약**입니다.

```java
int status = exception.getStatusCode().value(); // INTERNAL_SERVER_ERROR → 500
response.sendError(status, reason);
```

실제 구현은 `resolveResponseStatusException()`에서 상태 값을 읽고 `applyStatusAndReason()`에서 `sendError()`를 호출합니다. 이때 숫자가 HTTP 응답에 적용됩니다. 이후 오류 응답 본문을 만드는 과정과 상태 숫자를 정하는 과정은 구분해서 보면 됩니다.

서비스 메서드만 Java 코드로 직접 호출하면 `ResponseStatusException`을 받습니다. MVC를 거치는 HTTP 요청이어야 위 처리 끝에서 HTTP 500 응답을 받습니다.

</details>

[웹 요청의 기본 흐름](00-web-request-basics.md) · [다음: 값 검증 실패와 422](02-value-validation-422.md) · [04장 본문으로](../04-structured-output.md#5-실패는-테스트로-확인하기)

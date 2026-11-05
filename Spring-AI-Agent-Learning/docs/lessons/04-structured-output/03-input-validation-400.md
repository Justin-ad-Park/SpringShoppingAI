# 3. @NotBlank 입력 검증이 HTTP 400이 되는 과정

이 사례는 **Controller의 메서드 본문 실행 전**에 발생합니다. [웹 요청의 기본 흐름](00-web-request-basics.md)에서 본 것처럼, Spring MVC가 입력값을 준비하고 검증한 뒤 Controller를 실행합니다.

## 1. 공백 질문으로 실패 재현하기

[CriteriaController](../../../labs/04-structured-output/src/main/java/com/example/ailearning/l04structured/CriteriaController.java)의 실제 코드 발췌입니다.

```java
@PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
public SearchCriteria extract(@RequestBody @NotBlank @Size(max = 2000) String question) {
    return service.extract(question);
}
```

`@RequestBody`는 요청 본문을 질문 문자열로 읽으라는 선언입니다. `@NotBlank`는 `null`, 빈 문자열, 공백만 있는 문자열을 허용하지 않는 입력 규칙입니다. 테스트가 보내는 `"   "`는 이 규칙을 위반합니다.

현재 실습은 Validation Starter를 사용하므로 MVC가 검증기를 통해 이 규칙을 검사할 수 있습니다. 공백 질문은 검사에서 실패하여 Controller 본문의 `service.extract(question)`까지 도달하지 않습니다. 모델 호출·응답 변환·`criteria.validate()`도 실행되지 않습니다.

## 2. 400은 누가 지정할까?

이 실패에서는 서비스 코드가 HTTP 상태를 정하지 않습니다. **Spring MVC가 입력 검증 실패 예외를 만들 때 `BAD_REQUEST`를 지정**합니다.

| 구분 | 현재 실습의 처리 |
|---|---|
| 실패 발견 | MVC가 검증기를 통해 `@NotBlank` 제약 위반 확인 |
| 예외 생성과 상태 지정 | MVC가 `HandlerMethodValidationException`을 만들며 `HttpStatus.BAD_REQUEST` 지정 |
| 숫자와의 관계 | `HttpStatus.BAD_REQUEST`에 정의된 값이 400 |
| 응답 처리 | MVC 오류 처리가 예외에 저장된 상태를 HTTP 응답에 적용 |

`@NotBlank` 자체에 숫자 400이 들어 있는 것은 아닙니다. **입력 규칙은 애너테이션으로 선언하고, 위반 시 HTTP 400으로 응답하는 처리는 현재 MVC 기본 동작을 사용**합니다. 반면 502·422는 서비스에서 직접 `ResponseStatusException`을 만들어 지정했습니다.

## 3. HTTP 응답과 모델 요청 기록 확인하기

[StructuredOutputContractTest](../../../labs/04-structured-output/src/test/java/com/example/ailearning/l04structured/StructuredOutputContractTest.java)의 `공백_질문은_모델_호출_전에_거부한다()`에서 다음을 확인합니다. 실제 코드 발췌입니다.

```java
assertThat(postQuestion("   ").statusCode()).isEqualTo(400);
assertThat(MODEL.chat().getRequests()).isEmpty();
```

첫 줄은 HTTP 400 응답을, 둘째 줄은 모델 요청이 없었다는 점을 확인합니다. Controller 본문의 중단점에서 멈추지 않는 것도, 그 본문을 실행하기 전에 거부되었기 때문입니다.

## 선택: 정확한 상태 지정 코드와 디버깅

기본 실습에서는 위의 역할 구분과 테스트 결과를 이해하면 됩니다. 내부 소스를 따라가고 싶을 때만 아래 내용을 펼쳐 보세요. Spring Framework 7.0.10 기준입니다.

<details>
<summary>입력 검증 예외가 만들어지고 400이 저장되는 코드</summary>

## 어떤 예외가 발생하고 어디에서 400을 지정할까?

현재처럼 Controller의 `String` 매개변수에 직접 제약을 붙인 경우, Spring Framework **7.0.10**의 MVC 메서드 검증 경로는 다음과 같습니다.

| 단계 | 담당 코드 | 처리 |
|---|---|---|
| 1 | `InvocableHandlerMethod.invokeForRequest()` | 매개변수를 준비하고, 메서드 본문 호출 전에 인자 검증 실행 |
| 2 | `HandlerMethodValidator.applyArgumentValidation()` | 검증 결과에 처리되지 않은 오류가 있으면 `HandlerMethodValidationException` 생성·발생 |
| 3 | `HandlerMethodValidationException.initHttpStatus()` | **입력 인자 검증 실패이면 `HttpStatus.BAD_REQUEST` 선택** |
| 4 | `ResponseStatusExceptionResolver` | 예외의 상태 값 **400**을 읽어 HTTP 응답에 적용 |

### 1. 검증 실패를 발견하면 예외 객체를 만듭니다

`HandlerMethodValidator.applyArgumentValidation()`은 검증 결과에 오류가 있으면 다음 코드를 실행합니다. **Spring Framework 7.0.10 소스의 발췌이며, 실습에 추가할 코드는 아닙니다.**

```java
throw new HandlerMethodValidationException(
        result, this.modelAttributePredicate, this.requestParamPredicate);
```

`new`는 예외 객체를 만들고 생성자를 실행합니다. 생성자가 끝나면 `throw`가 그 객체를 던집니다. 여기서는 예외 객체를 만드는 동안 HTTP 상태도 정합니다.

### 2. 예외 생성자에서 BAD_REQUEST를 전달합니다

`HandlerMethodValidationException` 생성자의 첫 줄은 다음과 같습니다. 실제 소스의 발췌입니다.

```java
super(initHttpStatus(validationResult), "Validation failure", null, null, null);
```

`super(...)`는 **부모 클래스의 생성자를 호출하는 Java 문법**입니다. 이 클래스의 부모는 `ResponseStatusException`입니다. 첫 번째 인자에 전달할 값을 구하기 위해 먼저 `initHttpStatus(validationResult)`가 실행됩니다.

```java
private static HttpStatus initHttpStatus(MethodValidationResult validationResult) {
    return (validationResult.isForReturnValue()
            ? HttpStatus.INTERNAL_SERVER_ERROR
            : HttpStatus.BAD_REQUEST);
}
```

`isForReturnValue()`는 ‘메서드의 반환값을 검사하다 실패했는가?’를 뜻합니다. 이번에는 입력 매개변수 `question`의 검사이므로 `false`입니다. 따라서 `HttpStatus.BAD_REQUEST`를 반환합니다.

이 경우 생성자 호출을 이해하기 쉽게 풀어 쓰면 아래와 같습니다. **위 코드가 이번 입력에 대해 선택하는 값을 보여 주는 설명용 코드**입니다.

```java
super(HttpStatus.BAD_REQUEST, "Validation failure", null, null, null);
```

**바로 이 첫 번째 인자로 HTTP 상태를 전달합니다.** 부모인 `ResponseStatusException`의 생성자 처리를 거쳐 예외 객체에 상태가 저장됩니다. 아직 HTTP 응답을 보낸 것은 아닙니다.

### 3. BAD_REQUEST라는 이름에 숫자 400이 정의되어 있습니다

`HttpStatus`는 HTTP 상태에 이름을 붙여 둔 enum입니다. 실제 `BAD_REQUEST` 선언은 다음과 같습니다.

```java
BAD_REQUEST(400, Series.CLIENT_ERROR, "Bad Request")
```

따라서 `HttpStatus.BAD_REQUEST.value()`를 호출하면 **400**이 나옵니다. 개발자는 숫자 `400` 대신 의미를 읽기 쉬운 이름인 `BAD_REQUEST`를 코드에 사용합니다.

정리하면 **`initHttpStatus()`가 `BAD_REQUEST`를 선택하고 → 생성자가 그 상태를 예외 객체에 저장하고 → 나중에 MVC 예외 처리기가 그 안의 숫자 400을 읽습니다.** 502·422는 서비스 코드가 상태를 지정하지만, 이번 400은 Spring MVC의 예외 생성 코드가 지정합니다.

다른 형태의 Controller에서는 `MethodArgumentNotValidException` 등 다른 검증 예외가 발생할 수도 있습니다. 여기서는 현재 코드의 **매개변수에 직접 선언한 제약에 대한 메서드 검증 경로**를 설명합니다.

예외는 `DispatcherServlet`의 예외 처리 과정으로 전달됩니다. 현재 실습의 기본 구성에서는 `ResponseStatusExceptionResolver`가 `ResponseStatusException`의 하위 클래스인 이 예외를 처리합니다.

`resolveResponseStatusException()`이 `exception.getStatusCode().value()`로 400을 읽고, `applyStatusAndReason()`이 `response.sendError(400, reason)`에 해당하는 처리를 수행합니다. **제약 위반 발견 → MVC 예외 생성 시 400 지정 → 예외 처리기가 HTTP 응답에 400 적용**의 순서입니다.

별도의 `@ExceptionHandler`나 오류 처리 설정을 추가하면 응답 처리 담당이 달라질 수 있습니다. 여기서는 그러한 처리를 추가하지 않은 현재 실습의 경로를 기준으로 합니다.

</details>

<details>
<summary>IntelliJ에서 예외 발생·400 지정·HTTP 응답 적용 확인하기</summary>

## IntelliJ 디버거로 예외와 HTTP 400 확인하기

### Controller의 중단점에서 멈추지 않는 이유

`CriteriaController.extract()`의 `return service.extract(question);`에 중단점을 찍어도, 공백 질문으로는 멈추지 않습니다. Spring MVC가 **메서드 본문 실행 전에** `@NotBlank` 위반을 발견하고 예외를 던지기 때문입니다.

이 경로를 확인하려면 Controller 본문 대신 **예외가 던져지는 순간에 멈추는 예외 중단점(Exception Breakpoint)**을 사용합니다.

### 1. 예외가 발생하는 순간에 멈추기

1. IntelliJ의 **Run → View Breakpoints…**를 엽니다.
2. **+ → Java Exception Breakpoints**에서 다음 클래스를 추가합니다.

   ```text
   org.springframework.web.method.annotation.HandlerMethodValidationException
   ```

3. **Caught exception**을 체크합니다. 이 예외는 Spring MVC가 잡아서 HTTP 오류 응답으로 처리합니다. 따라서 잡히는 예외에서도 멈추도록 설정해야 합니다.
4. `StructuredOutputContractTest`의 `공백_질문은_모델_호출_전에_거부한다()`를 **Debug**로 실행합니다. 일반 **Run**으로 실행하면 디버거의 중단점에서 멈추지 않습니다.

멈추면 디버거의 **Frames / Call Stack**에서 `HandlerMethodValidator.applyArgumentValidation()`을 선택합니다. 이곳의 `throw new HandlerMethodValidationException(...)`이 입력 검증 실패를 예외로 전달하는 지점입니다.

이때는 예외의 생성자가 이미 실행되었으므로 예외 객체에 상태가 저장되어 있습니다. 사용 중인 IntelliJ 버전에 따라 메뉴나 예외 객체의 표시 위치는 다를 수 있습니다.

### 2. 예외에 400을 지정하는 순간 확인하기

예외 중단점은 예외를 던지는 순간에 멈춥니다. **생성자에서 상태를 선택하는 과정까지 보려면 아래 중단점을 추가하고 테스트를 다시 Debug로 실행합니다.**

IntelliJ의 클래스 검색으로 `HandlerMethodValidationException`을 열고, `initHttpStatus()`의 `return` 줄에 중단점을 설정합니다. 라이브러리 소스가 보이지 않으면 IDE의 소스 다운로드 기능을 사용합니다.

```java
return (validationResult.isForReturnValue()
        ? HttpStatus.INTERNAL_SERVER_ERROR
        : HttpStatus.BAD_REQUEST);
```

멈추면 **Evaluate Expression**에서 다음 값을 확인합니다.

| 확인할 식 | 예상 값 | 의미 |
|---|---|---|
| `validationResult.isForReturnValue()` | `false` | 반환값이 아닌 입력 매개변수의 검증 실패 |
| `org.springframework.http.HttpStatus.BAD_REQUEST.value()` | `400` | 선택할 상태 상수에 정의된 숫자 |

이 메서드는 `BAD_REQUEST`를 반환하고, 생성자의 `super(...)` 호출이 그 상태를 부모인 `ResponseStatusException`에 전달합니다. 여기까지는 **예외 객체에 상태를 저장하는 과정**이며 HTTP 응답 전송은 아직 일어나지 않았습니다.

### 3. 저장된 상태가 HTTP 응답에 적용되는 순간 확인하기

`ResponseStatusExceptionResolver.resolveResponseStatusException()`에도 중단점을 설정하고 실행을 계속합니다. 이 메서드의 예외 매개변수는 `ex`이며, Evaluate Expression에서 다음을 확인할 수 있습니다.

```java
ex.getStatusCode().value() // 400
```

이어서 `applyStatusAndReason()`에서 `response.sendError(...)`를 호출하는 줄에 중단점을 설정하면, 전달되는 `statusCode`가 `400`인지 확인할 수 있습니다. 이곳이 **예외에 저장된 숫자를 실제 HTTP 응답에 적용하는 지점**입니다.

마지막으로 실행을 계속하면 테스트의 `postQuestion("   ").statusCode()`가 400을 받아 검증을 통과합니다. 모델 요청 기록도 비어 있어야 합니다. 디버깅을 마치면 추가한 라이브러리 중단점과 예외 중단점을 해제합니다.

</details>

[이전: 값 검증 실패와 422](02-value-validation-422.md) · [04장 본문으로](../04-structured-output.md#5-실패는-테스트로-확인하기)

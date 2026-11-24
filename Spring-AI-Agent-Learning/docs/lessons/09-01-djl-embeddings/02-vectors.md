# 09 보충 1-2. DJL로 질문과 문서를 벡터로 바꾸기

**이번 목표:** E5 입력 규칙에 맞게 질문·문서를 전달하고, DJL이 반환한 벡터를 확인합니다.

[DJL 보충으로 돌아가기](../09-01-djl-embeddings.md) · [먼저 모델 준비](01-model-setup.md)

## 입력에서 벡터까지

| 개념 | 하는 일 | 이 예제의 값·동작 |
|---|---|---|
| 접두어 | 입력의 용도를 모델에 알려 줌 | 질문은 `query: `, 검색 문서는 `passage: ` |
| 토크나이저 | 문장을 모델이 읽는 토큰 번호로 바꿈 | `tokenizer.json` 사용 |
| 추론 | 모델의 가중치로 입력의 수치 표현 계산 | CPU에서 PyTorch 실행 |
| 평균 풀링 | 토큰별 표현을 한 문장의 벡터로 합침 | 패딩을 제외한 토큰 표현의 평균 |
| L2 정규화 | 벡터의 크기를 1로 맞춤 | 숫자 개수는 그대로 384개 |

정규화에서 말하는 **크기**는 벡터 값들의 제곱 합에 제곱근을 취한 값입니다.<br>
**차원 수 384**는 숫자의 개수이므로 정규화 후에도 변하지 않습니다.<br>
평균 풀링과 정규화는 DJL의 `TextEmbeddingTranslator`가 처리합니다.

## E5에 접두어를 붙이는 이유

[모델 제공자의 FAQ](https://huggingface.co/dragonkue/multilingual-e5-small-ko-v2#faq)는 접두어가 필요한 이유와 용도별 사용법을 설명합니다.<br>
접두어가 필요한 이유를 요약해 번역하면 다음과 같습니다.

> 이 모델은 접두어를 사용하는 형식으로 학습되었으므로, 접두어를 생략하면 성능이 저하될 수 있습니다.

| 사용 목적 | 접두어 사용법 |
|---|---|
| 질문으로 관련 문서 검색 | 질문에는 `query: `, 검색 대상 문서에는 `passage: ` |
| 두 문장의 의미 유사도 비교 | 비교할 두 문장 모두 `query: ` |

이 실습은 첫 번째 경우에 해당합니다.<br>
질문과 문서는 검색에서 맡는 역할이 다르므로 서로 다른 접두어를 붙입니다.

```text
질문 입력: query: 배송비는 얼마인가요?
문서 입력: passage: 기본 배송비는 3000원입니다.
```

접두어는 Spring AI나 DJL의 설정값이 아니라 **모델에 전달하는 입력 문자열의 일부**입니다.<br>
콜론 뒤의 공백을 포함해 붙이며, `E5InputFormatter`가 이 모델의 입력 규칙을 적용합니다.

## 이 실습에서 사용하는 DJL API

[단원 도입에서 설명한 DJL](../09-01-djl-embeddings.md#djl은-무엇일까)의 모델 로딩·추론 기능을 아래 API와 실제 코드로 연결합니다.

| API | 샘플에서 사용하는 부분 | 실제 역할 |
|---|---|---|
| `Criteria<String, float[]>` | 생성자의 [Criteria.builder() → build()](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java) | 문자열을 입력받아 벡터를 반환하도록 모델 경로·CPU 실행·변환 옵션 설정 |
| `ZooModel<String, float[]>` | [embeddingModel 필드](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java)와 [loadModel()](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java) | 로컬 파일에서 로드한 모델을 보관하고 여러 임베딩 요청에서 재사용 |
| `Predictor<String, float[]>` | predict()의 [embeddingModel.newPredictor()](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java) | 입력 문장을 실제로 추론해 벡터로 변환. 코드에서는 `var predictor`로 선언하며 호출 후 닫음 |
| `TextEmbeddingTranslatorFactory` | 생성자의 [optTranslatorFactory(...)](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java) | 토큰화와 모델 출력의 평균 풀링·정규화를 처리할 변환기 준비 |

먼저 [DjlModelConfiguration](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/DjlModelConfiguration.java)이 설정의 모델 경로를 읽어 서비스를 생성합니다.<br>
모델 로딩 전체 코드는 [Lesson02DjlEmbeddingService 생성자](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingService.java)에 있습니다.

아래는 그중 모델 처리 옵션을 정하는 부분입니다.

```java
.optTranslatorFactory(new TextEmbeddingTranslatorFactory())
.optArgument("pooling", "mean")
.optArgument("normalize", true)
.optArgument("padding", true)
.optArgument("truncation", true)
.optArgument("maxLength", 512)
```

### 실행 엔진은 어떤 역할일까?

모델 파일에는 학습된 가중치와 계산 구조가 들어 있고, **실행 엔진은 그 모델의 숫자 연산을 수행**합니다.<br>
현재 샘플은 다음 설정으로 Java 앱이 실행되는 PC의 CPU에서 PyTorch 엔진을 사용합니다.

```java
.optEngine("PyTorch")
.optDevice(Device.cpu())
```

`predictor.predict(...)`를 호출하면 DJL의 PyTorch 연동 기능이 네이티브 라이브러리를 호출해 모델 연산을 실행합니다.<br>
이 구성에는 별도의 Python 서버나 Ollama 서버가 필요하지 않습니다.<br>
앱을 다른 서버에 배포하면 그 서버의 CPU에서 계산합니다.

DJL은 다른 실행 엔진도 지원합니다.<br>
대표적인 엔진과 모델 형식은 다음과 같습니다.

| 엔진 | 사용하는 모델 형식 예 |
|---|---|
| PyTorch | 현재 샘플의 TorchScript `.pt` |
| ONNX Runtime | ONNX 형식의 `.onnx` |
| TensorFlow | TensorFlow SavedModel |

엔진을 변경하려면 이름 설정과 함께 **해당 엔진이 읽을 모델 파일, Gradle 의존성, 입력·출력 변환 설정**을 맞추고 결과를 확인해야 합니다.<br>
예를 들어 현재 모델을 ONNX Runtime으로 실행하려면 ONNX 형식으로 변환한 모델과 해당 엔진의 의존성을 준비해야 합니다.

참고: [DJL 공식 지원 엔진 안내](https://docs.djl.ai/master/docs/engine.html#supported-engines).

### optTranslatorFactory()가 준비하는 변환기

`optTranslatorFactory(new TextEmbeddingTranslatorFactory())`는 **DJL이 사용할 임베딩 변환기의 생성 방법을 지정**합니다.<br>
`Factory`는 객체를 만드는 역할을 뜻하며, 여기서는 실제 입력·출력 처리를 담당하는 `TextEmbeddingTranslator`를 만듭니다.

우리 코드는 `String`을 전달하지만, PyTorch 모델은 문자열을 그대로 계산하지 못합니다.<br>
변환기는 모델 폴더의 `tokenizer.json`을 사용하는 토크나이저로 문장을 토큰 번호로 바꿉니다.<br>
그 번호를 입력받은 모델이 토큰별 벡터를 계산하면, 변환기는 그 결과를 문장 하나의 임베딩 벡터로 정리합니다.

```text
입력 문자열: "query: 배송비는 얼마인가요?"
 → 변환기: 토크나이저로 모델 입력 생성
 → PyTorch 모델: 토큰별 벡터 계산
 → 변환기: 평균 풀링으로 문장 벡터 생성
 → 변환기: 정규화로 벡터의 크기를 1로 맞춤
 → 반환값: float[] — 이 모델에서는 384개 숫자
```

위 코드의 `pooling=mean`은 토큰별 벡터를 평균으로 합치는 방법을, `normalize=true`는 결과를 L2 정규화하도록 지정합니다.<br>
팩토리는 이 옵션을 반영한 변환기를 준비하고, 이후 `predictor.predict(formattedText)`를 실행할 때 변환기와 모델이 함께 위 과정을 수행합니다.

`padding`은 묶어 처리할 입력의 길이를 맞추는 설정이고, `truncation`은 최대 토큰 수를 넘는 부분을 자르는 설정입니다.<br>
긴 원문을 보존해 검색하려면 10단원에서 배운 문서 분할을 먼저 적용해야 합니다.

## 질문·문서 메서드 읽기

아래는 서비스의 임베딩 실행 부분을 발췌한 코드입니다.<br>
[E5InputFormatter](../../../labs/09-01-djl-embeddings/src/main/java/com/example/ailearning/l09djl/lesson02_vectors/E5InputFormatter.java)는 접두어와 빈 입력 검사를 담당합니다.

```java
public float[] embedQuery(String text) {
    return predict(E5InputFormatter.formatQuery(text));
}

public float[] embedDocument(String text) {
    return predict(E5InputFormatter.formatDocument(text));
}

private float[] predict(String formattedText) {
    try (var predictor = embeddingModel.newPredictor()) {
        return predictor.predict(formattedText);
    } catch (TranslateException error) {
        throw new IllegalStateException("로컬 E5 임베딩 생성에 실패했습니다.", error);
    }
}
```

예를 들어 `embedQuery("배송비는 얼마인가요?")`는 `query: 배송비는 얼마인가요?`를 모델에 전달합니다.<br>
문서에는 `passage: `를 붙입니다.<br>
이 규칙은 [모델 제공자의 사용법](https://huggingface.co/dragonkue/multilingual-e5-small-ko-v2#direct-usage-transformers)을 따릅니다.

모델은 앱에서 재사용하지만, 스레드 간 공유가 안전하지 않은 `Predictor`는 호출마다 생성하고 닫습니다.<br>
`try (...)`는 Java의 try-with-resources 문법으로, 정상 반환이나 예외 발생 후 모두 해당 자원을 닫습니다.<br>
모델 자체는 앱 종료 때 `DjlModelConfiguration`의 `destroyMethod = "close"`로 닫습니다.

## 실행과 결과

벡터만 생성하는 프로필로 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-01-djl-embeddings:bootRun --args='--spring.profiles.active=embedding-only'
```

다른 터미널에서 요청합니다.

```bash
curl -sS http://127.0.0.1:8116/api/embedding \
  -H 'Content-Type: text/plain; charset=UTF-8' \
  --data-raw '배송비는 얼마인가요?'
```

응답의 `dimensions`는 384이며 `values`에는 실수 384개가 들어갑니다.<br>
응답에서 확인할 값을 요약하면 다음과 같습니다.<br>
배열은 실제 결과의 앞 8개 값만 표시했습니다.

```text
dimensions: 384
values의 앞 8개: [-0.018699, -0.065587, -0.084919, -0.088638,
                 -0.022599, -0.048020, -0.075764, 0.099193]
```

[실제 모델 테스트](../../../labs/09-01-djl-embeddings/src/test/java/com/example/ailearning/l09djl/lesson02_vectors/Lesson02DjlEmbeddingLiveTest.java)는 차원 수·유한한 값·벡터 크기를 검사하고, 앞 8개 값을 출력합니다.<br>
[입력 단위 테스트](../../../labs/09-01-djl-embeddings/src/test/java/com/example/ailearning/l09djl/lesson02_vectors/E5InputFormatterUnitTest.java)는 모델 없이 접두어와 빈 입력 처리를 검사합니다.

## 확인 질문

정규화 후 벡터의 크기가 1이라면 숫자가 한 개만 남는다는 뜻일까요?

<details>
<summary>해설</summary>

아닙니다.<br>
숫자는 여전히 384개이며, 그 값들이 이루는 벡터의 크기를 1로 맞췄다는 뜻입니다.

</details>

[다음: Spring AI에 연결](03-spring-ai.md)

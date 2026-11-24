# 16-2. DJL로 질문과 문서를 벡터로 바꾸기

**이번 목표:** E5 입력 규칙에 맞게 질문·문서를 전달하고, DJL이 반환한 벡터를 확인합니다.

[16단원으로 돌아가기](../16-djl-embeddings.md) · [먼저 모델 준비](01-model-setup.md)

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

## DJL이 제공하는 기능

| API | 역할 |
|---|---|
| `Criteria<String, float[]>` | 입력·출력 타입, 모델 경로, 엔진, 변환 방법 지정 |
| `ZooModel<String, float[]>` | 로드한 모델과 추론에 필요한 자원 보관 |
| `Predictor<String, float[]>` | 입력 문장으로 실제 추론 실행 |
| `TextEmbeddingTranslatorFactory` | 토크나이저와 임베딩 결과 변환 기능 준비 |

먼저 [DjlModelConfiguration](../../../labs/16-00-djl-embeddings/src/main/java/com/example/ailearning/l16djl/DjlModelConfiguration.java)이 설정의 모델 경로를 읽어 서비스를 생성합니다.<br>
모델 로딩 전체 코드는 [Lesson02DjlEmbeddingService 생성자](../../../labs/16-00-djl-embeddings/src/main/java/com/example/ailearning/l16djl/lesson02_vectors/Lesson02DjlEmbeddingService.java)에 있습니다.

아래는 그중 모델 처리 옵션을 정하는 부분입니다.

```java
.optTranslatorFactory(new TextEmbeddingTranslatorFactory())
.optArgument("pooling", "mean")
.optArgument("normalize", true)
.optArgument("padding", true)
.optArgument("truncation", true)
.optArgument("maxLength", 512)
```

`padding`은 묶어 처리할 입력의 길이를 맞추는 설정이고, `truncation`은 최대 토큰 수를 넘는 부분을 자르는 설정입니다.<br>
긴 원문을 보존해 검색하려면 10단원에서 배운 문서 분할을 먼저 적용해야 합니다.

## 질문·문서 메서드 읽기

아래는 서비스의 임베딩 실행 부분을 발췌한 코드입니다.<br>
[E5InputFormatter](../../../labs/16-00-djl-embeddings/src/main/java/com/example/ailearning/l16djl/lesson02_vectors/E5InputFormatter.java)는 접두어와 빈 입력 검사를 담당합니다.

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
./gradlew :Spring-AI-Agent-Learning:labs:16-00-djl-embeddings:bootRun --args='--spring.profiles.active=embedding-only'
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

[실제 모델 테스트](../../../labs/16-00-djl-embeddings/src/test/java/com/example/ailearning/l16djl/lesson02_vectors/Lesson02DjlEmbeddingLiveTest.java)는 차원 수·유한한 값·벡터 크기를 검사하고, 앞 8개 값을 출력합니다.<br>
[입력 단위 테스트](../../../labs/16-00-djl-embeddings/src/test/java/com/example/ailearning/l16djl/lesson02_vectors/E5InputFormatterUnitTest.java)는 모델 없이 접두어와 빈 입력 처리를 검사합니다.

## 확인 질문

정규화 후 벡터의 크기가 1이라면 숫자가 한 개만 남는다는 뜻일까요?

<details>
<summary>해설</summary>

아닙니다.<br>
숫자는 여전히 384개이며, 그 값들이 이루는 벡터의 크기를 1로 맞췄다는 뜻입니다.

</details>

[다음: Spring AI에 연결](03-spring-ai.md)

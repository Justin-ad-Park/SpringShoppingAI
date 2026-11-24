# 09-1. 텍스트를 숫자 배열로 표현하기

**실행 코드·설정·테스트:** [labs/09-00-embeddings-search](../../../labs/09-00-embeddings-search/README.md).<br>
저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 텍스트가 임베딩 모델을 통해 숫자 배열로 변환되는 과정을 이해합니다.

**코드 읽기 안내:** 벡터 각 숫자에 사람이 단어 뜻을 이름 붙이지 않습니다.<br>
입력 텍스트가 검색 가능한 숫자로 변환된다는 관계만 먼저 이해합니다.

## 왜 문자열 검색만으로는 부족할까?

문서에 “배송비”가 있는데 사용자가 “운송 요금”이라고 묻는 경우를 생각해 보세요.<br>
`text.contains(query)`는 입력 문자열 자체가 문서에 있어야 성공합니다.<br>
임베딩 모델은 텍스트를 숫자 배열인 **벡터**로 바꿉니다.<br>
비슷한 의미가 가까운 방향에 놓이도록 학습한 모델을 사용하면 다른 표현도 검색할 수 있습니다.<br>
실제 의미 검색 품질은 모델·문서·질문에 따라 달라지며 항상 성공하는 것은 아닙니다.

## 새 용어

| 용어 | 뜻 |
|---|---|
| 임베딩(embedding) | 텍스트를 숫자 배열로 표현하는 변환 |
| 벡터(vector) | 텍스트를 표현하는 숫자 배열 |
| 차원(dimensions) | 배열의 숫자 개수. “의미 항목의 개수”라고 고정 해석하지 않음 |

예를 들어 설명용 벡터 `[1,0]`과 `[0.9,0.1]`은 방향이 가깝고 `[0,1]`은 상대적으로 다릅니다.<br>
이 숫자는 실제 모델 출력이 아닌 원리 설명용입니다.<br>
실제 각 차원의 의미를 사람이 이름 붙여 해석하는 방식은 아닙니다.
## 텍스트 하나를 변환하기

[Lesson01EmbeddingService](../../../labs/09-00-embeddings-search/src/main/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingService.java)의 전체 코드입니다.<br>
`EmbeddingModel`은 Starter가 준비한 객체를 주입받습니다.

```java
package com.example.ailearning.l09search.lesson01_vectors;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/** 09-1: 텍스트 하나의 임베딩만 요청한다. 문서를 적재하지 않는다. */
@Service
public class Lesson01EmbeddingService {
    public record EmbeddingVector(int dimensions, float[] values) {}
    private final EmbeddingModel embeddingModel;

    public Lesson01EmbeddingService(EmbeddingModel embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public EmbeddingVector embed(String text) {
        float[] vectorValues = embeddingModel.embed(text);
        return new EmbeddingVector(vectorValues.length, vectorValues);
    }
}
```

## 실행과 확인

같은 모듈에서 `embedding-only` 프로필을 선택하면 검색 Service·Controller를 제외합니다.<br>
따라서 문서를 적재하지 않고 입력 텍스트 하나만 임베딩합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests '*Lesson01EmbeddingOnlyLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:bootRun --args='--spring.profiles.active=embedding-only'
curl http://127.0.0.1:8099/api/embedding -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '배송비'
```

[테스트](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingOnlyLocalModelTest.java)는 시작 시 임베딩 요청이 없고 입력 후 요청이 한 번인지, 벡터 차원이 반환되는지 확인합니다.<br>
로컬 테스트는 외부 API를 호출하지 않습니다.<br>
`bootRun`은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_EMBEDDING_MODEL`로 실제 모델을 사용합니다.<br>
종료는 Ctrl+C입니다.

실제 모델 연결은 [Lesson01EmbeddingOnlyOpenAiLiveTest](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/lesson01_vectors/Lesson01EmbeddingOnlyOpenAiLiveTest.java)를 별도로 실행해 확인합니다.<br>
루트 `.env`가 필요하며 API 사용량이 발생합니다.

## 확인 질문

`embed(text)`의 입력과 출력은 무엇이며, 이 실습을 시작할 때 문서 임베딩도 발생하나요?

<details><summary>해설</summary>
입력은 텍스트, 출력은 float 배열이며 길이가 차원입니다.<br>
embedding-only에서는 문서 적재를 하지 않아 실행한 텍스트의 임베딩만 요청합니다.
</details>

[목차](../09-embeddings-search.md) · [다음: 기본 벡터 검색](02-store.md)

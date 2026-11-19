# 09-2. 문서를 넣고 query와 topK로 검색하기

**목표:** 제공되는 `SimpleVectorStore`로 문서를 적재하고 관련 문서를 검색합니다. 필터와 문자열 검색 비교는 다음 수업에서 추가합니다.

## 검색에 필요한 용어

“운송 요금”이라는 질문으로 “기본 배송비는 3000원”이라는 문서를 찾는 상황을 생각해 봅시다. 벡터 검색은 질문과 문서의 숫자 벡터를 비교해 관련 문서를 찾습니다.

| 용어 | 뜻 | 이번 실습에서의 예 |
|---|---|---|
| 벡터 저장소(VectorStore) | 문서와 임베딩 벡터를 보관하고, 질문 벡터와 비교해 관련 문서를 찾는 구성 요소 | 배송·반품 정책을 적재한 저장소 |
| 검색어(query) | 찾고 싶은 내용을 표현한 질문이나 문구 | `운송 요금` |
| 유사도 점수(score) | 질문 벡터와 문서 벡터가 얼마나 가까운지 나타내는 값 | 검색 결과를 관련성 순서로 비교할 때 사용. 문서 내용이 사실일 확률은 아님 |
| `topK` | 반환할 문서 수의 상한 | `topK=1`이면 최대 한 문서. `topK=5`라도 저장한 문서가 세 개면 다섯 개를 채울 수 없음 |
| 메타데이터(metadata) | 본문과 함께 저장하는 분류·출처 등의 부가 정보 | `category=shipping` |

이 실습의 `SimpleVectorStore`는 코사인 유사도로 벡터를 비교합니다. 코사인 유사도는 두 벡터의 방향이 얼마나 비슷한지 비교하는 방법입니다. 점수가 높다는 것만으로 질문에 답할 자료라고 확정하지 말고 원문도 읽어야 합니다. 유사도 하한과 메타데이터 필터는 [09-3](03-search.md)에서 추가합니다.

## 용어를 Spring AI 코드에 연결하기

| 타입·메서드 | 역할 |
|---|---|
| `Document` | 본문·ID·메타데이터를 담음 |
| `SimpleVectorStore` | 메모리에서 문서 벡터를 비교하는 구현체 |
| `add(documents)` | 연결된 임베딩 모델로 문서 벡터를 만들어 저장 |
| `similaritySearch(request)` | 질문도 임베딩해 관련 문서 반환 |
| `SearchRequest.query/topK` | 질문과 최대 결과 수 지정 |

[PolicyDocuments](../../../labs/09-00-embeddings-search/src/main/java/com/example/ailearning/l09search/PolicyDocuments.java)는 배송 정책(`shipping`), 미개봉 반품(`return-sealed`), 개봉 반품(`return-opened`) 문서를 만듭니다. 각 문서는 `Document`의 ID·본문·메타데이터로 구성합니다. 문서가 짧아 이 단계에서는 분할하지 않습니다.

문서와 질문은 **같은 임베딩 모델과 같은 차원 설정**으로 변환해야 합니다. 차원은 벡터 배열의 길이입니다. 배열 길이가 같아도 다른 모델이 만든 벡터는 숫자가 표현하는 의미가 다를 수 있어 섞어 비교하면 안 됩니다. 모델이나 차원 설정을 바꾸면 기존 문서도 새 설정으로 다시 임베딩해 적재합니다.

## 문서 적재와 검색은 언제 일어날까?

1. 서버 시작 시 `BasicVectorSearchService` 생성자가 저장소를 만들고 `add(...)`로 정책 문서 세 개를 전달합니다. 저장소는 연결된 `EmbeddingModel`로 문서를 임베딩해 보관합니다.
2. 검색 요청이 오면 `search()`가 질문과 `topK`를 `similaritySearch(...)`에 전달합니다. 저장소는 같은 `EmbeddingModel`로 질문을 임베딩합니다.
3. 저장소가 질문 벡터와 보관 중인 문서 벡터를 비교하고, 유사도 순서로 최대 `topK`개의 문서를 반환합니다.

개발자가 문서·질문의 숫자 배열을 직접 비교할 필요는 없습니다. `SimpleVectorStore`가 임베딩 모델 호출과 비교를 연결합니다. 반환값은 검색한 문서이며, 그 문서로 답변을 작성하는 RAG는 [10장](../10-rag.md)에서 배웁니다.

## 전체 서비스

[BasicVectorSearchService](../../../labs/09-00-embeddings-search/src/main/java/com/example/ailearning/l09search/BasicVectorSearchService.java)의 전체 코드입니다.

```java
package com.example.ailearning.l09search;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/** 09-2: 문서 적재와 query·topK 검색만 연결한다. */
@Service
@Profile("basic-search")
public class BasicVectorSearchService {
    private final SimpleVectorStore vectorStore;

    public BasicVectorSearchService(EmbeddingModel embeddingModel) {
        vectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        vectorStore.add(PolicyDocuments.createDocuments());
    }

    public List<Document> search(String question, int topK) {
        return vectorStore
                .similaritySearch(SearchRequest.builder()
                        .query(question)
                        .topK(topK)
                        .build());
    }
}
```

`basic-search` 프로필은 이 기본 검색을 선택하고 다음 수업의 비교 검색은 제외합니다. 검색 결과는 원문과 점수를 가진 Document이며, 모델이 작성한 답변은 아닙니다.

## 실행과 테스트

아래 명령은 저장소 루트에서 실행합니다. 로컬 테스트에는 실제 키가 필요 없습니다. `bootRun`은 루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_EMBEDDING_MODEL`을 사용합니다. 설정 방법은 [실행 안내](../../run-guide.md)를 참고하세요. 서버를 실행한 뒤 `curl`은 다른 터미널에서 실행합니다.


```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests '*BasicVectorSearchLocalModelTest'
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:bootRun --args='--spring.profiles.active=basic-search'
curl --get http://127.0.0.1:8099/api/search/basic --data-urlencode 'query=운송 요금' --data-urlencode 'topK=1'
```

[테스트](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/BasicVectorSearchLocalModelTest.java)는 고정 벡터로 문서를 적재하고 검색 ID와 개수를 확인합니다. 외부 API를 호출하지 않습니다. 실제 실행은 루트 `.env`를 사용하며 시작 시 문서, 검색 시 질문의 임베딩 API를 호출합니다. 종료는 Ctrl+C입니다.

이 저장소는 JVM 프로세스 메모리에 문서와 벡터를 보관합니다. 서버를 재시작하면 새 저장소를 만들고 문서를 다시 임베딩하므로 API 사용량이 다시 발생합니다. 07장의 대화 메모리는 사용자와 모델의 대화를 보관하고, 여기서는 검색할 정책 문서를 보관한다는 차이가 있습니다.

## 결과에서 무엇을 읽을까?

고정 벡터를 사용하는 위 로컬 테스트는 `배송`을 검색해 문서 한 개와 ID `shipping`을 확인합니다. 실제 모델로 `운송 요금`을 검색할 때는 같은 결과를 보장하지 않습니다. 반환한 `Document`에서 다음 값을 확인하세요.

| 확인할 값 | 읽는 방법 | 해석 |
|---|---|---|
| ID | `document.getId()` | 어떤 정책이 검색됐는지 확인. 배송 정책은 `shipping` |
| 원문 | `document.getText()` | 기본 배송비 3000원과 상품 금액 30000원 이상 무료배송 조건을 실제로 담고 있는지 확인 |
| 점수 | `document.getScore()` | 질문과 문서의 유사도. 가격이나 정답 확률이 아님 |
| 메타데이터 | `document.getMetadata()` | 문서에 지정한 `category` 확인 |

`topK`를 1에서 2로 바꾸어 같은 질문으로 검색하고 반환 개수·ID·원문을 비교해 보세요. 확인 후 1로 되돌립니다. 검색 결과 수가 늘었다고 필요한 자료가 더 잘 검색된 것은 아니므로 추가된 원문의 관련성도 판단합니다.

실제 모델 연결은 [BasicVectorSearchOpenAiLiveTest](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/BasicVectorSearchOpenAiLiveTest.java)를 별도로 실행해 확인합니다. 루트 `.env`가 필요하며 API 사용량이 발생합니다.

## 확인 질문

1. 문서 적재와 질문 검색에서 각각 어떤 텍스트를 임베딩하며, 두 과정이 모두 필요한 이유는 무엇인가요?
2. `topK=5`인데 문서 세 개만 적재했다면 다섯 개가 반환되나요?
3. 벡터 차원이 같으면 다른 임베딩 모델로 바꿔도 기존 문서 벡터를 그대로 사용할 수 있나요?

<details>
<summary>해설</summary>

1. `add(...)`는 문서 본문을, `similaritySearch(...)`는 검색 질문을 임베딩합니다. 같은 모델의 벡터로 표현해야 문서와 질문을 비교할 수 있기 때문입니다.
2. 아닙니다. `topK`는 최대 반환 수이며 저장한 문서보다 많은 결과를 만들지 않습니다. 검색 조건에 따라 그보다 적게 반환될 수도 있습니다.
3. 그대로 사용하면 안 됩니다. 차원이 같아도 모델이 만드는 벡터 공간이 다를 수 있습니다. 새 모델로 문서를 다시 임베딩해 적재해야 합니다. 이 실습은 재시작하면 문서를 다시 적재합니다.

</details>

[이전](01-vectors.md) · [목차](../09-embeddings-search.md) · [다음: 비교와 필터](03-search.md)

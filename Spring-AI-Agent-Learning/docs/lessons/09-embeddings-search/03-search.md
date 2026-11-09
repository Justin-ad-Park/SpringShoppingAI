# 09-3. 검색 결과와 필터 실험

**실행 코드·설정·테스트:** [labs/09-00-embeddings-search](../../../labs/09-00-embeddings-search/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 질문·결과 개수·유사도 조건을 바꾸며 검색 결과를 비교합니다.

**코드 읽기 안내:** 가짜 벡터 테스트는 검색 코드의 연결을 검사합니다. 실제 의미 검색의 품질은 실제 임베딩 모델로 따로 관찰합니다.

## 설정과 실행

이 실습에는 채팅 모델 이름이 필요 없습니다. 저장소 루트 `.env`에 `OPENAI_API_KEY`와 `OPENAI_EMBEDDING_MODEL`을 준비합니다.
키를 셸 환경 변수로 따로 입력할 필요는 없습니다. 작성 방법은 [실행 설정 안내](../../run-guide.md)를 참고하세요.
명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:test --tests com.example.ailearning.l09search.SearchLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:09-00-embeddings-search:bootRun
curl -sS http://127.0.0.1:8099/api/embedding \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '배송비'
curl --get http://127.0.0.1:8099/api/search \
  --data-urlencode 'query=운송 요금' --data-urlencode 'topK=2' --data-urlencode 'threshold=0.5'
```

bootRun은 **시작 시 문서 임베딩부터 실제 API를 호출**합니다. 검색 요청마다 질문 임베딩 호출이 추가됩니다.
따라서 서버 재시작에도 사용량이 발생할 수 있습니다. 로컬 테스트는 실제 키 없이 고정 응답 임베딩 서버를 사용합니다.
실제 결과 점수·배열 길이는 선택한 모델에 따라 다릅니다. 종료는 Ctrl+C입니다.

## 바꾸면서 확인하기

| 바꿀 값 | 관찰할 점 |
|---|---|
| query=배송비 / 운송 요금 | 문자열 결과와 벡터 결과의 차이. 모델이 항상 동의어를 찾는다는 보장은 없음 |
| topK=1 / 3 | 최대 반환 수 차이. threshold로 걸러지면 그보다 적거나 0개 |
| threshold=0.2 / 0.9 | 엄격해질수록 후보가 줄어드는지 확인 |
| category=returns | 배송 문서는 의미가 가까워도 필터에서 제외 |
| query=문서와 무관한 질문 | threshold가 낮으면 무관한 문서도 반환될 수 있음 |

카테고리가 잘못되거나 topK/threshold가 범위를 벗어나면 400입니다.
threshold 숫자는 실제 질문 세트로 조정해야 하며, 이 예제의 0.5는 서비스 품질을 보장하는 값이 아닙니다.

[SearchLocalModelTest](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/SearchLocalModelTest.java)는 실제 어댑터와 저장소를 사용합니다.
테스트 서버는 정해진 단어군에 3차원 직교 벡터를 반환해 적재·검색·필터를 재현합니다.
임베딩 API의 정상 벡터 반환과 검색 API의 잘못된 조건 거부는 서로 다른 테스트로 확인합니다. 잘못된 topK·threshold·category는 매개변수 테스트로 각각 실행하므로 어떤 조건에서 실패했는지 구분할 수 있습니다.
이는 실제 모델의 의미 검색 성능을 평가하는 테스트가 아닙니다. 실제 모델 품질은 별도 질문으로 확인해야 합니다.

## 확인 질문

1. 검색할 때 문서뿐 아니라 질문도 임베딩하는 이유는 무엇인가요?
2. topK=3이면 항상 3개가 나오나요?
3. score=0.9는 문서 내용이 90% 확률로 사실이라는 뜻인가요?
4. 같은 차원의 다른 모델로 바꿔도 기존 벡터를 계속 써도 될까요?

<details>
<summary>해설</summary>

1. 문서와 질문을 같은 벡터 공간에서 비교하기 위해서입니다.
2. 아닙니다. `topK=3`은 반환 개수의 상한입니다. 유사도 임계값을 만족하는 문서가 없거나 메타데이터 필터로 모두 제외되면 0개일 수 있습니다.
3. 아닙니다. score는 검색 유사도이며 문서 내용이 사실일 확률이 아닙니다.
4. 그대로 사용하면 안 됩니다. 차원이 같아도 모델마다 벡터 공간이 달라질 수 있으므로 같은 모델로 문서를 다시 임베딩해야 합니다.

</details>

검색 결과의 원문·ID·점수를 확인하면 완료입니다.

보충: [공식 VectorStore API](https://docs.spring.io/spring-ai/reference/api/vectordbs.html).

[이전](02-store.md) · [다음](../09-embeddings-search.md) · [단원 목차](../09-embeddings-search.md) · [학습 방법](../../learning-guide.md)

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

## 실제 검색 결과를 해석하기

[SearchOpenAiLiveTest](../../../labs/09-00-embeddings-search/src/test/java/com/example/ailearning/l09search/SearchOpenAiLiveTest.java)는 벡터가 생성되고 검색 결과가 반환되는지 검사합니다. 테스트에서 threshold를 0.0으로 두므로 결과가 있다는 사실만으로 관련 문서를 잘 찾았다고 판단할 수 없습니다.

출력의 ID·원문·점수를 아래 기준으로 비교합니다. 실제 모델은 다른 순위를 낼 수 있으며, 표는 자동 통과 조건이 아니라 사람이 관련성을 판단할 기준입니다.

| 질문 | 관련성 판단 기준 | 기대와 다를 때 확인할 것 |
|---|---|---|
| 배송비는 얼마인가요? / 운송 요금 | `shipping`의 기본 배송비 3000원·상품 금액 30000원 이상 무료배송 원문이 있는가 | 해당 문서가 후보에 있는지, 몇 번째인지, 필터로 제외되지 않았는지 |
| 미개봉 반품 기간 | `return-sealed`의 수령 후 7일 이내 반품 원문이 있는가 | `return-opened`만 반환됐다면 개봉 여부를 구분했는지 |
| 우주 여행 비용 | 제공한 정책에는 답할 근거가 없음 | 결과가 반환돼도 원문이 질문과 관련 있는지 확인. 낮은 threshold에서는 빈 결과가 보장되지 않음 |

질문·모델 이름·topK·threshold·반환 ID와 순위를 함께 적으면 조건 변경 전후를 비교할 수 있습니다. 점수는 정답 확률이 아니므로, 숫자만 높아졌다는 이유로 개선이라고 판단하지 마세요.

## 실무에 연결하기: 검색 결과를 계속 비교하기

의미 검색은 질문 표현에 따라 결과가 달라집니다. 예를 들어 “택배비”, “배송료”, “얼마 이상이면 무료배송?”을 같은 정책의 검색 사례로 묶어 비교할 수 있습니다. 이는 평가 질문의 예이며 현재 고정 벡터 테스트가 이 표현을 모두 이해한다는 뜻은 아닙니다.

| 바꾼 것 | 함께 확인할 것 |
|---|---|
| 임베딩 모델 | 문서 벡터 재생성, 동일 질문의 검색 ID·순위·비용 |
| `topK` | 관련 문서 누락과 불필요한 결과 증가 |
| 유사도 하한 | 원하는 문서 제외와 무관한 문서 유입 |
| 필터 | 의미가 비슷해도 다른 카테고리 문서가 섞이지 않는지 |
| 문서 갱신 | 폐기한 정책이 남지 않고 최신 원문이 검색되는지 |

한 설정을 바꾸고 같은 질문 세트의 결과를 비교한 뒤 원복합니다. 실제 모델 비교에서는 고정 벡터 테스트가 아닌 Live 또는 수동 검색을 사용하며 API 사용량이 발생할 수 있습니다. 저장소가 반환한 score를 모든 모델·저장소에서 통용되는 정답 확률로 해석하지 않습니다.

검색한 정책을 답변에 사용하는 단계는 [10장](../10-rag.md)입니다. 실제 문서의 갱신·접근 범위와 실패 위치의 구분은 [10-6](../10-rag/06-production-rag.md)에서 이어서 읽습니다. 참고: [Spring AI Vector Databases](https://docs.spring.io/spring-ai/reference/api/vectordbs.html).

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

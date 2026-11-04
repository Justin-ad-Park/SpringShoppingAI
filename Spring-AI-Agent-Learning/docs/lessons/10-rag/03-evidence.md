# 10-3. 출처를 확인하고 답변을 보류하기

**실행 코드·설정·테스트:** [labs/10-rag](../../../labs/10-rag/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

30~45분을 기준으로 한 번에 한 가지 흐름만 읽습니다. 읽기와 실습을 합친 시간이며, 단원 전체 예상 시간에 포함됩니다.

**이번 목표:** 출처 ID 확인과 내용의 진실성 확인이 다름을 설명합니다.

**코드 읽기 안내:** 상태 이름을 외우기보다 어떤 조건에서 생성 호출을 하지 않고, 어떤 조건에서 답변을 보류하는지 설명하세요.

## 결과 상태를 구분하기

| 상태 | 의미 |
|---|---|
| NO_CONTEXT | 검색 결과가 없음. 채팅 생성 없이 Java가 안내 |
| NO_ANSWER | 문서는 찾았지만 모델이 답을 뒷받침하지 못한다고 판단 |
| INVALID_CITATION | 누락되었거나 제공한 목록에 없는 출처 ID. 모델 답변을 채택하지 않음 |
| ANSWERED | 답변 가능으로 응답했고 출처 ID 검사 통과 |

ANSWERED는 **모든 문장이 근거로 증명되었다는 뜻은 아닙니다.** 모델이 유효한 ID를 붙여 틀린 주장을 할 수도 있습니다.
원문과 답변을 비교해야 합니다. 채팅 모델 호출·응답 변환 실패는 서비스가 HTTP 502로 바꾸며, 빈 질문은 Controller가 400으로 거부합니다.
질문 임베딩·검색은 그 예외 처리 범위 밖에 있습니다. 해당 단계의 외부 호출 실패는 이 예제에서 502로 변환하지 않아 일반 500으로 반환될 수 있습니다.
검색 threshold 0.65와 topK=2는 가상 데이터용 출발값이며 실제 품질 보장 값이 아닙니다.

## 실행

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL`을 사용합니다. 설정은 [01장](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)과 [09장](../09-embeddings-search.md)을 참고합니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:test
./gradlew :Spring-AI-Agent-Learning:labs:10-rag:bootRun
curl --get http://127.0.0.1:8100/api/rag/sources \
  --data-urlencode 'question=미개봉 상품 반품 기간은?'
curl -sS http://127.0.0.1:8100/api/rag \
  -H 'Content-Type: text/plain; charset=UTF-8' --data-raw '미개봉 상품 반품 기간은?'
```

첫 API는 검색 원문만, 두 번째는 근거를 넣은 답변을 반환합니다. 응답 형태 예시는 다음과 같습니다.

```json
{"status":"ANSWERED","answer":"미개봉 상품은 수령 후 7일 이내 반품할 수 있습니다.","sources":[{"id":"return-sealed","location":"policies.md#return-sealed"}]}
```

실제 응답에는 원문과 점수도 포함되며 자연어 문장은 달라질 수 있습니다.
서버 시작 시 문서 임베딩, 검색 시 질문 임베딩, 답변 시 채팅 API 사용량이 발생할 수 있습니다. 종료는 Ctrl+C입니다.

## 실험

1. “미개봉 반품”과 “개봉 반품”의 검색 결과·출처·답변을 비교합니다.
2. “우주 여행 비용”처럼 무관한 질문을 넣고 검색 점수와 보류 동작을 관찰합니다. 실제 모델에서 항상 검색 0개가 된다고 가정하지 마세요.
3. `policies.md`의 7일을 10일로 바꾼 뒤 **재시작해 재적재**합니다. 답변이 근거 변경을 반영하는지 확인하고 원복합니다.
4. 청크를 너무 잘게 나누면 “7일”만 남고 무엇의 기간인지 사라질 수 있습니다. 문맥 보존과 검색 정밀도의 균형을 생각합니다.

[RagContractTest](../../../labs/10-rag/src/test/java/com/example/ailearning/l10rag/RagContractTest.java)는 실제 임베딩/채팅 어댑터를 로컬 고정 서버로 연결해 6개 경로를 검증합니다.
정상 근거 전달, 생성 생략, 잘못된 출처, 답변 보류, 형식 오류, 입력 검증을 재현하며 실제 답변 품질 평가는 아닙니다.
다음 단원에서 고정 질문 세트와 실행 기록을 이용해 개선 여부를 비교합니다.

## 확인 질문

1. 문서의 내용을 바꿨는데 저장소를 재적재하지 않으면 무엇이 검색될까요?
2. 출처 ID가 유효하면 답변이 반드시 사실일까요?
3. NO_CONTEXT와 NO_ANSWER는 어떻게 다른가요?

<details><summary>해설</summary>

기존 적재 문서/벡터가 검색됩니다. ID 검증만으로 주장과 근거의 일치까지 증명하지 못합니다.
NO_CONTEXT는 검색 단계에서 근거 없음, NO_ANSWER는 찾은 문서로 답할 수 없다는 모델 판단입니다.
검색 원문이 실제 프롬프트에 들어간 것을 확인하고 상태별 차이를 설명하면 완료입니다.
</details>

보충: [공식 RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html).
08에서 배운 Advisor로 이 흐름을 감싸는 확장도 가능하지만, 여기서는 직접 구성한 순서를 먼저 익힙니다.


## 여기서 멈춰 확인하기

출처 ID 검사 통과와 답변 내용의 사실성이 다른 이유를 원문과 답변으로 설명해 보세요.

[이전](02-generation.md) · [다음](04-standard-indexing.md) · [단원 목차](../10-rag.md) · [학습 방법](../../learning-guide.md)

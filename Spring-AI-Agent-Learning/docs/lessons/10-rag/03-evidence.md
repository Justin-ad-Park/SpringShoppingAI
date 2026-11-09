# 10-3. 출처를 확인하고 답변을 보류하기

**실행 코드·설정·테스트:** [labs/10-00-rag](../../../labs/10-00-rag/README.md). 저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 출처 ID 확인과 내용의 진실성 확인이 다름을 설명합니다.

**코드 읽기 안내:** 상태 이름을 외우기보다 어떤 조건에서 생성 호출을 하지 않고, 어떤 조건에서 답변을 보류하는지 설명하세요.

## 응답을 채택할지 결정하는 코드

다음은 [RagService.answer()](../../../labs/10-00-rag/src/main/java/com/example/ailearning/l10rag/RagService.java)에서
모델 응답을 받은 뒤 실행하는 부분의 발췌입니다. `draft`는 모델 응답 객체이고, `sources`는 이번 질문으로 실제 검색한 문서 목록입니다.

```java
if (draft != null && Boolean.FALSE.equals(draft.answerable())) {
    return new Reply("NO_ANSWER", "검색된 근거로는 답할 수 없습니다.", List.of());
}
if (draft == null || !Boolean.TRUE.equals(draft.answerable()) || draft.answer() == null || draft
        .answer()
        .isBlank() || draft.sourceIds() == null
        || draft
        .sourceIds()
        .isEmpty()
        || draft
        .sourceIds()
        .stream()
        .anyMatch(id -> sources
                .stream()
                .noneMatch(source -> source
                        .id()
                        .equals(id)))) {
    return new Reply("INVALID_CITATION", "모델 답변의 출처를 확인할 수 없습니다.", List.of());
}
List<Source> cited = sources
        .stream()
        .filter(source -> draft
                .sourceIds()
                .contains(source.id()))
        .toList();
return new Reply("ANSWERED", draft.answer(), cited);
```

모델이 답할 수 없다고 하면 `NO_ANSWER`, 필수 응답이나 실제 검색한 출처 ID가 없으면 `INVALID_CITATION`으로 보류합니다.
마지막 `cited`에는 실제 검색 결과 중 모델이 선택한 문서만 담습니다.

## 결과 상태를 구분하기

| 상태 | 의미 |
|---|---|
| NO_CONTEXT | 검색 결과가 없음. 채팅 생성 없이 Java가 안내 |
| NO_ANSWER | 문서는 찾았지만 모델이 답을 뒷받침하지 못한다고 판단 |
| INVALID_CITATION | 누락되었거나 제공한 목록에 없는 출처 ID. 모델 답변을 채택하지 않음 |
| ANSWERED | 답변 가능으로 응답했고 출처 ID 검사 통과 |

ANSWERED는 **모든 문장이 근거로 증명되었다는 뜻은 아닙니다.** 모델이 유효한 ID를 붙여 틀린 주장을 할 수도 있습니다.
원문과 답변을 비교해야 합니다. 채팅 모델 호출·응답 변환 실패는 서비스가 HTTP 500으로 바꾸며, 빈 질문은 Controller가 400으로 거부합니다.
질문 임베딩·검색은 그 예외 처리 범위 밖에 있습니다. 해당 단계의 외부 호출 실패도 기본 오류 처리에 따라 HTTP 500으로 반환될 수 있지만, 위 서비스의 변환 실패 처리 코드를 거치는 것은 아닙니다.
검색 threshold 0.65와 topK=2는 가상 데이터용 출발값이며 실제 품질 보장 값이 아닙니다.

## 실행

루트 `.env`의 `OPENAI_API_KEY`, `OPENAI_MODEL`, `OPENAI_EMBEDDING_MODEL`을 사용합니다. 설정은 [01장](../01-setup.md#4-실제-모델-연결을-위한-설정-준비)과 [09장](../09-embeddings-search.md)을 참고합니다.
모든 명령은 저장소 루트에서 실행합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag:test --tests com.example.ailearning.l10rag.RagLocalModelTest
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag:bootRun
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

[RagLocalModelTest](../../../labs/10-00-rag/src/test/java/com/example/ailearning/l10rag/RagLocalModelTest.java)는 실제 임베딩/채팅 어댑터를 로컬 고정 서버로 연결해 6개 경로를 검증합니다.
정상 근거 전달, 생성 생략, 잘못된 출처, 답변 보류, 형식 오류, 입력 검증을 재현하며 실제 답변 품질 평가는 아닙니다.
다음 단원에서 고정 질문 세트와 실행 기록을 이용해 개선 여부를 비교합니다.

## 실제 답변을 해석하기

[RagOpenAiLiveTest](../../../labs/10-00-rag/src/test/java/com/example/ailearning/l10rag/RagOpenAiLiveTest.java)는 답변 채택 상태·빈 답변 여부·출처 존재를 검사합니다. 출처가 실제 주장을 뒷받침하는지는 다음 순서로 별도 확인합니다. 자연어 문장은 달라질 수 있어 문장 전체의 일치로 판정하지 않습니다.

| 질문 | 검색에서 확인 | 답변에서 확인 |
|---|---|---|
| 미개봉 상품 반품 기간은? | `return-sealed` 원문의 수령 후 7일 이내 조건 | 7일뿐 아니라 미개봉·수령 후라는 적용 조건을 바꾸거나 생략해 오해를 만들지 않는가 |
| 개봉 상품도 단순 변심 반품이 되나요? | `return-opened`의 단순 변심 불가·불량 별도 확인 | 미개봉 규정을 잘못 적용하거나 모든 사유의 반품이 불가하다고 확대하지 않는가 |
| 우주 여행 비용은? | 해당 정책 자료에 답할 근거가 없음 | 검색 0개이면 생성 생략, 관련 없는 자료가 검색됐다면 답변 보류가 적절한가. 실제 모델이 항상 보류한다고 보장하지 않음 |

검색 원문이 없거나 엉뚱하면 검색·문서 적재 조건부터 확인합니다. 올바른 원문이 전달됐는데 답변이 다르면 프롬프트·답변 생성 문제를 확인합니다. 출처 ID 검사만으로는 이 두 경우를 판정할 수 없습니다.

## 확인 질문

1. 문서의 내용을 바꿨는데 저장소를 재적재하지 않으면 무엇이 검색될까요?
2. 출처 ID가 유효하면 답변이 반드시 사실일까요?
3. 검색한 근거가 없는 경우와, 근거는 찾았지만 그 내용으로 답할 수 없는 경우를 RAG에서 왜 구분해야 하나요?

<details>
<summary>해설</summary>

1. 기존에 적재한 문서와 벡터가 검색됩니다. 변경된 파일을 다시 읽고 임베딩해 저장해야 새 내용이 반영됩니다.
2. 아닙니다. 출처 ID가 검색 결과에 있는지는 확인할 수 있지만, 답변의 주장과 원문이 일치하는지나 원문 자체가 사실인지까지 증명하지는 못합니다.
3. 전자는 검색 결과가 없어 이 예제처럼 생성 호출 전에 답변을 보류할 수 있는 경우이고, 후자는 검색한 자료로 질문에 답할 수 있는지 생성 단계에서 판단하는 경우입니다. 구분하면 검색 조건·문서 적재의 문제와 근거의 충분성·답변 가능성 문제를 나누어 확인할 수 있습니다.

</details>

검색 원문이 실제 프롬프트에 들어간 것을 확인하고 상태별 차이를 설명하면 완료입니다.

보충: [공식 RAG](https://docs.spring.io/spring-ai/reference/api/retrieval-augmented-generation.html).
08에서 배운 Advisor로 이 흐름을 감싸는 확장도 가능하지만, 여기서는 직접 구성한 순서를 먼저 익힙니다.

[이전](02-generation.md) · [다음](04-standard-indexing.md) · [단원 목차](../10-rag.md) · [학습 방법](../../learning-guide.md)

# 10-3. 문서를 읽고 검색 준비하기

**실행 코드·설정·테스트:** [labs/10-03-custom-rag](../../../labs/10-03-custom-rag/README.md).<br>
저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 여러 정책이 담긴 파일을 항목별로 나누고, 원문과 출처를 연결해 검색할 수 있도록 적재합니다.

**코드 읽기 안내:** Lesson03PolicyLoader의 구분자는 이 예제를 위한 단순한 규칙입니다.<br>
범용 문서 파서나 표준 청킹 알고리즘이라고 생각하지 마세요.

## 왜 직접 구현할까?

10-1·2에서는 제공 기능으로 RAG를 실행했습니다.<br>
여기서는 `policies.md`의 제목을 정책 ID로 쓰고 출처 위치를 고정하는 규칙을 직접 구현합니다.<br>
다음 수업에서는 근거가 없을 때 생성 생략과 출처 검증도 추가합니다.<br>
표준 기능 사용에 이 파서 구현이 필수인 것은 아닙니다.

## 검색과 생성은 다른 작업이다

09는 관련 문서를 찾아 반환했습니다.<br>
RAG(Retrieval Augmented Generation)는 이 문서를 참고 자료로 모델에 전달해 답변을 생성하는 방식입니다.<br>
모델을 새로 학습시키는 것이 아닙니다.<br>
이번 호출의 컨텍스트에 자료를 추가합니다.<br>
“찾은 문서가 있다”, “답변을 만들었다”, “답변의 주장이 사실이다”는 서로 다른 확인 사항입니다.

## 먼저 문서를 읽고 나누기

[policies.md](../../../labs/10-03-custom-rag/src/main/resources/policies.md)는 배송비·발송 일정·미개봉 반품·개봉 반품·환불·쿠폰 정책을 한 파일에 담습니다.<br>
각 정책에는 조건과 안내를 설명하는 문장이 여러 개 있습니다.

검색할 수 있도록 나눈 문서 조각을 **청크(chunk)**라고 부릅니다.<br>
이 예제는 `##` 제목 하나와 그 아래 본문을 정책 항목으로 보고, 본문 전체를 Document 하나에 담습니다.<br>
정책의 조건과 예외를 함께 읽을 수 있도록 항목 안의 문장을 임의로 잘라내지 않습니다.

| 파일의 제목 | 검색할 내용 |
|---|---|
| `shipping` | 배송비·무료배송 기준·주문 금액 합산 |
| `delivery-schedule` | 발송 시점·도착 예상·지연 안내 |
| `return-sealed` | 미개봉 반품 기한·접수 방법·제한 조건 |
| `return-opened` | 개봉 후 단순 변심 제한·불량 등 예외·접수 방법 |
| `refund` | 검수 후 환불·결제 수단·반영 기간 |
| `coupon` | 사용 조건·유효 기간·복원 조건 |

원본 파일을 그대로 저장하면 검색 결과 하나에 서로 다른 정책이 모두 포함될 수 있습니다.<br>
항목별로 나누면 다음 수업에서 질문과 관련된 항목만 검색해 모델에 전달할 수 있습니다.<br>
이 파서는 이 교재의 Markdown 형식용이며, 10-1의 TokenTextSplitter와 달리 제목 경계로 분할합니다.

```mermaid
flowchart LR
    F[policies.md 읽기] --> S[Java: 제목별 분할]
    S --> D[Document: ID와 원문과 출처]
    D --> E[임베딩 모델: 문서 벡터]
    E --> V[SimpleVectorStore에 저장]
```

## 제목과 본문이 Document로 바뀌는 예

[policies.md](../../../labs/10-03-custom-rag/src/main/resources/policies.md)에서 미개봉 반품 정책 부분만 발췌하면 다음과 같습니다.

```markdown
## return-sealed
미개봉 상품은 수령 후 7일 이내 반품을 신청할 수 있습니다.
포장과 구성품을 보관한 상태로 주문 내역에서 반품을 접수해야 하며, 상품을 임의로 보내기 전에 회수 안내를 받아야 합니다.
상품별로 별도 안내된 반품 제한 조건이 있으면 그 조건도 함께 확인합니다.
```

`Lesson03PolicyLoader`는 이 부분을 다음 값을 가진 Document 하나로 만듭니다.

| Document의 값 | 변환 결과 | 원문에서 가져온 부분 |
|---|---|---|
| ID | `return-sealed` | `##` 뒤의 제목 |
| 본문 | 위 세 문장 전체 | 제목 다음부터 다음 `##` 이전까지의 내용 |
| 메타데이터의 `source` | `policies.md#return-sealed` | 파일 이름과 위 ID를 연결 |

이렇게 만든 ID와 출처는 나중에 검색한 정책을 식별하고 답변의 출처를 표시할 때 사용합니다.<br>
나머지 다섯 정책도 같은 방식으로 바꾸어 `List<Document>`에 담습니다.

## 문서를 읽고 Document 목록 만들기

다음은 [Lesson03PolicyLoader.java](../../../labs/10-03-custom-rag/src/main/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoader.java)의 전체 코드입니다.<br>
`split()`으로 제목과 본문을 구분하고, `Document.builder()`에서 위 표의 값을 채우는 부분을 연결해 읽으세요.

```java
package com.example.ailearning.l10rag.lesson03_indexing;

import org.springframework.ai.document.Document;
import org.springframework.core.io.ClassPathResource;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 하나의 정책 파일을 ## 항목별로 나누고 ID·원문·출처를 부여한다. 이 교재의 Markdown 형식 전용이다. */
public final class Lesson03PolicyLoader {
    private Lesson03PolicyLoader() {}
    public static List<Document> loadDocuments() throws IOException {
        String markdown;
        try (var input = new ClassPathResource("policies.md").getInputStream()) {
            markdown = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        var documents = new ArrayList<Document>();
        String[] sections = markdown.split("(?m)^## ");
        for (int sectionIndex = 1; sectionIndex < sections.length; sectionIndex++) {
            String[] titleAndBody = sections[sectionIndex]
                    .strip()
                    .split("\n", 2);
            if (titleAndBody.length != 2 || titleAndBody[1].isBlank()) throw new IllegalStateException("정책 절이 비었습니다.");
            String policyId = titleAndBody[0].strip();
            documents.add(Document.builder()
                            .id(policyId)
                            .text(titleAndBody[1].strip())
                            .metadata(Map.of("source", "policies.md#" + policyId))
                            .build());
        }
        return List.copyOf(documents);
    }
}
```

출처는 실제 리소스 위치와 청크 ID로 만듭니다.<br>
모델이 파일 이름을 임의로 생성하도록 맡기지 않습니다.

## 만든 문서를 벡터 저장소에 적재하기

다음은 [Lesson03PolicySearch](../../../labs/10-03-custom-rag/src/main/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicySearch.java)의 생성자 발췌입니다.<br>
서버 시작 시 Spring이 이 객체를 만들면서 정책 문서를 적재합니다.

```java
public Lesson03PolicySearch(EmbeddingModel embeddingModel) throws IOException {
    policyVectorStore = SimpleVectorStore.builder(embeddingModel)
            .build();
    policyVectorStore.add(Lesson03PolicyLoader.loadDocuments());
}
```

`Lesson03PolicyLoader.loadDocuments()`는 파일을 읽어 위 표처럼 ID·본문·출처를 담은 문서 목록을 만듭니다.<br>
그 목록을 받은 `add(...)`가 연결된 임베딩 모델로 본문을 벡터로 바꾸어 저장합니다.<br>
문서 목록 만들기와 임베딩·저장은 서로 다른 단계입니다.

질문이 오면 이미 적재한 문서를 검색하며, 그때마다 파일을 다시 읽어 적재하지 않습니다.<br>
이 저장소는 프로세스 메모리에 보관되므로 서버를 재시작하면 문서를 다시 읽고 임베딩합니다.


## 파일 분할 결과 확인하기

[Lesson03PolicyLoaderUnitTest](../../../labs/10-03-custom-rag/src/test/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoaderUnitTest.java)는 모델 없이 실제 정책 파일을 읽습니다.<br>
콘솔에서 여섯 항목의 원문과 출처를 확인하고, 개봉 반품의 제한과 예외가 같은 조각에 남는지 검사합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests '*Lesson03PolicyLoaderUnitTest'
```

## 확인 질문

RAG 검색 전에 문서를 읽고 분할·임베딩해 저장해야 하는 이유는 무엇인가요? 질문이 올 때마다 같은 문서를 다시 적재하는 것과 어떤 차이가 있나요?

<details>
<summary>해설</summary>

검색할 문서를 적절한 조각과 벡터로 준비해야 질문과 비교해 필요한 근거를 찾을 수 있습니다.<br>
미리 적재한 결과는 여러 질문에서 재사용할 수 있어, 같은 문서를 매번 읽고 임베딩하는 비용과 대기를 줄입니다.<br>
문서가 바뀌면 적재 결과도 갱신해야 합니다.<br>
서버 시작 때 적재하는 것은 이 샘플의 선택이며, RAG가 반드시 서버 시작 때만 적재해야 하는 것은 아닙니다.

</details>

[이전](02-standard-advisor.md) · [다음](04-custom-generation.md) · [단원 목차](../10-rag.md) · [학습 방법](../../learning-guide.md)

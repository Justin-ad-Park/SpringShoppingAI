# 10-3. 문서를 읽고 검색 준비하기

**실행 코드·설정·테스트:** [labs/10-03-custom-rag](../../../labs/10-03-custom-rag/README.md).<br>
저장소 루트에서 해당 README의 명령으로 실행합니다.

**이번 목표:** 폴더의 정책 파일들을 항목별로 나누고, 원문과 출처를 연결해 검색할 수 있도록 적재합니다.

**코드 읽기 안내:** Lesson03PolicyLoader의 구분자는 이 예제를 위한 단순한 규칙입니다.<br>
범용 문서 파서나 표준 청킹 알고리즘이라고 생각하지 마세요.

## 왜 직접 구현할까?

10-1·2에서는 제공 기능으로 RAG를 실행했습니다.<br>
여기서는 `policies/`의 Markdown 파일을 읽어 제목을 정책 ID로 쓰고 출처 위치를 고정하는 규칙을 직접 구현합니다.<br>
다음 수업에서는 근거가 없을 때 생성 생략과 출처 검증도 추가합니다.<br>
표준 기능 사용에 이 파서 구현이 필수인 것은 아닙니다.

## 검색과 생성은 다른 작업이다

09는 관련 문서를 찾아 반환했습니다.<br>
RAG(Retrieval Augmented Generation)는 이 문서를 참고 자료로 모델에 전달해 답변을 생성하는 방식입니다.<br>
모델을 새로 학습시키는 것이 아닙니다.<br>
이번 호출의 컨텍스트에 자료를 추가합니다.<br>
“찾은 문서가 있다”, “답변을 만들었다”, “답변의 주장이 사실이다”는 서로 다른 확인 사항입니다.

## 먼저 문서를 읽고 나누기

`src/main/resources/policies/`에 두 정책 파일을 둡니다.<br>
[delivery-policies.md](../../../labs/10-03-custom-rag/src/main/resources/policies/delivery-policies.md)에는 배송비·발송 일정·쿠폰을, [return-policies.md](../../../labs/10-03-custom-rag/src/main/resources/policies/return-policies.md)에는 반품·환불을 모았습니다.<br>
각 정책에는 조건과 안내를 설명하는 문장이 여러 개 있습니다.

```text
src/main/resources/policies/
├── delivery-policies.md
└── return-policies.md
```

로더는 이 폴더와 하위 폴더의 `.md` 파일을 모두 읽습니다.<br>
정책 파일을 추가하면 로더에 파일 이름을 추가하지 않고 앱을 재시작해 적재할 수 있습니다.

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
    F[policies 폴더의 Markdown 파일 읽기] --> S[Java: 제목별 분할]
    S --> D[Document: ID와 원문과 출처]
    D --> E[임베딩 모델: 문서 벡터]
    E --> V[SimpleVectorStore에 저장]
```

## 제목과 본문이 Document로 바뀌는 예

[return-policies.md](../../../labs/10-03-custom-rag/src/main/resources/policies/return-policies.md)에서 미개봉 반품 정책 부분만 발췌하면 다음과 같습니다.

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
| 메타데이터의 `source` | `policies/return-policies.md#return-sealed` | 폴더 기준 파일 경로와 위 ID를 연결 |

이렇게 만든 ID와 출처는 나중에 검색한 정책을 식별하고 답변의 출처를 표시할 때 사용합니다.<br>
나머지 다섯 정책도 같은 방식으로 바꾸어 `List<Document>`에 담습니다.

## 문서를 읽고 Document 목록 만들기

다음은 [Lesson03PolicyLoader.java](../../../labs/10-03-custom-rag/src/main/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoader.java)의 전체 코드입니다.<br>
먼저 `loadDocuments()`에서 파일 찾기 → 파일별 정책 읽기 → 결과 합치기 순서를 읽습니다.<br>
세부 규칙은 아래 메서드에서 각각 확인합니다.

| 메서드 | 담당하는 처리 |
|---|---|
| `findPolicyFiles()` | 클래스패스의 정책 폴더에서 모든 `.md` 파일 찾기 |
| `loadPolicyFile()` | 파일 하나를 UTF-8로 읽고 정책 항목별 Document로 변환 |
| `extractPolicySections()` | `##` 제목 경계로 나누고 첫 정책 앞의 소개문 제외 |
| `createPolicyDocument()` | ID·본문·원본 파일 경로가 있는 Document 생성 |
| `getPolicySourcePath()` | 출처로 쓸 `policies/` 기준 파일 경로 추출 |

```java
package com.example.ailearning.l10rag.lesson03_indexing;

import org.springframework.ai.document.Document;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** policies/의 Markdown 파일을 읽고 ## 항목별 ID·원문·출처를 부여한다. */
public final class Lesson03PolicyLoader {
    private static final String POLICY_RESOURCE_PATTERN = "classpath*:policies/**/*.md";

    private Lesson03PolicyLoader() {}

    public static List<Document> loadDocuments() throws IOException {
        var documents = new ArrayList<Document>();
        for (Resource policyFile : findPolicyFiles()) {
            documents.addAll(loadPolicyFile(policyFile));
        }
        return List.copyOf(documents);
    }

    private static List<Resource> findPolicyFiles() throws IOException {
        var resolver = new PathMatchingResourcePatternResolver();
        Resource[] policyFiles = resolver.getResources(POLICY_RESOURCE_PATTERN);
        if (policyFiles.length == 0) {
            throw new IOException("policies/ 폴더에서 Markdown 정책 파일을 찾을 수 없습니다.");
        }
        return Arrays.stream(policyFiles)
                .sorted(Comparator.comparing(Resource::getDescription))
                .toList();
    }

    private static List<Document> loadPolicyFile(Resource policyFile) throws IOException {
        String markdownContent = policyFile.getContentAsString(StandardCharsets.UTF_8);
        String sourcePath = getPolicySourcePath(policyFile);
        return extractPolicySections(markdownContent).stream()
                .map(section -> createPolicyDocument(section, sourcePath))
                .toList();
    }

    private static String getPolicySourcePath(Resource policyFile) throws IOException {
        // 일반 폴더와 JAR 내부 URL 모두 policies/부터의 경로만 출처에 남긴다.
        String resourceUrl = policyFile.getURL().toExternalForm();
        return resourceUrl.substring(resourceUrl.lastIndexOf("/policies/") + 1);
    }

    private static List<String> extractPolicySections(String markdownContent) {
        // 줄 시작의 "## "로 나눈 뒤, 첫 정책 항목 앞의 문서 소개는 제외한다.
        return Arrays.stream(markdownContent.split("(?m)^## "))
                .skip(1)
                .toList();
    }

    private static Document createPolicyDocument(String policySection, String sourcePath) {
        // 첫 줄은 ID, 나머지 전체는 본문으로 두어 조건과 예외를 함께 유지한다.
        String[] titleAndBody = policySection
                .strip()
                .split("\n", 2);
        if (titleAndBody.length != 2 || titleAndBody[1].isBlank()) {
            throw new IllegalStateException("정책 절이 비었습니다.");
        }
        String policyId = titleAndBody[0].strip();
        String policyText = titleAndBody[1].strip();
        return Document.builder()
                .id(policyId)
                .text(policyText)
                .metadata(Map.of("source", sourcePath + "#" + policyId))
                .build();
    }
}
```

`PathMatchingResourcePatternResolver`는 Spring이 제공하는 경로 패턴 검색 기능입니다.<br>
디스크 디렉터리뿐 아니라 JAR에 포함된 클래스패스 자료도 찾아주므로 `File`로 변환해 탐색할 필요가 없습니다.

| 패턴의 부분 | 의미 |
|---|---|
| `classpath*:` | 클래스패스에서 일치하는 모든 자료 검색 |
| `policies/` | 정책 파일을 모아 놓은 폴더 |
| `**/` | 현재 폴더와 그 아래 모든 하위 폴더 |
| `*.md` | Markdown 확장자 파일 |

`loadPolicyFile()`은 `getContentAsString()`으로 원문을 읽고, 각 항목에 `createPolicyDocument(section, sourcePath)`를 적용합니다.<br>
`sourcePath`에는 실제 읽은 파일의 경로를 사용하므로, 같은 이름의 파일이 하위 폴더에 있어도 출처를 구분할 수 있습니다.

분할 규칙에서 `(?m)^## `는 각 줄의 시작에 있는 `## `를 찾는 정규식입니다.<br>
분할 결과의 첫 값은 정책 항목 앞의 소개문이므로 `skip(1)`로 제외합니다.<br>
`split("\n", 2)`는 첫 줄과 나머지 본문 두 부분으로만 나눠 본문의 줄바꿈과 예외 설명을 유지합니다.


출처는 실제 파일의 상대 경로와 청크 ID로 만듭니다.<br>
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
각 `##` 정책 ID는 전체 정책 파일에서 중복되지 않게 작성합니다.<br>
그 목록을 받은 `add(...)`가 연결된 임베딩 모델로 본문을 벡터로 바꾸어 저장합니다.<br>
문서 목록 만들기와 임베딩·저장은 서로 다른 단계입니다.

질문이 오면 이미 적재한 문서를 검색하며, 그때마다 파일을 다시 읽어 적재하지 않습니다.<br>
이 저장소는 프로세스 메모리에 보관되므로 서버를 재시작하면 문서를 다시 읽고 임베딩합니다.


## 파일 분할 결과 확인하기

[Lesson03PolicyLoaderUnitTest](../../../labs/10-03-custom-rag/src/test/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyLoaderUnitTest.java)는 모델 없이 실제 정책 파일을 읽습니다.<br>
콘솔에서 두 파일의 여섯 항목 원문과 출처를 확인하고, 개봉 반품의 제한과 예외가 같은 조각에 남는지 검사합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-03-custom-rag:test --tests '*Lesson03PolicyLoaderUnitTest'
```

[JAR 호환성 테스트](../../../labs/10-03-custom-rag/src/test/java/com/example/ailearning/l10rag/lesson03_indexing/Lesson03PolicyJarLoadingUnitTest.java)는 보조 검증용입니다.<br>
JAR 안의 하위 폴더를 포함해 `.md`만 읽고, 파일 이름이 같아도 원본 경로를 구분하는지 확인합니다.

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

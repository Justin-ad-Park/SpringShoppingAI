# 10-1. 제공되는 Reader·Splitter로 검색 자료 준비하기

**목표:** 문서를 읽고 분할한 뒤 벡터 저장소에 적재합니다.<br>
09단원의 검색과 연결하며 채팅 답변은 다음 수업에서 만듭니다.

## 왜 문서를 먼저 준비할까?

“미개봉 반품 기간은?”에 답하려면 모델에 전달할 정책 원문을 먼저 찾을 수 있어야 합니다.<br>
09단원에서는 Java가 짧은 Document를 직접 만들었고, 여기서는 리소스 파일을 읽어 검색 자료로 준비합니다.<br>
긴 파일 전체를 한 문서로 비교하면 필요한 부분이 묻힐 수 있어 검색할 조각으로 나눕니다.

조각을 청크(chunk)라고 부릅니다.<br>
`TokenTextSplitter`는 모델이 텍스트를 처리하는 단위인 토큰 수를 기준으로 문서를 나눕니다.<br>
토큰은 글자 수와 정확히 같지는 않습니다.<br>
적절한 조각 크기는 정책의 문맥을 유지하면서 필요한 부분을 찾도록 정해야 합니다.

## 읽기·분할·적재 역할 연결하기

| 기능 | 제공되는 클래스 | 결과 |
|---|---|---|
| 파일 읽기 | `TextReader` | 본문을 담은 `Document` 목록 |
| 분할 | `TokenTextSplitter` | 검색할 문서 조각 목록 |
| 임베딩·저장 | `SimpleVectorStore` | 질문으로 검색할 수 있는 자료 |

분할은 문장을 요약하는 작업이 아닙니다.<br>
원문이 짧으면 한 조각일 수도 있습니다.<br>
`add()`에서 문서 임베딩 API를 호출합니다.

## 실행 코드

[PolicyDocumentIndexer](../../../labs/10-00-rag-advisor/src/main/java/com/example/ailearning/l10advisor/PolicyDocumentIndexer.java)의 전체 코드입니다.<br>
`@Bean`으로 준비한 저장소를 다음 수업의 Service가 생성자로 받습니다.

```java
package com.example.ailearning.l10advisor;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 표준 Reader·Splitter로 검색 자료를 준비한다. 채팅 모델은 호출하지 않는다. */
@Configuration(proxyBeanMethods = false)
public class PolicyDocumentIndexer {
    @Bean
    public SimpleVectorStore policyVectorStore(EmbeddingModel embeddingModel) {
        var policyReader = new TextReader(new ClassPathResource("return-policy.txt"));
        var policySplitter = TokenTextSplitter.builder()
                .build();
        var policyVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        policyVectorStore.add(policySplitter.apply(policyReader.get()));
        return policyVectorStore;
    }
}
```

## 중첩 호출을 안쪽부터 읽기

`new ClassPathResource("return-policy.txt")`는 애플리케이션의 클래스패스에서 정책 파일을 찾습니다.<br>
원문은 [src/main/resources/return-policy.txt](../../../labs/10-00-rag-advisor/src/main/resources/return-policy.txt)에 있습니다.

`policyVectorStore.add(policySplitter.apply(policyReader.get()))`는 다음 데이터를 순서대로 전달합니다.

| 순서 | 호출 | 입력 → 출력 또는 처리 |
|---|---|---|
| 1 | `policyReader.get()` | 파일을 읽어 본문과 메타데이터를 가진 `List<Document>` 반환 |
| 2 | `policySplitter.apply(...)` | 읽은 문서 목록을 받아 검색할 조각의 `List<Document>` 반환 |
| 3 | `policyVectorStore.add(...)` | 조각 목록의 본문을 임베딩해 벡터 저장소에 적재 |

같은 Document 타입이어도 1번은 읽은 원문이고 2번은 분할한 조각이라는 차이가 있습니다.<br>
짧은 파일이면 분할 결과가 한 개일 수도 있으며, 조각 수를 늘리는 것 자체가 목표는 아닙니다.

Spring은 이 `@Bean` 메서드가 반환한 저장소를 빈으로 등록합니다.<br>
다음 수업의 Service는 같은 저장소를 주입받아 질문마다 검색합니다.<br>
질문을 받을 때마다 원문을 다시 읽어 적재하는 구조가 아닙니다.<br>
이 실습은 시작 시 적재하고 프로세스 메모리에 보관하므로 재시작하면 다시 임베딩하며, 실제 API 사용량이 발생합니다.

## 채팅 없이 적재 검증하기

[PolicyIndexingLocalModelTest](../../../labs/10-00-rag-advisor/src/test/java/com/example/ailearning/l10advisor/PolicyIndexingLocalModelTest.java)는 `indexing-only` 프로필로 답변용 Service·Controller를 제외합니다.<br>
고정 임베딩 응답으로 문서 검색을 확인하고 채팅 호출이 없음을 검증합니다.

```bash
./gradlew :Spring-AI-Agent-Learning:labs:10-00-rag-advisor:test --tests '*PolicyIndexingLocalModelTest' --tests '*TokenTextSplitterUnitTest'
```

[TokenTextSplitterUnitTest](../../../labs/10-00-rag-advisor/src/test/java/com/example/ailearning/l10advisor/TokenTextSplitterUnitTest.java)는 긴 문서의 분할만 검증합니다.<br>
외부 모델 API는 호출하지 않습니다.

실제 모델 연결은 [PolicyIndexingOpenAiLiveTest](../../../labs/10-00-rag-advisor/src/test/java/com/example/ailearning/l10advisor/PolicyIndexingOpenAiLiveTest.java)를 별도로 실행해 확인합니다.<br>
루트 `.env`가 필요하며 API 사용량이 발생합니다.

## 실행 결과에서 확인할 것

로컬 적재 테스트에서는 적재한 정책 원문이 검색되는지, 임베딩 요청 기록이 있는지와 채팅 요청이 없는지를 확인합니다.<br>
분할 단위 테스트에서는 긴 입력이 여러 Document로 나뉘는지 확인합니다.<br>
하나의 짧은 정책이 검색됐다는 결과만으로 긴 문서의 분할까지 검증했다고 해석하지 않습니다.

## 확인 질문

문서 내용이 바뀌면 질문만 다시 보내면 될까요?

<details><summary>해설</summary>
읽기·분할·임베딩 적재를 다시 해야 검색 자료에 반영됩니다.<br>
이 단계는 채팅 모델을 호출하지 않습니다.
</details>

[목차](../10-rag.md) · [다음: Advisor로 답하기](02-standard-advisor.md)

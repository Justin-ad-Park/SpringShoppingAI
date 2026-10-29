package com.example.shopping.tutor.lesson.l07vector;

import com.example.shopping.tutor.lesson.l06tools.support.DemoProductCatalog;
import com.example.shopping.tutor.lesson.l09rag.support.DocumentDisplay;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.*;

import java.util.*;

/** 07 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class VectorStoreLesson {

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public List<Document> products() {
        return DemoProductCatalog.products().stream()
                .map(
                        product ->
                                Document.builder()
                                        .id(product.id())
                                        .text(product.name() + " " + product.category())
                                        .metadata(
                                                Map.of(
                                                        "category",
                                                        product.category(),
                                                        "inStock",
                                                        product.inStock(),
                                                        "price",
                                                        product.price()))
                                        .build())
                .toList();
    }

    /** VectorStore에 저장하고 topK로 검색한 뒤 삭제합니다. */
    public Result indexAndSearch(String question, EmbeddingModel embeddingModel, int topK) {
        // 저장소를 먼저 만들고 문서를 적재한다. 모델 차원이 바뀌면 새로 적재해야 한다.
        var store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(products());
        var results =
                store.similaritySearch(SearchRequest.builder().query(question).topK(topK).build());
        store.delete(List.of("P009"));
        return Result.ok(
                "검색 후 P009를 삭제했습니다. 이번 실행의 저장소만 변경됩니다.",
                Map.of("documents", DocumentDisplay.views(results), "deletedId", "P009"));
    }

    /** 의미 검색과 재고·카테고리 필터를 조합합니다. */
    public Result filterMetadata(String question, EmbeddingModel embeddingModel, int topK) {
        // 저장소를 먼저 만들고 문서를 적재한다. 모델 차원이 바뀌면 새로 적재해야 한다.
        var store = SimpleVectorStore.builder(embeddingModel).build();
        store.add(products());
        var results =
                store.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(topK)
                                .filterExpression("category == '생활' && inStock == true")
                                .build());
        return Result.ok(
                "필터 후에도 원본 상품의 사실을 다시 확인합니다.",
                Map.of(
                        "documents",
                        DocumentDisplay.views(results),
                        "products",
                        results.stream()
                                .map(d -> DemoProductCatalog.findProduct(d.getId()))
                                .toList()));
    }

    /** 선택 DB를 명시적으로 설정하고 동일 VectorStore 계약을 적용합니다. */
    public synchronized Result swapStore(
            String question,
            EmbeddingModel embeddingModel,
            int topK,
            String pgUrl,
            String pgUser,
            String pgPassword) {
        if (pgUrl.isBlank())
            return Result.unsupported(
                    "TUTOR_PGVECTOR_URL과 전용 학습 DB가 필요합니다. scripts/pgvector-up.sh로 준비할 수 있습니다.");
        var ds =
                new org.springframework.jdbc.datasource.DriverManagerDataSource(
                        pgUrl, pgUser, pgPassword);
        var store =
                org.springframework.ai.vectorstore.pgvector.PgVectorStore.builder(
                                new org.springframework.jdbc.core.JdbcTemplate(ds), embeddingModel)
                        .vectorTableName("tutor_vectors_" + embeddingModel.dimensions())
                        .idType(
                                org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIdType
                                        .TEXT)
                        .dimensions(embeddingModel.dimensions())
                        .initializeSchema(true)
                        .build();
        store.afterPropertiesSet();
        store.add(products());
        var result =
                store.similaritySearch(
                        SearchRequest.builder()
                                .query(question)
                                .topK(topK)
                                .filterExpression("category == '생활' && inStock == true")
                                .build());
        return Result.ok(
                "실제 pgvector 저장소에서 검색했습니다.",
                Map.of(
                        "documents",
                        DocumentDisplay.views(result),
                        "store",
                        "pgvector",
                        "dimensions",
                        embeddingModel.dimensions()));
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}

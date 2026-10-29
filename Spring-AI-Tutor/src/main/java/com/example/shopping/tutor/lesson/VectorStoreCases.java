package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class VectorStoreCases {
    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_URL:}") private String pgUrl="";
    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_USER:tutor}") private String pgUser="tutor";
    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_PASSWORD:}") private String pgPassword="";
    public List<Document> products() { return Shop.PRODUCTS.stream().map(p -> Document.builder().id(p.id()).text(p.name()+" "+p.category())
        .metadata(Map.of("category",p.category(),"inStock",p.inStock(),"price",p.price())).build()).toList(); }
    @TutorCase(id="L07-02",title="벡터 저장·검색·삭제",sample="따뜻한 차 마실 용품",goal="VectorStore에 저장하고 topK로 검색한 뒤 삭제합니다.")
    public Outcome indexAndSearch(RunContext c) {
        var store=Knowledge.store(c.embedding,products());
        var results=store.similaritySearch(SearchRequest.builder().query(c.message).topK(c.topK()).build());
        store.delete(List.of("P009"));
        return Outcome.ok("검색 후 P009를 삭제했습니다. 이번 실행의 저장소만 변경됩니다.",Map.of("documents",Knowledge.views(results),"deletedId","P009"));
    }
    @TutorCase(id="L07-03",title="메타데이터 필터",sample="생활 카테고리 중 재고 있는 것",goal="의미 검색과 재고·카테고리 필터를 조합합니다.")
    public Outcome filterMetadata(RunContext c) {
        var store=Knowledge.store(c.embedding,products());
        var results=store.similaritySearch(SearchRequest.builder().query(c.message).topK(c.topK()).filterExpression("category == '생활' && inStock == true").build());
        return Outcome.ok("필터 후에도 원본 상품의 사실을 다시 확인합니다.",Map.of("documents",Knowledge.views(results),"products",results.stream().map(d -> Shop.product(d.getId())).toList()));
    }
    @TutorCase(id="L07-04",title="pgvector 저장소 교체",sample="생활용품을 pgvector에서 검색해줘",goal="선택 DB를 명시적으로 설정하고 동일 VectorStore 계약을 적용합니다.",requires="pgvector")
    public synchronized Outcome swapStore(RunContext c) {
        if(pgUrl.isBlank()) return Outcome.unsupported("TUTOR_PGVECTOR_URL과 전용 학습 DB가 필요합니다. scripts/pgvector-up.sh로 준비할 수 있습니다.");
        var ds=new org.springframework.jdbc.datasource.DriverManagerDataSource(pgUrl,pgUser,pgPassword);
        var store=org.springframework.ai.vectorstore.pgvector.PgVectorStore.builder(new org.springframework.jdbc.core.JdbcTemplate(ds),c.embedding)
            .vectorTableName("tutor_vectors_"+c.embedding.dimensions()).idType(org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIdType.TEXT)
            .dimensions(c.embedding.dimensions()).initializeSchema(true).build();
        store.afterPropertiesSet();store.add(products());
        var result=store.similaritySearch(SearchRequest.builder().query(c.message).topK(c.topK()).filterExpression("category == '생활' && inStock == true").build());
        return Outcome.ok("실제 pgvector 저장소에서 검색했습니다.",Map.of("documents",Knowledge.views(result),"store","pgvector","dimensions",c.embedding.dimensions()));
    }
}

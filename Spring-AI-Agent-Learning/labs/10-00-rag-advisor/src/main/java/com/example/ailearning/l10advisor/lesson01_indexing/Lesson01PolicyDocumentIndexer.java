package com.example.ailearning.l10advisor.lesson01_indexing;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 표준 Reader·Splitter로 검색 자료를 준비한다. 채팅 모델은 호출하지 않는다. */
@Configuration(proxyBeanMethods = false)
public class Lesson01PolicyDocumentIndexer {
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

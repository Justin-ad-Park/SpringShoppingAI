package com.example.ailearning.l10advisor;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class AdvisorRagService {
    private final ChatClient chatClient;

    public AdvisorRagService(ChatClient.Builder builder, EmbeddingModel embeddingModel) {
        // 시작 시: 읽기 → 분할 → 저장. 작은 파일은 한 조각일 수도 있습니다.
        var policyReader = new TextReader(new ClassPathResource("return-policy.txt"));
        var tokenTextSplitter = TokenTextSplitter.builder()
                .build();
        var policyVectorStore = SimpleVectorStore.builder(embeddingModel)
                .build();
        policyVectorStore.add(tokenTextSplitter.apply(policyReader.get()));

        // 질문 시: Advisor가 검색 결과를 프롬프트에 추가합니다.
        var questionAnswerAdvisor = QuestionAnswerAdvisor.builder(policyVectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(1)
                        .similarityThreshold(0.7)
                        .build())
                .build();
        chatClient = builder
                .defaultAdvisors(questionAnswerAdvisor)
                .build();
    }

    public String answer(String question) {
        return chatClient
                .prompt()
                .user(question)
                .call()
                .content();
    }
}

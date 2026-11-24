package com.example.ailearning.l10advisor.lesson02_advisor;

import org.springframework.context.annotation.Profile;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;

@Profile("!indexing-only")
@Service
public class Lesson02AdvisorRagService {
    private final ChatClient chatClient;

    public Lesson02AdvisorRagService(ChatClient.Builder builder, SimpleVectorStore policyVectorStore) {
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

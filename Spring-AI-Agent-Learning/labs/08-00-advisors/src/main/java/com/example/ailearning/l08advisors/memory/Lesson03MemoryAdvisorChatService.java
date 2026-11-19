package com.example.ailearning.l08advisors.memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.stereotype.Service;

/** 08-3: 이전 메시지 조회·전달·저장을 기본 메모리 Advisor에 맡긴다. */
@Service
public class Lesson03MemoryAdvisorChatService {
    public record Reply(String answer, int storedMessages) {}

    private final ChatClient chatClient;
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();

    public Lesson03MemoryAdvisorChatService(ChatClient.Builder builder) {
        chatClient = builder
                .defaultSystem("이전 대화를 참고해 사용자의 질문에 답하세요.")
                // default: 이 ChatClient의 모든 요청에 메모리 기능을 연결한다.
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(conversationMemory)
                        .build())
                .build();
    }

    // synchronized는 chat()/clear()의 동시 실행을 막는다. 모델 응답을 기다리는 동안 다른 사용자도 대기한다.
    public synchronized Reply chat(String owner, String conversationId, String question) {
        String conversationKey = owner + ":" + conversationId;
        String answer = chatClient
                .prompt()
                .user(question)
                // Advisor를 추가하는 코드가 아니라, 이번 요청에서 사용할 대화 키를 설정한다.
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, conversationKey))
                .call()
                .content();
        // Advisor가 저장하므로 conversationMemory.add()를 다시 호출하지 않는다.
        return new Reply(answer, conversationMemory.get(conversationKey).size());
    }

    public synchronized void clear(String owner, String conversationId) {
        conversationMemory.clear(owner + ":" + conversationId);
    }
}

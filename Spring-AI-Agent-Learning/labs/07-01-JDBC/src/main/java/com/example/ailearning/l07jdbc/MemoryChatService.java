package com.example.ailearning.l07jdbc;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MemoryChatService {
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public MemoryChatService(ChatClient.Builder builder, ChatMemory chatMemory) {
        this.chatClient = builder.build();
        this.chatMemory = chatMemory;
    }

    // 단일 서버 실습의 같은 대화에 대한 읽기·저장 충돌을 막습니다. 여러 서버의 동시성 제어는 별도입니다.
    public synchronized String chat(String conversationId, String question) {
        UserMessage currentUserMessage = new UserMessage(question);
        List<Message> requestMessages = createRequestMessages(conversationId, currentUserMessage);
        String answer = callModel(requestMessages);
        validateAnswer(answer);
        saveConversationTurn(conversationId, currentUserMessage, answer);
        return answer;
    }

    private List<Message> createRequestMessages(String conversationId, UserMessage currentUserMessage) {
        List<Message> requestMessages = new ArrayList<>();
        requestMessages.add(new SystemMessage("이전 대화와 현재 질문을 참고해 답하세요. 알 수 없는 정보는 모른다고 말하세요."));
        requestMessages.addAll(chatMemory.get(conversationId));
        requestMessages.add(currentUserMessage);
        return requestMessages;
    }

    private String callModel(List<Message> requestMessages) {
        return chatClient
                .prompt()
                .messages(requestMessages)
                .call()
                .content();
    }

    private void validateAnswer(String answer) {
        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "빈 모델 응답은 저장하지 않습니다.");
        }
    }

    private void saveConversationTurn(String conversationId, UserMessage currentUserMessage, String answer) {
        chatMemory.add(conversationId, List.of(currentUserMessage, new AssistantMessage(answer)));
    }

    public synchronized List<Message> history(String conversationId) {
        return chatMemory.get(conversationId);
    }

    public synchronized void clear(String conversationId) {
        chatMemory.clear(conversationId);
    }
}

package com.example.ailearning.l07memory;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayList;
import java.util.List;

@Service
public class MemoryChatService {
    private static final String SHOPPING_CONVERSATION_INSTRUCTION = """
            쇼핑 대화를 돕습니다. 전달된 이전 메시지와 현재 질문을 참고하세요.
            제공된 정보가 없으면 모른다고 말하세요. 실제 상품 조회 기능은 없습니다.
            """;
    private final ChatClient chatClient;
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();

    public MemoryChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    // 입문용 단일 서버: 같은 대화의 동시 저장 충돌을 막기 위해 요청을 직렬 처리한다.
    public synchronized MemoryReply chat(String owner, String conversationId, String question) {
        String conversationKey = createConversationKey(owner, conversationId);
        UserMessage currentUserMessage = new UserMessage(question);
        List<Message> requestMessages = createRequestMessages(conversationKey, currentUserMessage);
        String answer = callModel(requestMessages);
        validateAnswer(answer);
        saveConversationTurn(conversationKey, currentUserMessage, answer);
        return createMemoryReply(answer, requestMessages, conversationKey);
    }

    private List<Message> createRequestMessages(String conversationKey, UserMessage currentUserMessage) {
        List<Message> requestMessages = new ArrayList<>();
        requestMessages.add(new SystemMessage(SHOPPING_CONVERSATION_INSTRUCTION));
        requestMessages.addAll(conversationMemory.get(conversationKey));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "빈 모델 응답은 저장하지 않습니다.");
        }
    }

    private void saveConversationTurn(String conversationKey, UserMessage currentUserMessage, String answer) {
        conversationMemory.add(conversationKey, List.of(currentUserMessage, new AssistantMessage(answer)));
    }

    private MemoryReply createMemoryReply(String answer, List<Message> requestMessages, String conversationKey) {
        return new MemoryReply(answer, requestMessages
                .stream()
                .map(MessageView::from)
                .toList(), conversationMemory
                .get(conversationKey)
                .size());
    }

    public String chatWithoutMemory(String question) {
        // 메모리 없는 비교 경로: 이전 요청을 조회하거나 저장하지 않는다.
        return chatClient
                .prompt()
                .system(SHOPPING_CONVERSATION_INSTRUCTION)
                .user(question)
                .call()
                .content();
    }

    public synchronized List<MessageView> history(String owner, String conversationId) {
        return conversationMemory
                .get(createConversationKey(owner, conversationId))
                .stream()
                .map(MessageView::from)
                .toList();
    }

    public synchronized void clear(String owner, String conversationId) {
        conversationMemory.clear(createConversationKey(owner, conversationId));
    }

    private String createConversationKey(String owner, String conversationId) {
        // owner는 요청 본문이 아닌 서버 세션에서 가져온다.
        return owner + ":" + conversationId;
    }
}

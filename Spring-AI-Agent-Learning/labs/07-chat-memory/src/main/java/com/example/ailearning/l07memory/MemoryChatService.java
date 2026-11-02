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
    private static final String SYSTEM = """
            쇼핑 대화를 돕습니다. 전달된 이전 메시지와 현재 질문을 참고하세요.
            제공된 정보가 없으면 모른다고 말하세요. 실제 상품 조회 기능은 없습니다.
            """;
    private final ChatClient client;
    private final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(6).build();

    public MemoryChatService(ChatClient.Builder builder) { client = builder.build(); }

    // 입문용 단일 서버: 같은 대화의 동시 저장 충돌을 막기 위해 요청을 직렬 처리한다.
    public synchronized MemoryReply chat(String owner, String conversationId, String question) {
        String key = key(owner, conversationId);
        UserMessage current = new UserMessage(question);
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(SYSTEM));
        messages.addAll(memory.get(key)); // 1. 이전 질문·응답 조회
        messages.add(current);            // 2. 새 질문 추가

        String answer = client.prompt().messages(messages).call().content(); // 3. 함께 전달
        if (answer == null || answer.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "빈 모델 응답은 저장하지 않습니다.");
        }
        // 4. 성공한 질문·응답만 저장. 이 다음 호출에서 재사용한다.
        memory.add(key, List.of(current, new AssistantMessage(answer)));
        return new MemoryReply(answer, messages.stream().map(MessageView::from).toList(), memory.get(key).size());
    }

    public String stateless(String question) {
        // 메모리 없는 비교 경로: 이전 요청을 조회하거나 저장하지 않는다.
        return client.prompt().system(SYSTEM).user(question).call().content();
    }

    public synchronized List<MessageView> history(String owner, String conversationId) {
        return memory.get(key(owner, conversationId)).stream().map(MessageView::from).toList();
    }

    public synchronized void clear(String owner, String conversationId) {
        memory.clear(key(owner, conversationId));
    }

    private String key(String owner, String conversationId) {
        // owner는 요청 본문이 아닌 서버 세션에서 가져온다.
        return owner + ":" + conversationId;
    }
}

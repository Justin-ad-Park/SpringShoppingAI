package com.example.shopping.tutor.lesson.l01chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.*;

/** 01: 같은 모델을 저수준 API와 fluent API로 호출해 비교한다. */
public final class BasicChatLesson {
    private final ChatModel chatModel;
    private final ChatClient chatClient;

    public BasicChatLesson(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.chatClient = ChatClient.create(chatModel);
    }

    public record Comparison(String directModel, String chatClient) {}

    /** 두 번의 독립된 호출이다. LIVE에서는 두 요청 모두 사용량이 발생한다. */
    public Comparison compareModelAndClient(String question) {
        String direct = chatModel.call(new Prompt(question)).getResult().getOutput().getText();
        String fluent = chatClient.prompt().user(question).call().content();
        return new Comparison(direct, fluent);
    }

    /** system은 역할과 경계, user는 이번 질문을 담는다. 재고를 모르면 확인이 필요하다고 답한다. */
    public String separateRoles(String question) {
        return chatClient
                .prompt()
                .system("당신은 학습용 쇼핑 어시스턴트입니다. 한국어로 간결히 답하세요. 제공하지 않은 가격·재고·정책을 만들어내지 마세요.")
                .user(question)
                .call()
                .content();
    }

    /** content() 대신 chatResponse()를 받으면 metadata의 usage도 확인할 수 있다. */
    public ChatResponse inspectOptionsAndUsage(String question, double temperature) {
        if (!Double.isFinite(temperature) || temperature < 0 || temperature > 1)
            throw new IllegalArgumentException("temperature는 0~1입니다.");
        return chatClient
                .prompt()
                .options(ChatOptions.builder().temperature(temperature))
                .user(question)
                .call()
                .chatResponse();
    }
}

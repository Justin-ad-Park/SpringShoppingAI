package com.example.shopping.tutor.lesson.l01chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;

import java.util.Map;

/** 01: 템플릿의 변수와 사용자 입력을 구분한다. */
public final class PromptTemplateLesson {
    public record RenderedAnswer(String answer, String renderedPrompt) {}

    /** {question} 자리에 값만 넣는다. 값 안의 중괄호를 새 템플릿 변수로 해석하지 않는다. */
    public RenderedAnswer renderTemplate(ChatClient chatClient, String question) {
        var template =
                PromptTemplate.builder()
                        .template("쇼핑 질문: {question}\n가격을 단정하지 말고 선물 선택 기준을 3개 제시하세요.")
                        .build();
        String rendered = template.render(Map.of("question", question));
        String answer = chatClient.prompt().user(rendered).call().content();
        return new RenderedAnswer(answer, rendered);
    }
}

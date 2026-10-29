package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l01chat.BasicChatLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatBasicsCases {
    @TutorCase(
            id = "L01-01",
            title = "ChatModel과 ChatClient 비교",
            sample = "선물 고를 때 고려할 점 3개",
            goal = "저수준 Prompt와 fluent API의 전달 메시지를 비교합니다.",
            lesson = BasicChatLesson.class,
            learningMethod = "compareModelAndClient")
    public Outcome compareModelAndClient(LessonExecution execution) {
        var result =
                new BasicChatLesson(execution.model()).compareModelAndClient(execution.userInput());
        return Outcome.ok(
                result.chatClient(),
                Map.of("directModel", result.directModel(), "chatClient", result.chatClient()));
    }

    @TutorCase(
            id = "L01-02",
            title = "System과 User 역할",
            sample = "지금 텀블러 재고가 몇 개야?",
            goal = "프롬프트의 지시와 사용자 질문을 분리하고 지식의 한계를 확인합니다.",
            lesson = BasicChatLesson.class,
            learningMethod = "separateRoles")
    public Outcome separateRoles(LessonExecution execution) {
        return Outcome.text(
                new BasicChatLesson(execution.model()).separateRoles(execution.userInput()));
    }

    @TutorCase(
            id = "L01-04",
            title = "옵션과 토큰 사용량",
            sample = "선물 추천 기준 알려줘",
            goal = "temperature 전달과 provider가 제공하는 usage를 관찰합니다.",
            lesson = BasicChatLesson.class,
            learningMethod = "inspectOptionsAndUsage")
    public Outcome inspectOptionsAndUsage(LessonExecution execution) {
        double temperature =
                ((Number) execution.options.getOrDefault("temperature", 0.2)).doubleValue();
        var response =
                new BasicChatLesson(execution.model())
                        .inspectOptionsAndUsage(execution.userInput(), temperature);
        return Outcome.ok(
                response.getResult().getOutput().getText(),
                Map.of("temperature", temperature, "usageNotice", "usage 미제공은 0원/0토큰을 의미하지 않습니다."));
    }
}

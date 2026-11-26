package com.example.ailearning.l13shopping.lesson09_completeness;

import com.example.ailearning.l13shopping.lesson02_intent.Lesson02CounselWorkflow;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/** 13-9 선택 실습: 필요한 결과 추출 → 기존 상담 → 누락 확인. 대화 저장은 하지 않는다. */
@Service
public class Lesson09CompleteCounselService {
    private final ChatClient chatClient;
    private final Lesson02CounselWorkflow counselWorkflow;
    private final Lesson09ResultCompletenessChecker completenessChecker = new Lesson09ResultCompletenessChecker();

    public Lesson09CompleteCounselService(ChatClient.Builder builder, Lesson02CounselWorkflow counselWorkflow) {
        this.chatClient = builder.build();
        this.counselWorkflow = counselWorkflow;
    }

    public Lesson09ResultCompletenessChecker.Result answer(String question) {
        Lesson09RequiredResults requiredResults = chatClient
                .prompt()
                .system("고객 요청에 상품 추천이 있으면 productRecommendation=true, 정책 안내가 있으면 policyGuidance=true로 두세요. 두 가지를 함께 요청할 수 있습니다.")
                .user(question)
                .call()
                .entity(Lesson09RequiredResults.class);
        if (requiredResults == null) throw new IllegalArgumentException("필요한 결과 항목을 추출하지 못했습니다.");
        requiredResults.validate();
        var counselResult = counselWorkflow.processCounselRequest(question, List.of());
        return completenessChecker.check(requiredResults, counselResult);
    }
}

package com.example.ailearning.l13shopping.lesson09_completeness;

import com.example.ailearning.l13shopping.workflow.CounselResult;
import java.util.ArrayList;
import java.util.List;

/** 기존 금액·ID 검증을 통과한 결과에, 요청한 결과 종류가 모두 있는지 추가로 검사한다. */
public class Lesson09ResultCompletenessChecker {
    public record Result(String status, List<String> missingResults, CounselResult counselResult) {}

    public Result check(Lesson09RequiredResults requiredResults, CounselResult counselResult) {
        requiredResults.validate();
        if (!"VERIFIED".equals(counselResult.status())) {
            return new Result("BASE_RESULT_NOT_VERIFIED", List.of(), counselResult);
        }
        var missingResults = new ArrayList<String>();
        if (requiredResults.productRecommendation() && counselResult.offers().isEmpty()) {
            missingResults.add("상품 추천");
        }
        if (requiredResults.policyGuidance() && counselResult.sources().isEmpty()) {
            missingResults.add("정책 안내");
        }
        return new Result(missingResults.isEmpty() ? "COMPLETE" : "MISSING_RESULTS",
                List.copyOf(missingResults), counselResult);
    }
}

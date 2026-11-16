package com.example.ailearning.l14jev;

import com.example.ailearning.l14jev.domain.SupportPriority;
import com.example.ailearning.l14jev.domain.SupportRoutingPolicy;
import java.util.Map;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Score;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springaicommunity.typesafe.response.SystemOneResponse;
import org.springaicommunity.typesafe.response.ScoreAnswer;
import org.springframework.stereotype.Service;

@Service
public class SupportPriorityService {
    public record SupportPriorityResult(double score, double confidence, Map<Integer, Double> probabilities,
                         SupportPriority priority) {}

    private static final Score URGENCY_QUESTION = Score.of(
            "고객 메시지에 드러난 상담의 시급성을 평가하세요. 사실 여부를 추측하거나 단순한 강한 어조만으로 높게 평가하지 마세요.",
            "일반 상품·정책 문의이며 현재 발생한 문제나 임박한 기한이 없음",
            "배송 지연·상품 하자 등 현재 문제가 있지만 즉시 조치할 기한은 제시되지 않음",
            "오늘까지의 취소 기한·현재 진행 중인 중복 결제 등 즉시 확인이 필요한 사정이 명시됨");
    private final TypeSafeClient typeSafeClient;

    public SupportPriorityService(TypeSafeClient typeSafeClient) {
        this.typeSafeClient = typeSafeClient;
    }

    public SupportPriorityResult assessPriority(String message) {
        SystemOneRequest priorityRequest = SystemOneRequest.builder()
                .state(message)
                .question("urgency", URGENCY_QUESTION)
                .build();
        SystemOneResponse priorityResponse = typeSafeClient.systemOne(priorityRequest);
        ScoreAnswer urgencyAnswer = priorityResponse.score("urgency");
        SupportPriority priority = SupportRoutingPolicy.determinePriority(urgencyAnswer.value(), urgencyAnswer.confidence());
        return new SupportPriorityResult(urgencyAnswer.value(), urgencyAnswer.confidence(), urgencyAnswer.probabilities(), priority);
    }
}

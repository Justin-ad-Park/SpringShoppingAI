package com.example.ailearning.l14jev;

import com.example.ailearning.l14jev.domain.HumanSupportAction;
import com.example.ailearning.l14jev.domain.SupportRoutingPolicy;
import org.springaicommunity.typesafe.TypeSafeClient;
import org.springaicommunity.typesafe.question.Noul;
import org.springaicommunity.typesafe.question.SystemOneRequest;
import org.springframework.stereotype.Service;

@Service
public class HumanSupportService {
    public record Result(double yesProbability, HumanSupportAction action) {}

    private static final Noul HUMAN_SUPPORT_QUESTION = Noul.builder()
            .instructions("고객이 자동 응답 대신 사람 상담원과의 연결을 요청하고 있다.")
            .whenTrue("사람 상담원과 통화하거나 채팅하고 싶다는 명시적인 요청이 있음")
            .whenFalse("단순 문의이거나 상담원 연결을 원하지 않는다고 말함")
            .build();
    private final TypeSafeClient client;

    public HumanSupportService(TypeSafeClient client) {
        this.client = client;
    }

    public Result assess(String message) {
        var request = SystemOneRequest.builder()
                .state(message)
                .question("human_support", HUMAN_SUPPORT_QUESTION)
                .build();
        double yesProbability = client.systemOne(request).noulValue("human_support");
        HumanSupportAction action = SupportRoutingPolicy.determineHumanSupportAction(yesProbability);
        return new Result(yesProbability, action);
    }
}

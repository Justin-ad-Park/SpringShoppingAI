package com.example.shopping.tutor.lesson;

import java.util.Map;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class AdvisorCases {
    @TutorCase(id="L05-01",title="Advisor 호출 순서",sample="선물 고르는 기준 알려줘",goal="A before → B before → 모델 → B after → A after를 확인합니다.")
    public Outcome traceOrder(RunContext c) {
        return Outcome.text(c.client().prompt().advisors(new TraceAdvisor("A",10,c.advisorSteps),new TraceAdvisor("B",20,c.advisorSteps))
            .user(c.userInput()).call().content());
    }
    @TutorCase(id="L05-02",title="진단 출력 마스킹",sample="010-1234-5678로 배송 문의하고 싶어",goal="모델 전달과 화면 진단 마스킹의 경계를 구분합니다.")
    public Outcome maskDiagnostics(RunContext c) {
        String answer=c.client().prompt().user(c.userInput()).call().content();
        return Outcome.ok(RunContext.mask(answer),Map.of("notice","관찰 패널은 마스킹합니다. 원문 provider 전송을 차단하는 기능과는 다릅니다."));
    }
    @TutorCase(id="L05-03",title="Memory와 Trace 조합",sample="내 예산은 3만원이야",goal="advisor 조합 시 메모리 중복 저장과 실제 전달 이력을 확인합니다.")
    public Outcome composeMemoryAndTrace(RunContext c) {
        return Outcome.text(c.client().prompt().advisors(new TraceAdvisor("trace",0,c.advisorSteps),MessageChatMemoryAdvisor.builder(c.memory).build())
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID,c.conversationId)).user(c.userInput()).call().content());
    }
}

package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.chat.prompt.*;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class ChatBasicsCases {
    @TutorCase(id="L01-01",title="ChatModel과 ChatClient 비교",sample="선물 고를 때 고려할 점 3개",goal="저수준 Prompt와 fluent API의 전달 메시지를 비교합니다.")
    public Outcome compareModelAndClient(RunContext c) {
        String direct=c.model().call(new Prompt(c.userInput())).getResult().getOutput().getText();
        String fluent=c.client().prompt().user(c.userInput()).call().content();
        return Outcome.ok(fluent,Map.of("directModel",direct,"chatClient",fluent));
    }
    @TutorCase(id="L01-02",title="System과 User 역할",sample="지금 텀블러 재고가 몇 개야?",goal="프롬프트의 지시와 사용자 질문을 분리하고 지식의 한계를 확인합니다.")
    public Outcome separateRoles(RunContext c) { return Outcome.text(c.client().prompt().user(c.userInput()).call().content()); }

    @TutorCase(id="L01-04",title="옵션과 토큰 사용량",sample="선물 추천 기준 알려줘",goal="temperature 전달과 provider가 제공하는 usage를 관찰합니다.")
    public Outcome inspectOptionsAndUsage(RunContext c) {
        double t=((Number)c.options.getOrDefault("temperature",0.2)).doubleValue();
        if(t<0 || t>1) throw new IllegalArgumentException("temperature는 0~1입니다.");
        var response=c.client().prompt().options(ChatOptions.builder().temperature(t).build()).user(c.userInput()).call().chatResponse();
        return Outcome.ok(response.getResult().getOutput().getText(),Map.of("temperature",t,"usageNotice","usage 미제공은 0원/0토큰을 의미하지 않습니다."));
    }
}

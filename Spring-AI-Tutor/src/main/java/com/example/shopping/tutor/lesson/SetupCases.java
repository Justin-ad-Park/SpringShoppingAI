package com.example.shopping.tutor.lesson;

import java.util.Map;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class SetupCases {
    @TutorCase(id="L00-01",title="첫 ChatClient 호출",sample="쇼핑 도우미야, 안녕",goal="서버에서 모델을 호출하고 응답을 관찰합니다.")
    public Outcome helloStub(RunContext c) { return Outcome.text(c.client().prompt().user(c.userInput()).call().content()); }

    @TutorCase(id="L00-02",title="실행 모드와 모델",sample="현재 실행 환경을 알려줘",goal="STUB과 LIVE, 자격증명의 서버 경계를 구분합니다.")
    public Outcome inspectMode(RunContext c) {
        return Outcome.ok(c.settings.mode().equals("stub")?"준비된 시나리오를 재현하는 STUB입니다.":"실제 API를 호출하는 LIVE입니다. 사용량이 발생합니다.",
            Map.of("mode",c.settings.mode(),"model",c.settings.mode().equals("stub")?"scripted":c.settings.model()));
    }
}

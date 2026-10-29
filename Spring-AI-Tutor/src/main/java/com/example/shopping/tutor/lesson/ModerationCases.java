package com.example.shopping.tutor.lesson;

import java.util.Map;
import org.springframework.ai.moderation.ModerationPrompt;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class ModerationCases {
    private final MediaModels models;
    public ModerationCases(MediaModels models) { this.models=models; }
    @TutorCase(id="L14-05",title="Moderation 모델",sample="상품 문의를 하고 싶어요",goal="콘텐츠 판정 결과를 관찰합니다. 업무 권한 검증과는 별개입니다.",requires="live")
    public Outcome checkAbusiveMessage(RunContext c) {
        if(c.settings.mode().equals("stub")) return Outcome.unsupported("실제 moderation 판정은 LIVE에서 실행하세요.");
        var result=models.moderation.call(new ModerationPrompt(c.message)).getResult().getOutput();
        return Outcome.ok("콘텐츠 모델의 판정입니다. 장애 시 안전하다고 확정하지 않습니다.",Map.of("moderation",result));
    }
}

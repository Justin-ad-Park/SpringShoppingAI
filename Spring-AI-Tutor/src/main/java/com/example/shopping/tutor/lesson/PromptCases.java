package com.example.shopping.tutor.lesson;

import java.util.Map;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class PromptCases {
    @TutorCase(id="L01-03",title="템플릿 변수",sample="예산 3만원, 동료 선물 추천 기준",goal="변수 치환 후의 프롬프트를 확인합니다. 사용자 입력을 템플릿으로 실행하지 않습니다.")
    public Outcome renderTemplate(RunContext c) {
        var template=PromptTemplate.builder().template("쇼핑 질문: {question}\n가격을 단정하지 말고 선물 선택 기준을 3개 제시하세요.").build();
        String rendered=template.render(Map.of("question",c.message));
        return Outcome.ok(c.client().prompt().user(rendered).call().content(),Map.of("rendered",RunContext.mask(rendered)));
    }
}

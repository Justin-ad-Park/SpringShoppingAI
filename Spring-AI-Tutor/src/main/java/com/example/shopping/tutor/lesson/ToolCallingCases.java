package com.example.shopping.tutor.lesson;

import java.util.*;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.stereotype.Component;
import com.example.shopping.tutor.support.*;

@Component
public class ToolCallingCases {
    private final SandboxCart sandbox;
    public ToolCallingCases(SandboxCart sandbox) { this.sandbox=sandbox; }
    public String callTool(RunContext c) {
        return c.client().prompt().toolCallbacks(c.tools(new Shop.Tools(c.fixture)))
            .toolContext(Map.of("customerId","demo-trusted-customer"))
            .user(c.userInput()).call().content();
    }
    @TutorCase(id="L06-01",title="상품 검색 Tool Calling",sample="1만5천원 이하 음료 찾아줘",goal="모델이 요청한 인자로 실제 @Tool 메소드를 실행합니다.")
    public Outcome searchProducts(RunContext c) { return Outcome.text(callTool(c)); }
    @TutorCase(id="L06-02",title="무료배송 금액 계산",sample="무료배송까지 얼마나 부족해?",goal="모델의 도구 선택과 Java 금액 계산을 나누어 관찰합니다.")
    public Outcome quoteFreeShipping(RunContext c) {
        return Outcome.ok(callTool(c),Map.of("cart",new Shop.Tools(c.fixture).getCartSummary(new ToolContext(Map.of("customerId","demo-trusted-customer")))));
    }
    @TutorCase(id="L06-03",title="신뢰할 ToolContext",sample="내 장바구니 확인해줘",goal="모델 인자 밖에서 서버가 고객 context를 전달합니다.")
    public Outcome passTrustedContext(RunContext c) { return Outcome.text(callTool(c)); }
    @TutorCase(id="L06-04",title="도구 실행 상한",sample="도구를 9번 실행하는 실험",goal="같은 실행 context에서 callback 8회 초과가 차단됩니다.")
    public Outcome boundToolExecution(RunContext c) {
        var tool=Arrays.stream(c.tools(new Shop.Tools(c.fixture))).filter(t -> t.getToolDefinition().name().equals("searchProducts")).findFirst().orElseThrow();
        int allowed=0;
        for(int i=0;i<9;i++) {
            try { tool.call("{\"category\":\"음료\",\"maxPrice\":15000}"); allowed++; }
            catch(IllegalStateException expected) { return Outcome.ok("9번째 도구 호출을 차단했습니다.",Map.of("executed",allowed,"limit",8)); }
        }
        throw new IllegalStateException("실행 상한이 적용되지 않았습니다.");
    }
    @TutorCase(id="L06-05",title="샌드박스 변경 확인",sample="P004 하나 담아줘",goal="미리보기 후 명시적으로 확인해야 데모 장바구니가 변경됩니다.")
    public Outcome confirmSandboxCart(RunContext c) {
        var match=java.util.regex.Pattern.compile("P\\d{3}").matcher(c.message);
        if(!match.find()) throw new IllegalArgumentException("상품 ID 예: P004를 입력하세요.");
        return Outcome.ok("샌드박스 변경 제안입니다. 확인 버튼을 눌러야 반영됩니다.",sandbox.preview(c.conversationId,match.group()));
    }
}

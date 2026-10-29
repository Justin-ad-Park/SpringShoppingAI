package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l06tools.CartSummaryTools;
import com.example.shopping.tutor.lesson.l06tools.ProductSearchTools;
import com.example.shopping.tutor.lesson.l06tools.ProductToolCallingLesson;
import com.example.shopping.tutor.lesson.l06tools.support.SandboxCart;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 ProductToolCallingLesson부터 읽는다. */
@Component
public class ToolCallingCases {
    private final SandboxCart sandbox;

    public ToolCallingCases(SandboxCart sandbox) {
        this.sandbox = sandbox;
    }

    public String callTool(LessonExecution execution) {
        return new ProductToolCallingLesson(sandbox)
                .callTool(
                        execution.client(),
                        execution.userInput(),
                        execution.tools(
                                new ProductSearchTools(), new CartSummaryTools(execution.fixture)));
    }

    @TutorCase(
            id = "L06-01",
            title = "상품 검색 Tool Calling",
            sample = "1만5천원 이하 음료 찾아줘",
            goal = "모델이 요청한 인자로 실제 @Tool 메소드를 실행합니다.",
            lesson = ProductToolCallingLesson.class,
            learningMethod = "searchProducts")
    public Outcome searchProducts(LessonExecution execution) {
        var result =
                new ProductToolCallingLesson(sandbox)
                        .searchProducts(
                                execution.client(),
                                execution.userInput(),
                                execution.tools(
                                        new ProductSearchTools(),
                                        new CartSummaryTools(execution.fixture)));
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L06-02",
            title = "무료배송 금액 계산",
            sample = "무료배송까지 얼마나 부족해?",
            goal = "모델의 도구 선택과 Java 금액 계산을 나누어 관찰합니다.",
            lesson = ProductToolCallingLesson.class,
            learningMethod = "quoteFreeShipping")
    public Outcome quoteFreeShipping(LessonExecution execution) {
        var result =
                new ProductToolCallingLesson(sandbox)
                        .quoteFreeShipping(
                                execution.client(),
                                execution.userInput(),
                                execution.fixture,
                                execution.tools(
                                        new ProductSearchTools(),
                                        new CartSummaryTools(execution.fixture)));
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L06-03",
            title = "신뢰할 ToolContext",
            sample = "내 장바구니 확인해줘",
            goal = "모델 인자 밖에서 서버가 고객 context를 전달합니다.",
            lesson = ProductToolCallingLesson.class,
            learningMethod = "passTrustedContext")
    public Outcome passTrustedContext(LessonExecution execution) {
        var result =
                new ProductToolCallingLesson(sandbox)
                        .passTrustedContext(
                                execution.client(),
                                execution.userInput(),
                                execution.tools(
                                        new ProductSearchTools(),
                                        new CartSummaryTools(execution.fixture)));
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L06-04",
            title = "도구 실행 상한",
            sample = "도구를 9번 실행하는 실험",
            goal = "같은 실행 context에서 callback 8회 초과가 차단됩니다.",
            lesson = ProductToolCallingLesson.class,
            learningMethod = "boundToolExecution")
    public Outcome boundToolExecution(LessonExecution execution) {
        var result =
                new ProductToolCallingLesson(sandbox)
                        .boundToolExecution(
                                execution.tools(
                                        new ProductSearchTools(),
                                        new CartSummaryTools(execution.fixture)));
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L06-05",
            title = "샌드박스 변경 확인",
            sample = "P004 하나 담아줘",
            goal = "미리보기 후 명시적으로 확인해야 데모 장바구니가 변경됩니다.",
            lesson = ProductToolCallingLesson.class,
            learningMethod = "confirmSandboxCart")
    public Outcome confirmSandboxCart(LessonExecution execution) {
        var result =
                new ProductToolCallingLesson(sandbox)
                        .confirmSandboxCart(execution.userInput(), execution.conversationId);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}

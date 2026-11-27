package com.example.ailearning.l13shopping.lesson11_agent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** 실제 OpenAI 채팅·임베딩과 로컬 상품·정책 도구를 연결한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(120)
class Lesson11ShoppingAgentOpenAiLiveTest {
    @Autowired Lesson11ShoppingAgent shoppingAgent;
    @MockitoSpyBean Lesson11ShoppingTools shoppingTools;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_OpenAI가_상품_총액과_미개봉_반품_기간을_함께_안내한다() {
        String answer = shoppingAgent.answer("3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.", List.of());

        System.out.println(answer);
        verifyShoppingAndReturnAnswer(answer);
    }

    @Test
    void 실제_OpenAI가_빠진_정보를_묻고_고객이_답하면_조회를_이어서_처리한다() {
        String firstQuestion = "음료 추천해 주고 반품 기간도 알려줘.";
        String clarification = shoppingAgent.answer(firstQuestion, List.of());

        System.out.println("확인 질문: " + clarification);
        assertThat(clarification).containsAnyOf("예산", "얼마");
        assertThat(clarification).containsAnyOf("개봉", "미개봉");

        List<Message> previousMessages = List.of(new UserMessage(firstQuestion), new AssistantMessage(clarification));
        String answer = shoppingAgent.answer("배송비 포함 3만원이고, 개봉하지 않았어요.", previousMessages);

        System.out.println("이어서 받은 답변:\n" + answer);
        verifyShoppingAndReturnAnswer(answer);
    }

    @Test
    void 실제_OpenAI가_반품_기간만_물으면_정책_도구만_사용한다() {
        String answer = shoppingAgent.answer("미개봉 반품 기간만 알려줘.", List.of());

        System.out.println(answer);
        verify(shoppingTools, atLeastOnce()).searchReturnPolicy(anyString());
        verify(shoppingTools, never()).searchProducts(any(), any());
        verify(shoppingTools, never()).calculateShipping(anyString());
        assertReturnPolicyAnswer(answer);
    }

    private void verifyShoppingAndReturnAnswer(String answer) {
        verify(shoppingTools, atLeastOnce()).searchProducts(any(), any());
        verify(shoppingTools, atLeastOnce()).calculateShipping("P01");
        verify(shoppingTools, atLeastOnce()).searchReturnPolicy(anyString());
        assertThat(answer).contains("허브티");
        assertThat(answer).containsPattern("15[,]?000\\s*원|1만\\s*5천\\s*원");
        assertReturnPolicyAnswer(answer);
    }

    private static void assertReturnPolicyAnswer(String answer) {
        assertThat(answer).containsAnyOf("수령", "받은");
        assertThat(answer).containsPattern("7\\s*일|일주일");
        assertThat(answer).contains("return-sealed");
    }
}

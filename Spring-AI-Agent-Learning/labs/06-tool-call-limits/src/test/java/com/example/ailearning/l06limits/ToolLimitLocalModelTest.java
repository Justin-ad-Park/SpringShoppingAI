package com.example.ailearning.l06limits;

import com.example.ailearning.testsupport.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static com.example.ailearning.testsupport.OpenAiChatStub.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {"spring.config.import="})
class ToolLimitLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    @Autowired ToolLimitService toolLimitService;
    @MockitoSpyBean ShoppingTools shoppingTools;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.api-key", () -> "local-test-key");
        registry.add("spring.ai.openai.chat.options.model", () -> "learning-model");
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
    }

    @AfterAll
    static void 종료() { MODEL.close(); }

    @Test
    void 도구별_상한을_초과한_세번째_검색은_실행하지_않는다() {
        // Given: 모델은 같은 검색을 계속 요청한다. 설정은 도구별 최대 2회이다.
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantToolCallMessage(
                "search-" + MODEL.chat().getRequests().size(), "searchProducts", "{\"category\":\"음료\"}"));

        // When: Spring AI의 자동 도구 루프 실행
        var reply = toolLimitService.ask("음료를 계속 검색해줘.");
        System.out.println("제한 결과: " + reply);
        System.out.println("모델 요청 수: " + MODEL.chat().getRequests().size());

        // Then: 세 번째 요청은 실행 전에 차단된다. verify는 이미 실행된 호출을 검사한다.
        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        assertThat(reply.finishReason()).isEqualTo("toolCallLimitExceeded");
        verify(shoppingTools, times(2)).searchProducts("음료");
        verify(shoppingTools, never()).calculateShipping(anyString());
        assertThat(MODEL.chat().getRequests()).hasSize(3);
    }

    @Test
    void 전체_도구_호출_허용_횟수를_초과하면_다음_실행을_차단한다() {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("s1", "searchProducts", "{\"category\":\"음료\"}"),
                createAssistantToolCallMessage("d1", "calculateShipping", "{\"productId\":\"P01\"}"),
                createAssistantToolCallMessage("s2", "searchProducts", "{\"category\":\"음료\"}"),
                createAssistantToolCallMessage("d2", "calculateShipping", "{\"productId\":\"P01\"}"));
        var reply = toolLimitService.ask("검색과 배송비 조회를 반복해줘.");
        System.out.println("전체 상한 결과: " + reply);
        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        verify(shoppingTools, times(2)).searchProducts("음료");
        verify(shoppingTools, times(1)).calculateShipping("P01");
        assertThat(MODEL.chat().getRequests()).hasSize(4);
    }

    @Test
    void 상한_안에서는_도구_결과를_받아_정상적으로_답한다() {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("s1", "searchProducts", "{\"category\":\"음료\"}"),
                createAssistantToolCallMessage("d1", "calculateShipping", "{\"productId\":\"P01\"}"),
                createAssistantTextMessage("허브티 12000원, 배송비 3000원으로 총 15000원입니다."));
        var reply = toolLimitService.ask("음료 가격과 배송비를 알려줘.");
        System.out.println("정상 결과: " + reply);
        assertThat(reply.status()).isEqualTo("COMPLETED");
        assertThat(reply.answer()).contains("15000");
        assertThat(MODEL.chat().getRequests()).hasSize(3);
    }

    @Test
    void 새_사용자_요청에는_이전_요청의_도구_횟수를_합산하지_않는다() {
        MODEL
                .chat()
                .setResponseFunction(request -> createAssistantToolCallMessage(
                "search-" + MODEL.chat().getRequests().size(), "searchProducts", "{\"category\":\"음료\"}"));
        var first = toolLimitService.ask("계속 검색");
        var second = toolLimitService.ask("다시 계속 검색");
        System.out.println("첫 요청: " + first + " / 다음 요청: " + second);
        assertThat(first.status()).isEqualTo("TOOL_LIMIT");
        assertThat(second.status()).isEqualTo("TOOL_LIMIT");
        verify(shoppingTools, times(4)).searchProducts("음료");
        assertThat(MODEL.chat().getRequests()).hasSize(6);
    }

    @Test
    void 도구가_필요_없는_인사에는_도구를_실행하지_않는다() {
        MODEL
                .chat()
                .setResponseText("안녕하세요.");
        var reply = toolLimitService.ask("안녕");
        System.out.println("인사 결과: " + reply);
        assertThat(reply.status()).isEqualTo("COMPLETED");
        verifyNoInteractions(shoppingTools);
    }
}

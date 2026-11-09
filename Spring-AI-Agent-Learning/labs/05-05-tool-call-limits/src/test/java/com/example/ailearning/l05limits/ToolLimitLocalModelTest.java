package com.example.ailearning.l05limits;

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

        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 세 번째 요청은 실행 전에 차단된다. verify는 이미 실행된 호출을 검사한다.
        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        assertThat(reply.finishReason()).isEqualTo("toolCallLimitExceeded");
        verify(shoppingTools, times(2)).searchProducts("음료");
        verify(shoppingTools, never()).calculateShipping(anyString());
        assertThat(MODEL.chat().getRequests()).hasSize(3);
    }

    @Test
    void 세_카테고리의_검색을_요청해도_두_카테고리까지만_검색한다() {
        // Given: 카테고리마다 검색 도구를 요청하는 모델 응답
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("s1", "searchProducts", "{\"category\":\"음료\"}"),
                        createAssistantToolCallMessage("s2", "searchProducts", "{\"category\":\"생활\"}"),
                        createAssistantToolCallMessage("s3", "searchProducts", "{\"category\":\"간식\"}"));

        // When: 같은 도구로 세 카테고리 검색을 시도
        var reply = toolLimitService.ask("음료, 생활, 간식을 각각 검색해서 정리해줘.");
        System.out.println("카테고리 검색 결과: " + reply);
        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 세 번째 도구 실행은 차단. 앞서 검색한 카테고리는 실제 실행 기록으로 확인
        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        assertThat(reply.finishReason()).isEqualTo("toolCallLimitExceeded");
        verify(shoppingTools).searchProducts("음료");
        verify(shoppingTools).searchProducts("생활");
        verify(shoppingTools, never()).searchProducts("간식");
        assertThat(MODEL.chat().getRequests()).hasSize(3);
    }

    @Test
    void 전체_도구_호출_허용_횟수를_초과하면_다음_실행을_차단한다() {
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("s1", "searchProducts", "{\"category\":\"음료\"}"),
                createAssistantToolCallMessage("d1", "calculateShipping", "{\"productId\":\"P01\"}"),
                createAssistantToolCallMessage("s2", "searchProducts", "{\"category\":\"생활\"}"),
                createAssistantToolCallMessage("d2", "calculateShipping", "{\"productId\":\"P03\"}"));
        var reply = toolLimitService.ask("검색과 배송비 조회를 반복해줘.");
        System.out.println("전체 상한 결과: " + reply);
        ToolCallPrinter.printToolCalls(shoppingTools);

        assertThat(reply.status()).isEqualTo("TOOL_LIMIT");
        verify(shoppingTools).searchProducts("음료");
        verify(shoppingTools).searchProducts("생활");
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
        // Given: 실제 AI 대신, 검색 도구 호출 1회와 최종 답변을 순서대로 반환한다.
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("first-search", "searchProducts", "{\"category\":\"음료\"}"),
                        createAssistantTextMessage("음료 검색을 완료했습니다."));

        // When: 첫 사용자 요청에서 검색을 한 번 실행한다.
        var first = toolLimitService.ask("음료를 한 번만 검색해줘.");
        System.out.println("첫 요청: " + first);
        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 제한에 걸리지 않고 정상 종료한다.
        assertThat(first.status()).isEqualTo("COMPLETED");
        verify(shoppingTools).searchProducts("음료");
        verifyNoMoreInteractions(shoppingTools);
        // 모델에 사용자 질문을 보내고, 도구 실행 결과를 보내 최종 답변을 받으므로 총 2회 요청한다.
        assertThat(MODEL.chat().getRequests()).hasSize(2);

        // Given: 출력·검증용 Spy 기록만 비운다. Spring AI의 제한 횟수를 초기화하는 코드는 아니다.
        clearInvocations(shoppingTools);
        MODEL
                .chat()
                .setResponseSequence(
                        createAssistantToolCallMessage("second-search-1", "searchProducts", "{\"category\":\"생활\"}"),
                        createAssistantToolCallMessage("second-search-2", "searchProducts", "{\"category\":\"간식\"}"),
                        createAssistantTextMessage("생활과 간식 검색을 완료했습니다."));

        // When: 새 사용자 요청에서 같은 검색 도구를 두 번 실행한다.
        var second = toolLimitService.ask("생활과 간식을 각각 한 번씩 검색해줘.");
        System.out.println("두번째 요청: " + second);
        ToolCallPrinter.printToolCalls(shoppingTools);

        // Then: 새 사용자 요청에서는 검색 도구의 제한 횟수를 처음부터 센다.
        // 첫 요청에서 이미 한 번 검색했어도, 두 번째 요청에서 다시 두 번 검색하고 정상 종료한다.
        assertThat(second.status()).isEqualTo("COMPLETED");
        verify(shoppingTools).searchProducts("생활");
        verify(shoppingTools).searchProducts("간식");
        verifyNoMoreInteractions(shoppingTools);
        // 모델 요청 기록은 누적된다. 첫 요청 2회 + 다음 요청 3회이다.
        assertThat(MODEL.chat().getRequests()).hasSize(5);
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

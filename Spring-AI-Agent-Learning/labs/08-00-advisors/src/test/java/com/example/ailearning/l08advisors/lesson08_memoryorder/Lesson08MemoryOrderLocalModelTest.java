package com.example.ailearning.l08advisors.lesson08_memoryorder;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantTextMessage;
import static com.example.ailearning.testsupport.model.OpenAiChatStub.createAssistantToolCallMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = Lesson08MemoryOrderLocalModelTest.ModelConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.config.import=")
@ActiveProfiles("test")
class Lesson08MemoryOrderLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private static final String CONVERSATION_ID = "memory-order";

    // 테스트마다 새 메모리와 도구 객체를 준비한다.
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();
    private final TeaPriceTools teaPriceTools = spy(new TeaPriceTools());

    @Autowired
    ChatModel chatModel;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class ModelConfiguration {}

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
        MODEL
                .chat()
                .setResponseSequence(
                createAssistantToolCallMessage("price-1", "getTeaPrice", "{\"productName\":\"허브티\"}"),
                createAssistantTextMessage("허브티 가격은 12000원입니다."));
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 기본_order에서는_전후_처리를_한번씩_하고_질문과_최종_답변만_저장한다() {
        // Given: order를 지정하지 않아 Builder의 기본값을 사용한다.
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
                .build());
        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        // When: 한 사용자 질문에서 모델이 도구를 요청하고 결과로 최종 답변을 만든다.
        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");

        // Then: 실제 실행 순서와 메모리에 저장된 역할·본문을 함께 확인한다.
        System.out.println("최종 답변: " + answer);
        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        assertThat(memoryAdvisor.getOrder()).isEqualTo(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER);
        assertThat(memoryAdvisor.getOrder()).isLessThan(ToolCallingAdvisor.DEFAULT_ORDER);
        assertThat(calls).containsExactly("before", "after");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").get(0).path("role").asString()).isEqualTo("system");
        verify(teaPriceTools).getTeaPrice("허브티");
        assertThat(messages).extracting(message -> message.getMessageType()).containsExactly(MessageType.USER, MessageType.ASSISTANT);
        assertThat(messages.get(0).getText()).isEqualTo("허브티 가격은 얼마인가요?");
        assertThat(messages.get(1).getText()).isEqualTo(answer);
        assertThat(((AssistantMessage) messages.get(1)).getToolCalls()).isEmpty();
        printAndVerifyToolResultSentToModel();
    }

    @Test
    void 반복_안에서는_모델_요청마다_전후_처리를_하고_도구_메시지도_저장한다() {
        // Given: ToolCallingAdvisor보다 큰 order로 반복 안에 배치한다.
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
                .order(ToolCallingAdvisor.DEFAULT_ORDER + 100)
                .build());
        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        // When
        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");

        // Then
        System.out.println("최종 답변: " + answer);
        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        assertThat(memoryAdvisor.getOrder()).isGreaterThan(ToolCallingAdvisor.DEFAULT_ORDER);
        assertThat(calls).containsExactly("before", "after", "before", "after");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
        assertThat(MODEL.chat().getRequests().getFirst().path("messages").get(0).path("role").asString()).isEqualTo("system");
        verify(teaPriceTools).getTeaPrice("허브티");
        assertThat(messages).extracting(message -> message.getMessageType()).containsExactly(MessageType.USER, MessageType.ASSISTANT, MessageType.TOOL, MessageType.ASSISTANT);
        assertThat(messages.get(0).getText()).isEqualTo("허브티 가격은 얼마인가요?");
        var toolCall = ((AssistantMessage) messages.get(1)).getToolCalls().getFirst();
        var toolResult = ((ToolResponseMessage) messages.get(2)).getResponses().getFirst();
        assertThat(toolCall.id()).isEqualTo("price-1");
        assertThat(toolCall.name()).isEqualTo("getTeaPrice");
        assertThat(toolCall.arguments()).contains("productName", "허브티");
        assertThat(toolResult.id()).isEqualTo(toolCall.id());
        assertThat(toolResult.responseData()).contains("12000");
        assertThat(messages.get(3).getText()).isEqualTo(answer);
        printAndVerifyToolResultSentToModel();
    }

    private void printAndVerifyToolResultSentToModel() {
        var secondRequest = MODEL.chat().getRequests().get(1);
        var toolMessage = secondRequest
                .path("messages")
                .valueStream()
                .filter(message -> message.path("role").asString().equals("tool"))
                .findFirst()
                .orElseThrow();
        String callId = toolMessage.path("tool_call_id").asString();
        String content = toolMessage.path("content").asString();
        System.out.printf("두 번째 모델 요청의 도구 메시지: role=tool, tool_call_id=%s, content=%s%n", callId, content);
        assertThat(callId).isEqualTo("price-1");
        assertThat(content).contains("12000");
    }
}

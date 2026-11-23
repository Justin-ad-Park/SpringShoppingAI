package com.example.ailearning.l08advisors.lesson08_memoryorder;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/** 실제 OpenAI를 호출한다. 저장소 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(classes = Lesson08MemoryOrderOpenAiLiveTest.ModelConfiguration.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(180)
class Lesson08MemoryOrderOpenAiLiveTest {
    private static final String CONVERSATION_ID = "memory-order-live";
    private final ChatMemory conversationMemory = MessageWindowChatMemory.builder()
            .maxMessages(20)
            .build();

    private final ChatMemory conversationMemory6 = MessageWindowChatMemory.builder()
            .maxMessages(6)
            .build();


    private final TeaPriceTools teaPriceTools = spy(new TeaPriceTools());

    @Autowired
    ChatModel chatModel;

    @TestConfiguration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class ModelConfiguration {}

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 기본생성시_orderNo가_도구호출loop의_바깥에서_처리되어서_최초요청_최종응답_메시지만_저장된다() {
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
                .build());

        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");

        System.out.println("최종 답변: " + answer);
        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        verify(teaPriceTools, atLeastOnce()).getTeaPrice(any());
        assertThat(answer).isNotBlank();
        // 기본값은 Ordered.HIGHEST_PRECEDENCE + 200이다.
        // ToolCallingAdvisor.DEFAULT_ORDER(= Ordered.HIGHEST_PRECEDENCE + 300)보다 작다.
        // 도구 반복 앞에 배치되므로 정상 처리 시 사용자 요청 시작에 before 한 번,
        // 최종 응답이 돌아온 뒤 after 한 번만 실행된다. 내부 모델 요청 횟수와는 무관하다.
        assertThat(memoryAdvisor.getOrder()).isEqualTo(Advisor.DEFAULT_CHAT_MEMORY_PRECEDENCE_ORDER);
        assertThat(calls).containsExactly("before", "after");
        assertThat(messages).extracting(message -> message.getMessageType()).containsExactly(MessageType.USER, MessageType.ASSISTANT);
        assertThat(messages.getLast().getText()).isEqualTo(answer);
        assertThat(((AssistantMessage) messages.getLast()).getToolCalls()).isEmpty();
    }

    @Test
    void 도구호출Loop_안에_배치하면_도구_요청과_결과도_메모리에_남는다() {
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
                // 설정값은 Ordered.HIGHEST_PRECEDENCE + 400(-2147483248)이다.
                // ToolCallingAdvisor.DEFAULT_ORDER(= Ordered.HIGHEST_PRECEDENCE + 300)보다 커서 반복 안에 배치된다.
                // 각 모델 요청 전에 before, 정상 모델 응답 뒤에 after가 실행된다.
                // 모델을 두 번 요청하면 각각 두 번 실행되며, 첫 after는 도구 호출 요청 응답 뒤에도 실행된다.
                .order(ToolCallingAdvisor.DEFAULT_ORDER + 100)
                .build());
        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");

        System.out.println("최종 답변: " + answer);
        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        verify(teaPriceTools, atLeastOnce()).getTeaPrice(any());
        assertThat(answer).isNotBlank();
        // 질문 한 번과 도구 결과를 반영하는 모델 재요청에 각각 before·after가 필요하다.
        assertThat(calls.size()).isGreaterThanOrEqualTo(4);
        assertThat(messages).extracting(message -> message.getMessageType()).contains(MessageType.TOOL).doesNotContain(MessageType.SYSTEM);
        assertThat(messages.getLast().getText()).isEqualTo(answer);
    }

    @Test
    void 도구호출Loop_안에_배치_유사_질문_케이스() {
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory)
                // 설정값은 Ordered.HIGHEST_PRECEDENCE + 400(-2147483248)이다.
                // ToolCallingAdvisor.DEFAULT_ORDER(= Ordered.HIGHEST_PRECEDENCE + 300)보다 커서 반복 안에 배치된다.
                // 각 모델 요청 전에 before, 정상 모델 응답 뒤에 after가 실행된다.
                // 모델을 두 번 요청하면 각각 두 번 실행되며, 첫 after는 도구 호출 요청 응답 뒤에도 실행된다.
                .order(ToolCallingAdvisor.DEFAULT_ORDER + 100)
                .build());
        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");
        System.out.println("최종 답변: " + answer);

        String answer2 = service.chat(CONVERSATION_ID, "허브티는 얼마인가요?");
        System.out.println("최종 답변: " + answer2);

        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        verify(teaPriceTools, atLeastOnce()).getTeaPrice("허브티");
        assertThat(answer2).isNotBlank();
        // 사용자 질문 두 번과 적어도 한 번의 도구 결과 재요청으로 모델 요청은 최소 세 번이다.
        // 후속 질문에서 도구를 다시 사용할지는 모델이 판단하므로 정확한 횟수는 고정하지 않는다.
        assertThat(calls.size()).isGreaterThanOrEqualTo(6);
        assertThat(messages).extracting(message -> message.getMessageType()).contains(MessageType.TOOL).doesNotContain(MessageType.SYSTEM);
        assertThat(messages.getLast().getText()).isEqualTo(answer2);
    }

    @Test
    void 도구호출Loop_안에_배치_다른_상품_질문_케이스_최대메시지6개_보관() {
        var memoryAdvisor = spy(MessageChatMemoryAdvisor.builder(conversationMemory6)
                // 설정값은 Ordered.HIGHEST_PRECEDENCE + 400(-2147483248)이다.
                // ToolCallingAdvisor.DEFAULT_ORDER(= Ordered.HIGHEST_PRECEDENCE + 300)보다 커서 반복 안에 배치된다.
                // 각 모델 요청 전에 before, 정상 모델 응답 뒤에 after가 실행된다.
                // 모델을 두 번 요청하면 각각 두 번 실행되며, 첫 after는 도구 호출 요청 응답 뒤에도 실행된다.
                .order(ToolCallingAdvisor.DEFAULT_ORDER + 100)
                .build());
        var service = new Lesson08MemoryOrderChatService(chatModel, memoryAdvisor, teaPriceTools);

        String answer = service.chat(CONVERSATION_ID, "허브티 가격은 얼마인가요?");
        System.out.println("최종 답변: " + answer);

        String answer2 = service.chat(CONVERSATION_ID, "콜라는 얼마인가요?");
        System.out.println("최종 답변: " + answer2);

        var calls = MemoryOrderTestOutput.printAdvisorCalls(memoryAdvisor);
        var messages = conversationMemory6.get(CONVERSATION_ID);
        MemoryOrderTestOutput.printMessages(messages);

        verify(teaPriceTools, atLeastOnce()).getTeaPrice("허브티");
        verify(teaPriceTools, atLeastOnce()).getTeaPrice("콜라");
        assertThat(answer2).isNotBlank();
        // 두 상품의 도구 결과를 반영하므로 모델 요청은 최소 네 번, before·after는 합쳐 최소 여덟 번이다.
        assertThat(calls.size()).isGreaterThanOrEqualTo(8);
        assertThat(messages).hasSizeLessThanOrEqualTo(6);
        assertThat(messages).extracting(message -> message.getMessageType()).contains(MessageType.TOOL).doesNotContain(MessageType.SYSTEM);
        assertThat(messages.getLast().getText()).isEqualTo(answer2);
    }
}

package com.example.ailearning.l08advisors.lesson08_memoryorder;

import java.util.List;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import static org.mockito.Mockito.mockingDetails;

/** 08-8 전용 출력: 실제 Advisor 호출 기록과 ChatMemory에서 꺼낸 메시지를 보여 준다. */
final class MemoryOrderTestOutput {
    private MemoryOrderTestOutput() {}

    /** Spy로 관찰한 before·after의 실제 실행 순서를 출력한다. Advisor를 다시 실행하지 않는다. */
    static List<String> printAdvisorCalls(MessageChatMemoryAdvisor memoryAdvisor) {
        var calls = mockingDetails(memoryAdvisor)
                .getInvocations()
                .stream()
                .map(invocation -> invocation.getMethod().getName())
                .filter(name -> name.equals("before") || name.equals("after"))
                .toList();
        System.out.println("메모리 Advisor order: " + memoryAdvisor.getOrder());
        System.out.println("메모리 Advisor 실행 순서: " + calls);
        return calls;
    }

    /** 역할·본문과 도구 호출 ID·이름·인자·결과를 출력해 빈 assistant 본문만 보이는 일을 피한다. */
    static void printMessages(List<Message> messages) {
        System.out.println("ChatMemory에 저장된 메시지:");
        for (int index = 0; index < messages.size(); index++) {
            Message message = messages.get(index);
            System.out.printf("%d. role=%s, text=%s%n", index + 1,
                    message.getMessageType(), message.getText());
            if (message instanceof AssistantMessage assistantMessage) {
                for (var toolCall : assistantMessage.getToolCalls()) {
                    System.out.printf("   도구 요청: id=%s, name=%s, arguments=%s%n",
                            toolCall.id(), toolCall.name(), toolCall.arguments());
                }
            }
            if (message instanceof ToolResponseMessage toolMessage) {
                for (var response : toolMessage.getResponses()) {
                    System.out.printf("   도구 결과: id=%s, name=%s, result=%s%n",
                            response.id(), response.name(), response.responseData());
                }
            }
        }
    }
}

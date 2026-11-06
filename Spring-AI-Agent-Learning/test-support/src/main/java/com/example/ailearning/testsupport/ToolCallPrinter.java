package com.example.ailearning.testsupport;

import java.util.Arrays;
import java.util.Comparator;
import org.mockito.Mockito;
import org.mockito.invocation.Invocation;

/** 도구로 사용하는 Mockito Mock·Spy의 호출 횟수와 이름·인자를 실행 순서대로 출력한다. */
public final class ToolCallPrinter {
    private ToolCallPrinter() {}

    /** Mockito 기본 형식으로 호출 위치와 스텁 설정을 포함한 상세 내역을 출력한다. */
    public static void printMockitoDetails(Object toolSpy) {
        System.out.println("=== 도구 Mock·Spy 상세 내역 ===");
        System.out.println(Mockito.mockingDetails(toolSpy).printInvocations());
    }

    /** 이미 생성한 Mock·Spy를 전달한다. 도구를 다시 실행하거나 새 Spy를 만들지 않는다. */
    public static void printToolCalls(Object toolSpy) {
        var invocations = Mockito
                .mockingDetails(toolSpy)
                .getInvocations()
                .stream()
                .sorted(Comparator.comparingInt(Invocation::getSequenceNumber))
                .toList();
        System.out.printf("도구 실행 횟수: %d%n", invocations.size());
        int callNumber = 1;
        for (var invocation : invocations) {
            System.out.printf("ToolCall %d: %s, 인자=%s%n",
                    callNumber++, invocation.getMethod().getName(), Arrays.deepToString(invocation.getArguments()));
        }
    }
}

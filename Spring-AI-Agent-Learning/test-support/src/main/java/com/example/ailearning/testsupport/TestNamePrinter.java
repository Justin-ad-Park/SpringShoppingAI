package com.example.ailearning.testsupport;

import org.junit.jupiter.api.TestInfo;

/** 각 테스트 시작 시 출력할 제목 형식을 한 곳에서 정의한다. */
public final class TestNamePrinter {
    private TestNamePrinter() {}

    /** 전체 단원의 @BeforeEach에서 호출하며 @DisplayName 대신 실제 메서드명을 사용한다. */
    public static void printTestName(TestInfo testInfo) {
        String testName = testInfo
                .getTestMethod()
                .orElseThrow()
                .getName();
        System.out.printf("%n===== [ %s ] ======%n", testName);
    }
}

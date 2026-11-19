package com.example.ailearning.l08advisors.chain;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 OpenAI를 호출한다. 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(180)
class Lesson04AdvisorChainOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    Lesson04AdvisorChainChatService service;

    @Test
    void 실제_모델_호출_전후에도_네_Advisor가_정해진_순서로_실행된다() {
        // When: 도구와 메모리 없이 순서만 관찰
        var reply = service.chat("안녕하세요. 어떤 도움을 받을 수 있나요?");
        System.out.printf("답변: %s%n실행 순서: %s%n", reply.answer(), reply.steps());

        // Then: 순서는 Java 체인의 동작이므로 실제 모델에서도 정확히 검사한다.
        assertThat(reply.answer()).isNotBlank();
        assertThat(reply.steps()).containsExactly("DefaultA:before", "DefaultB:before", "AdvisorA:before", "AdvisorB:before",
                        "AdvisorB:after", "AdvisorA:after", "DefaultB:after", "DefaultA:after");
    }
}

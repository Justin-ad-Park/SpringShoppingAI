package com.example.ailearning.l15ollama.lesson05_multiple;

import com.example.ailearning.l15ollama.domain.ShoppingIntent;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class Lesson05CustomerAssessmentLiveTest {
    @Autowired Lesson05CustomerAssessmentService customerAssessmentService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_Ollama가_하나의_메시지에_대한_세_질문에_답한다() {
        var result = customerAssessmentService.assessCustomerMessage("배송이 지연되고 있어요. 오늘 꼭 받아야 하니 사람 상담원에게 연결해 주세요.");
        System.out.println("세 질문의 결과: " + result);
        assertThat(result.intent()).isEqualTo(ShoppingIntent.ORDER_DELIVERY);
        assertThat(result.humanSupportProbability()).isBetween(0.0, 1.0);
        assertThat(result.urgencyScore()).isBetween(0.0, 2.0);
    }
}

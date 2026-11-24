package com.example.ailearning.l15ollama.lesson03_yesno;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class Lesson03HumanSupportRequestLiveTest {
    @Autowired Lesson03HumanSupportRequestService humanSupportRequestService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_Ollama가_상담원_연결_요청과_거절을_구분한다() {
        var requested = humanSupportRequestService.assessHumanSupportRequest("사람 상담원과 직접 이야기하고 싶어요. 연결해 주세요.");
        var declined = humanSupportRequestService.assessHumanSupportRequest("상담원 연결은 원하지 않아요. 자동 안내만 해 주세요.");
        System.out.println("연결 요청: " + requested);
        System.out.println("자동 안내 요청: " + declined);
        assertThat(requested.yesProbability()).isBetween(0.0, 1.0);
        assertThat(declined.yesProbability()).isBetween(0.0, 1.0);
        assertThat(requested.yesProbability()).isGreaterThan(declined.yesProbability());
    }
}

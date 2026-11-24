package com.example.ailearning.l15ollama.lesson04_score;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로컬 Ollama와 다운로드한 의사결정 모델이 필요하다. OpenAI·Jev는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Timeout(180)
class Lesson04SupportPriorityLiveTest {
    @Autowired Lesson04SupportPriorityService supportPriorityService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 실제_Ollama가_일반_문의보다_임박한_마감의_시급성을_높게_평가한다() {
        var routine = supportPriorityService.assessPriority("급한 문제는 없고 텀블러 재질이 궁금해요.");
        var urgent = supportPriorityService.assessPriority("취소 마감이 오늘 한 시간 뒤입니다. 아직 취소가 안 돼 즉시 확인이 필요해요.");
        System.out.println("일반 문의: " + routine);
        System.out.println("마감 임박: " + urgent);
        assertThat(routine.score()).isBetween(0.0, 2.0);
        assertThat(urgent.score()).isBetween(0.0, 2.0);
        assertThat(urgent.score()).isGreaterThan(routine.score());
    }
}

package com.example.ailearning.l13shopping.lesson09_completeness;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** 실제 OpenAI를 사용한다. 저장소 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.timeout=30s")
class Lesson09CompleteCounselOpenAiLiveTest {
    @Autowired Lesson09CompleteCounselService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_OpenAI의_처리_결과를_확인한다() {

        var result = service.answer("배송비 포함 3만원 이하 음료를 추천하고 미개봉 상품 반품 기간도 알려줘.");
        System.out.println("실제 상담 결과와 누락 항목: " + result);
        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.counselResult().offers()).isNotEmpty();
        assertThat(result.counselResult().sources()).isNotEmpty();
    }
}

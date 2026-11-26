package com.example.ailearning.l12evaluation.lesson07_stages;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

/** 실제 OpenAI를 사용한다. 저장소 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.timeout=30s")
class Lesson07RagStageEvaluationOpenAiLiveTest {
    @Autowired Lesson07RagStageEvaluationService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 실제_OpenAI의_처리_결과를_확인한다() {

        var result = service.evaluate("미개봉 상품의 반품 기간은 며칠인가요?", List.of("return-sealed"), "7일");
        System.out.println("실제 검색과 생성의 단계별 결과: " + result);
        assertThat(result.retrievalPassed()).isTrue();
        assertThat(result.generationEvaluated()).isTrue();
        assertThat(result.answer()).isNotBlank();
        // 문구 검사 결과를 출력한다. 자연어 표현의 차이를 사실 오류로 단정하지 않는다.
    }
}

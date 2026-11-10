package com.example.ailearning.l13intent;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출하므로 API 사용량이 발생한다. 분류표 전체의 정확도 평가는 아니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.chat.timeout=30s")
class IntentClassificationOpenAiLiveTest {
    @Autowired
    private IntentClassificationService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(90)
    void 실제_모델로_반품_배송비_문의의_대분류와_소분류를_확인한다() {
        var result = service.classify("미개봉 상품을 반품하려고 합니다. 반품 배송비는 누가 부담하나요?");
        System.out.printf("분류 결과: %s%n", result);

        assertThat(result.decision()).isEqualTo(IntentClassificationService.Decision.MATCH);
        assertThat(result.majorCode()).isEqualTo("RETURN");
        assertThat(result.minorCode()).isEqualTo("RETURN.FEE");
    }
}

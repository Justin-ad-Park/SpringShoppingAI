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
    void 실제_모델로_반품_수거_문의의_대분류와_소분류를_함께_받는다() {
        var result = service.classify("반품을 이미 접수했습니다. 택배 기사님이 상품을 가져가는 날짜는 언제인가요?");
        System.out.printf("분류 결과: %s%n", result);

        assertThat(result.decision()).isEqualTo(IntentClassificationService.Decision.MATCH);
        assertThat(result.mainIntent()).isEqualTo("RETURN");
        assertThat(result.subIntent()).isEqualTo("PICKUP");
    }

    @Test
    @Timeout(90)
    void 실제_모델로_환불_입금_문의의_대분류와_소분류를_함께_받는다() {
        var result = service.classify("반품은 완료됐고 환불금이 계좌에 들어오기까지 며칠 걸리나요?");
        System.out.printf("분류 결과: %s%n", result);

        assertThat(result.decision()).isEqualTo(IntentClassificationService.Decision.MATCH);
        assertThat(result.mainIntent()).isEqualTo("REFUND");
        assertThat(result.subIntent()).isEqualTo("TIMING");
    }
}

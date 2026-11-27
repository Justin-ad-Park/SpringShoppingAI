package com.example.ailearning.l13shopping.lesson10_shoppingandreturn;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** 실제 OpenAI 채팅과 임베딩을 사용한다. 저장소 루트의 .env가 필요하다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.openai.timeout=30s")
class Lesson10ShoppingAndReturnOpenAiLiveTest {
    @Autowired
    Lesson10ShoppingAndReturnService service;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    @Timeout(180)
    void 실제_OpenAI로_한_질문에서_추천과_반품_안내를_함께_얻는다() {
        var result = service.answer("3만원 이하 음료를 추천하고 미개봉 반품 기간도 알려줘.");

        System.out.println(result.answer());
        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.recommendation().product().category()).isEqualTo("음료");
        assertThat(result.recommendation().total()).isLessThanOrEqualTo(30000);
        assertThat(result.returnPolicy().id()).isEqualTo("return-sealed");
        assertThat(result.answer()).contains("수령 후 7일 이내");
    }
}

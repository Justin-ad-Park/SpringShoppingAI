package com.example.ailearning.l04structured;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s"
})
class StructuredOutputOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired CriteriaService service;

    @Test
    @Timeout(180)
    void 실제_OpenAI_응답을_문자열과_record로_받고_검증한다() {
        String question = "음료 중 15000원 이하 선물을 찾아줘.";
        String text = service.text(question);
        System.out.printf("문자열 응답: %s%n", text);

        SearchCriteria criteria = service.extract(question);
        System.out.printf("검증된 조건: %s%n", criteria);

        assertThat(text).isNotBlank();
        assertThat(criteria.category()).isEqualTo("음료");
        assertThat(criteria.maxPrice()).isEqualTo(15000);
    }
}

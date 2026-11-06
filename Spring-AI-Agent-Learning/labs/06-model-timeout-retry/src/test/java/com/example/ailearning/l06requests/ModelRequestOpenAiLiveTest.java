package com.example.ailearning.l06requests;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 OpenAI를 호출한다. 루트 .env를 사용하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ModelRequestOpenAiLiveTest {
    @Autowired ModelRequestService modelRequestService;

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    @Timeout(180)
    void 타임아웃과_재시도가_설정된_클라이언트로_실제_OpenAI에_질문한다() {
        String answer = modelRequestService.ask("짧은 한국어 인사를 해줘.");
        System.out.println("실제 모델 답변: " + answer);
        assertThat(answer).isNotBlank();
    }
}

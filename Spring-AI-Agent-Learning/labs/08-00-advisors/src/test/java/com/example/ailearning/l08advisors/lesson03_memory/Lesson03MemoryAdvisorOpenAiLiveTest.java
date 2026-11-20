package com.example.ailearning.l08advisors.lesson03_memory;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.*;

/** 실제 OpenAI를 호출한다. 루트 .env가 필요하며 API 사용량이 발생한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.ai.openai.timeout=30s")
@Timeout(180)
class Lesson03MemoryAdvisorOpenAiLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired
    Lesson03MemoryAdvisorChatService service;

    // Spy는 실제 모델 호출을 그대로 실행하며 전달한 Prompt만 기록한다. 모델 응답을 대체하지 않는다.
    @MockitoSpyBean
    ChatModel chatModel;

    @AfterEach
    void 테스트_대화_삭제() {
        service.clear("live-user", "budget");
    }

    @Test
    void 실제_모델의_후속_요청에_이전_질문과_실제_답변을_함께_전달한다() {
        // Given: 이전 대화가 없는 상태에서 첫 대화 실행
        service.clear("live-user", "budget");
        String firstQuestion = "친구 선물 예산은 3만원입니다.";
        System.out.println("질문1:" + firstQuestion);
        var firstReply = service.chat("live-user", "budget", firstQuestion);

        // When
        String followUpQuestion = "제가 말한 선물 예산은 얼마인가요?";
        System.out.println("질문2:" + followUpQuestion);
        var followUpReply = service.chat("live-user", "budget", followUpQuestion);
        System.out.printf("첫 답변: %s%n후속 답변: %s%n저장 메시지 수: %d%n",
                firstReply.answer(), followUpReply.answer(), followUpReply.storedMessages());

        // Then: 모델의 표현을 고정하지 않고, 실제 후속 호출의 입력 메시지를 확인한다.
        var prompts = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel, times(2)).call(prompts.capture());
        var sentTexts = prompts.getAllValues().get(1).getInstructions().stream()
                .map(message -> message.getText())
                .toList();
        System.out.printf("두 번째 모델 요청의 메시지: %s%n", sentTexts);
        assertThat(sentTexts).containsSubsequence(firstQuestion, firstReply.answer(), followUpQuestion);
        assertThat(firstReply.answer()).isNotBlank();
        assertThat(followUpReply.answer()).isNotBlank();
        assertThat(firstReply.storedMessages()).isEqualTo(2);
        assertThat(followUpReply.storedMessages()).isEqualTo(4);
    }
}

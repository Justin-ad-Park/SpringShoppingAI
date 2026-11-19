package com.example.ailearning.l03prompts;

import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;

/** 03장 3.3: 기본 시스템 메시지와 요청별 대체를 로컬 모델 요청으로 확인한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.import=",
        "spring.ai.openai.api-key=local-test-key",
        "spring.ai.openai.chat.options.model=tutor-test-model"
})
class SystemMessageDefaultsLocalModelTest {
    private static final OpenAiStubServer MODEL_SERVER = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @Autowired
    private ChatClient.Builder chatClientBuilder;

    @DynamicPropertySource
    static void modelProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL_SERVER::getBaseUrl);
    }

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @AfterAll
    static void 로컬_모델_서버_종료() {
        MODEL_SERVER.close();
    }

    // 03장: 같은 ChatClient로 기본값 사용 → 요청별 대체 → 기본값 유지 여부를 확인한다.
    @Test
    void defaultSystem은_기본값을_제공하고_system은_현재_요청만_대체한다() {
        MODEL_SERVER
                .chat()
                .setResponseText("로컬 테스트 모델의 고정 응답입니다.");

        ChatClient chatClient = chatClientBuilder
                .defaultSystem("쉬운 영어로 답하세요.")
                .build();

        chatClient
                .prompt()
                .user("Spring AI가 뭐야?")
                .call()
                .content();
        chatClient
                .prompt()
                .system("쉬운 한국어로 답하세요.")
                .user("Spring AI가 뭐야?")
                .call()
                .content();
        chatClient
                .prompt()
                .user("Spring AI가 뭐야?")
                .call()
                .content();

        var requests = MODEL_SERVER.chat().getRequests();
        assertThat(requests).hasSize(3);
        for (int index = 0; index < requests.size(); index++) {
            JsonNode messages = requests.get(index).path("messages");
            output.printValue((index + 1) + "번째 요청 메시지", messages);
            assertThat(messages.size()).isEqualTo(2);
            assertThat(messages.get(0).path("role").asText()).isEqualTo("system");
            assertThat(messages.get(1).path("role").asText()).isEqualTo("user");
        }
        assertThat(requests.get(0).at("/messages/0/content").asText()).isEqualTo("쉬운 영어로 답하세요.");
        assertThat(requests.get(1).at("/messages/0/content").asText()).isEqualTo("쉬운 한국어로 답하세요.");
        assertThat(requests.get(2).at("/messages/0/content").asText()).isEqualTo("쉬운 영어로 답하세요.");
    }

}

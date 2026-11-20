package com.example.ailearning.l08advisors.lesson03_memory;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** 08-3의 전용 로컬 테스트. 실습 앱과 Spring AI를 실행하고 모델 HTTP 응답만 고정한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.config.import=")
@ActiveProfiles("test")
class Lesson03MemoryAdvisorLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @Value("${local.server.port}")
    int port;

    @RegisterExtension
    final LocalTestHttpClient appClient = new LocalTestHttpClient(() -> this.port, output);

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL
                .chat()
                .resetResponsesAndRequests();
        MODEL
                .chat()
                .setResponseText("고정 응답");
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 후속_요청에_이전_대화를_전달하고_질문과_답변을_한번씩_저장한다() throws Exception {
        try (var userBrowser = appClient.newSession()) {
            // Given: 같은 사용자와 대화 ID로 첫 대화 저장
            userBrowser.postText("/api/advisors/memory/budget", "선물 예산은 3만원입니다.");

            // When
            var response = userBrowser.postText("/api/advisors/memory/budget", "제가 말한 예산은 얼마인가요?");

            // Then: 저장 개수만 보지 않고 실제 두 번째 모델 요청의 메시지도 검사
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(OpenAiStubServer.JSON.readTree(response.body()).path("storedMessages").asInt()).isEqualTo(4);
            var messages = MODEL.chat().getRequests().get(1).path("messages");
            assertThat(messages.size()).isEqualTo(4); // system + 이전 user/assistant + 현재 user
            assertThat(messages.get(1).path("content").asString()).isEqualTo("선물 예산은 3만원입니다.");
            assertThat(messages.get(2).path("content").asString()).isEqualTo("고정 응답");
            assertThat(messages.get(3).path("content").asString()).isEqualTo("제가 말한 예산은 얼마인가요?");
        }
    }

    @Test
    void 모델_실패_후에도_기본_메모리_Advisor가_먼저_저장한_질문은_남는다() throws Exception {
        try (var userBrowser = appClient.newSession()) {
            // Given
            MODEL
                    .chat()
                    .setHttpStatus(503);

            // When
            var failed = userBrowser.postText("/api/advisors/memory/failure", "실패한 질문의 예산은 3만원");

            // Then
            assertThat(failed.statusCode()).isEqualTo(500);

            // Given: 다음 모델 요청은 성공하도록 복구
            MODEL
                    .chat()
                    .setHttpStatus(200);

            // When
            var next = userBrowser.postText("/api/advisors/memory/failure", "이전 질문이 남아 있나요?");

            // Then: 실패한 user + 다음 user/assistant. 실패한 응답의 assistant는 없다.
            assertThat(next.statusCode()).isEqualTo(200);
            assertThat(OpenAiStubServer.JSON.readTree(next.body()).path("storedMessages").asInt()).isEqualTo(3);
            assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                    .contains("실패한 질문의 예산은 3만원", "이전 질문이 남아 있나요?");
        }
    }

    @Test
    void 대화_ID가_같아도_서로_다른_사용자의_이전_대화는_섞이지_않는다() throws Exception {
        try (var userABrowser = appClient.newSession(); var userBBrowser = appClient.newSession()) {
            // Given
            userABrowser.postText("/api/advisors/memory/demo", "사용자 A의 예산은 3만원");

            // When
            var response = userBBrowser.postText("/api/advisors/memory/demo", "사용자 B의 예산은 1만원");

            // Then
            assertThat(OpenAiStubServer.JSON.readTree(response.body()).path("storedMessages").asInt()).isEqualTo(2);
            assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                    .contains("사용자 B").doesNotContain("사용자 A");
        }
    }

    @Test
    void 삭제한_사용자의_대화만_초기화하고_다른_대화는_유지한다() throws Exception {
        try (var userABrowser = appClient.newSession(); var userBBrowser = appClient.newSession()) {
            // Given: 사용자 A의 두 대화와 사용자 B의 대화 준비
            userABrowser.postText("/api/advisors/memory/one", "사용자 A의 첫 대화");
            userABrowser.postText("/api/advisors/memory/two", "사용자 A의 다른 대화");
            userBBrowser.postText("/api/advisors/memory/one", "사용자 B의 첫 대화");

            // When
            userABrowser.delete("/api/advisors/memory/one");

            // Then: 삭제한 대화는 새로 시작
            var cleared = userABrowser.postText("/api/advisors/memory/one", "새 질문");
            assertThat(OpenAiStubServer.JSON.readTree(cleared.body()).path("storedMessages").asInt()).isEqualTo(2);
            assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).doesNotContain("첫 대화");

            // Then: 같은 사용자의 다른 대화와 다른 사용자의 대화는 보존
            var otherConversation = userABrowser.postText("/api/advisors/memory/two", "후속 질문");
            assertThat(OpenAiStubServer.JSON.readTree(otherConversation.body()).path("storedMessages").asInt()).isEqualTo(4);
            assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("사용자 A의 다른 대화");
            var otherUser = userBBrowser.postText("/api/advisors/memory/one", "후속 질문");
            assertThat(OpenAiStubServer.JSON.readTree(otherUser.body()).path("storedMessages").asInt()).isEqualTo(4);
            assertThat(MODEL.chat().getRequests().getLast().path("messages").toString()).contains("사용자 B의 첫 대화");
        }
    }
}

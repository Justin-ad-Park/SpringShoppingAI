package com.example.ailearning.l08advisors.lesson08_recording;

import com.example.ailearning.testsupport.http.LocalTestHttpClient;
import com.example.ailearning.testsupport.model.OpenAiStubServer;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import com.example.ailearning.testsupport.output.TestOutput;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** 08-8의 전용 로컬 테스트. 실습 앱과 Spring AI를 실행하고 모델 HTTP 응답만 고정한다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.config.import=")
@ActiveProfiles("test")
class Lesson08RequestTraceLocalModelTest {
    private static final OpenAiStubServer MODEL = new OpenAiStubServer();
    private final TestOutput output = new TestOutput();

    @Autowired
    Lesson08RequestTraceChatService service;

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
    void order가_작은_Advisor부터_진입하고_반대_순서로_반환한다() throws Exception {
        // When: 메모리와 도구가 없는 체인 예제 실행
        var response = appClient.postText("/api/advisors/chain", "안녕하세요.");

        // Then: 다른 Advisor 기능을 섞지 않고 기본·요청별 Advisor의 순서만 확인
        var reply = OpenAiStubServer.JSON.readValue(response.body(), Lesson08RequestTraceChatService.Reply.class);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(reply.steps())
                .containsExactly("DefaultA:before", "DefaultB:before", "AdvisorA:before", "AdvisorB:before",
                        "AdvisorB:after", "AdvisorA:after", "DefaultB:after", "DefaultA:after");
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        assertThat(MODEL.chat().getRequests().getFirst().path("tools").isMissingNode()).isTrue();
    }

    @Test
    void 새_요청에는_이전_질문과_실행_기록을_합산하지_않는다() throws Exception {
        // Given
        appClient.postText("/api/advisors/chain", "첫 번째 질문");

        // When
        var response = appClient.postText("/api/advisors/chain", "두 번째 질문");

        // Then
        var reply = OpenAiStubServer.JSON.readValue(response.body(), Lesson08RequestTraceChatService.Reply.class);
        assertThat(reply.steps()).hasSize(8);
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                .contains("두 번째 질문").doesNotContain("첫 번째 질문");
    }

    @Test
    void 요청에_추가한_Advisor는_다음_기본_호출에_남지_않는다() {
        // Given: 기본 두 개에 요청별 두 개를 추가하여 한 번 실행한다.
        service.chat("첫 질문");

        // When: 같은 ChatClient로 기본 설정만 사용해 다음 질문을 보낸다.
        var reply = service.chatWithDefaults("다음 질문");
        output.printValue("기본 등록만 사용한 실행 순서", reply.steps());

        // Then: AdvisorA/B는 이전 요청에만 적용된다.
        assertThat(reply.steps()).containsExactly(
                "DefaultA:before", "DefaultB:before", "DefaultB:after", "DefaultA:after");
        assertThat(MODEL.chat().getRequests()).hasSize(2);
    }

    @Test
    void 공백_질문은_모델을_호출하기_전에_거부한다() throws Exception {
        // When
        var response = appClient.postText("/api/advisors/chain", "   ");

        // Then
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(MODEL.chat().getRequests()).isEmpty();
    }
}

package com.example.ailearning.l07runtime;

import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.TestNamePrinter;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static com.example.ailearning.testsupport.OpenAiChatStub.createAssistantTextMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실제 Redis Stack과 고정 응답 모델을 사용합니다. 테스트 전에 시작하고 반복 실행 동안 재사용하며, 실습을 마치면 종료합니다. OpenAI API는 호출하지 않습니다.
 * Redis 실행·종료 명령은 저장소 루트 기준입니다. 기존 Docker Desktop을 먼저 실행합니다.
 * <pre>
 * bash Spring-AI-Agent-Learning/labs/07-07-runtime-memory/runShell/start-redis.sh
 * bash Spring-AI-Agent-Learning/labs/07-07-runtime-memory/runShell/stop-redis.sh
 * </pre>
 * 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 자세한 사용법은 runShell/README.md를 참고합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Timeout(30)
class IntentMemoryRedisLiveTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    static final String SMALL_TALK = "{\"intent\":\"SMALL_TALK\"}";
    static final String PRODUCT_SEARCH = "{\"intent\":\"PRODUCT_SEARCH\"}";
    final String conversationId = UUID.randomUUID().toString();
    @Autowired ConversationService service;
    @Autowired ChatMemorySelector memorySelector;

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
    }

    @AfterEach
    void 테스트_대화만_정리() {
        for (MemoryStorage storage : MemoryStorage.values()) {
            memorySelector.select(storage).clear(conversationId);
        }
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 같은_실행에서_의도별_저장소를_선택하고_각각의_대화를_이어간다() {
        respond(SMALL_TALK, "반갑습니다.", PRODUCT_SEARCH, "허브티는 12000원입니다.",
                SMALL_TALK, "좋은 하루 보내세요.", PRODUCT_SEARCH, "커피는 28000원입니다.");

        assertThat(ask("안녕하세요.").storage()).isEqualTo(MemoryStorage.IN_MEMORY);
        assertThat(ask("3만원 안에서 음료 선물을 추천해 주세요.").storage()).isEqualTo(MemoryStorage.REDIS);
        String firstProductRequest = MODEL.chat().getRequests().get(3).path("messages").toString();
        assertThat(firstProductRequest).contains("허브티", "12000").doesNotContain("반갑습니다.");

        assertThat(ask("오늘 기분이 좋아요.").storage()).isEqualTo(MemoryStorage.IN_MEMORY);
        String secondSmallTalkRequest = MODEL.chat().getRequests().get(5).path("messages").toString();
        assertThat(secondSmallTalkRequest).contains("안녕하세요.", "반갑습니다.")
                .doesNotContain("3만원 안에서 음료 선물");

        assertThat(ask("음료 선물 추천 예산을 4만원으로 바꿔 주세요.").storage()).isEqualTo(MemoryStorage.REDIS);
        String secondProductRequest = MODEL.chat().getRequests().get(7).path("messages").toString();
        assertThat(secondProductRequest).contains("3만원 안에서 음료 선물", "허브티는 12000원입니다.")
                .doesNotContain("오늘 기분이 좋아요.");
        assertThat(memorySelector.select(MemoryStorage.IN_MEMORY).get(conversationId)).hasSize(4);
        assertThat(memorySelector.select(MemoryStorage.REDIS).get(conversationId)).hasSize(4);
    }

    @Test
    void 의도를_확인하지_못하면_어느_메모리에도_저장하지_않는다() {
        respond("{\"intent\":null}");

        assertThatThrownBy(() -> service.chat(conversationId, "안녕하세요.")).isInstanceOf(IllegalStateException.class);
        assertThat(MODEL.chat().getRequests()).hasSize(1);
        for (MemoryStorage storage : MemoryStorage.values()) {
            assertThat(memorySelector.select(storage).get(conversationId)).isEmpty();
        }
    }

    @Test
    void 다른_의도의_응답이_비어도_기존_대화는_유지하고_실패한_질문은_저장하지_않는다() {
        respond(SMALL_TALK, "반갑습니다.");
        ask("안녕하세요.");
        respond(PRODUCT_SEARCH, "");

        assertThatThrownBy(() -> service.chat(conversationId, "음료를 추천해 주세요."))
                .isInstanceOf(IllegalStateException.class);
        assertThat(memorySelector.select(MemoryStorage.REDIS).get(conversationId)).isEmpty();
        assertThat(memorySelector.select(MemoryStorage.IN_MEMORY).get(conversationId))
                .extracting(message -> message.getText()).containsExactly("안녕하세요.", "반갑습니다.");
    }

    private ConversationService.Reply ask(String question) {
        ConversationService.Reply reply = service.chat(conversationId, question);
        System.out.println("질문: " + question + " / 결과: " + reply);
        assertThat(reply.answer()).isNotBlank();
        return reply;
    }

    private void respond(String... texts) {
        MODEL.chat().setResponseSequence(Arrays.stream(texts)
                .map(text -> createAssistantTextMessage(text))
                .toArray(tools.jackson.databind.node.ObjectNode[]::new));
    }
}

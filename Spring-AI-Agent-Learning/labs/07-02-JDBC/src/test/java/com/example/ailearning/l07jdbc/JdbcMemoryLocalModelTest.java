package com.example.ailearning.l07jdbc;

import com.example.ailearning.testsupport.OpenAiStubServer;
import com.example.ailearning.testsupport.TestNamePrinter;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class JdbcMemoryLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    final String conversationId = UUID.randomUUID().toString();
    @Autowired MemoryChatService service;
    @Autowired ChatMemoryRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired MemoryController controller;

    @DynamicPropertySource
    static void modelAddress(DynamicPropertyRegistry registry) {
        // 실행 시 결정되는 모델 대역 주소만 Java에서 설정합니다. 고정 설정은 application-test.yml에 둡니다.
        registry.add("spring.ai.openai.base-url", MODEL::getBaseUrl);
    }

    @BeforeEach
    void 테스트_준비(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
        MODEL.chat().resetResponsesAndRequests();
    }

    @AfterEach
    void 테스트_대화_정리() {
        service.clear(conversationId);
    }

    @AfterAll
    static void 모델_대역_종료() {
        MODEL.close();
    }

    @Test
    void 선택한_저장소의_이전_대화를_다음_모델_요청에_전달한다() {
        MODEL.chat().setResponseText("예산을 확인했습니다.");
        service.chat(conversationId, "예산은 3만원이에요.");
        MODEL.chat().setResponseText("앞서 말씀하신 예산은 3만원입니다.");
        String answer = service.chat(conversationId, "내 예산이 얼마인가요?");

        assertThat(answer).isNotBlank();
        assertThat(service.history(conversationId)).hasSize(4);
        assertThat(MODEL.chat().getRequests().getLast().path("messages").toString())
                .contains("예산은 3만원이에요.", "예산을 확인했습니다.", "내 예산이 얼마인가요?");
        System.out.println("저장된 대화: " + service.history(conversationId));
    }

    @Test
    void 모델_실패_시_기존_대화를_유지하고_실패한_질문은_저장하지_않는다() {
        MODEL.chat().setResponseText("예산을 확인했습니다.");
        service.chat(conversationId, "예산은 3만원이에요.");
        MODEL.chat().setHttpStatus(500);

        assertThatThrownBy(() -> service.chat(conversationId, "예산을 바꿀게요.")).isInstanceOf(RuntimeException.class);
        assertThat(service.history(conversationId)).extracting(message -> message.getText())
                .containsExactly("예산은 3만원이에요.", "예산을 확인했습니다.");
    }

    @Test
    void JDBC에_저장한_대화는_새_메모리_객체에서도_조회된다() {
        MODEL.chat().setResponseText("예산을 확인했습니다.");
        service.chat(conversationId, "예산은 3만원이에요.");

        var reconnectedMemory = MessageWindowChatMemory.builder()
                .chatMemoryRepository(repository)
                .maxMessages(6)
                .build();
        assertThat(repository).isInstanceOf(JdbcChatMemoryRepository.class);
        assertThat(reconnectedMemory.get(conversationId)).extracting(message -> message.getText())
                .containsExactly("예산은 3만원이에요.", "예산을 확인했습니다.");
        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from SPRING_AI_CHAT_MEMORY where conversation_id = ?", Integer.class, conversationId);
        assertThat(rows).isEqualTo(2);
        System.out.println("JDBC 저장 행 수: " + rows);
    }

    @Test
    void 세션과_긴_대화_ID도_JDBC_컬럼_길이에_맞춰_저장한다() {
        var session = new MockHttpSession();
        String longConversationId = "a".repeat(40);
        MODEL.chat().setResponseText("반갑습니다.");
        try {
            controller.chat(longConversationId, "안녕하세요.", session);
            assertThat(controller.history(longConversationId, session)).hasSize(2);
        } finally {
            controller.clear(longConversationId, session);
        }
    }
}

package com.example.ailearning.l07selection;

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
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.RedisClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 이 테스트는 InMemory와 고정 응답 모델을 사용하므로 Redis 실행이 필요 없습니다. 같은 프로젝트의 Redis Live 테스트 실행 시에만 아래 스크립트를 사용합니다.
 * Redis 실행·종료 명령은 저장소 루트 기준입니다. 기존 Docker Desktop을 먼저 실행합니다.
 * <pre>
 * bash Spring-AI-Agent-Learning/labs/07-03-Redis-Selection/runShell/start-redis.sh
 * bash Spring-AI-Agent-Learning/labs/07-03-Redis-Selection/runShell/stop-redis.sh
 * </pre>
 * 종료 스크립트는 컨테이너만 중지하고 데이터 볼륨은 보존합니다. 자세한 사용법은 runShell/README.md를 참고합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class InMemorySelectionLocalModelTest {
    static final OpenAiStubServer MODEL = new OpenAiStubServer();
    final String conversationId = UUID.randomUUID().toString();
    @Autowired MemoryChatService service;
    @Autowired ChatMemoryRepository repository;
    @Autowired ApplicationContext context;

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
    void InMemory_선택에서는_Redis_클라이언트를_생성하지_않는다() {
        assertThat(repository).isInstanceOf(InMemoryChatMemoryRepository.class);
        assertThat(context.getBeansOfType(RedisClient.class)).isEmpty();
        System.out.println("선택된 저장소: " + repository.getClass().getSimpleName());
    }
}

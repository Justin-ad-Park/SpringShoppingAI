package com.example.ailearning.l07repositories;

import com.example.ailearning.testsupport.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.chat.memory.repository.jdbc.autoconfigure.JdbcChatMemoryRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 HSQLDB와 JDBC 자동 구성을 사용한다. 모델 API나 Redis는 호출하지 않는다. */
class JdbcMemoryRepositoryIntegrationTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 자동_구성된_JDBC_저장소의_대화를_새_메모리에서도_읽는다() {
        // Given: JDBC·DataSource 자동 구성과 실제 내장 DB 준비
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        JdbcTemplateAutoConfiguration.class, JdbcChatMemoryRepositoryAutoConfiguration.class))
                .withUserConfiguration(JdbcMemoryConfiguration.class)
                .withPropertyValues("spring.datasource.url=jdbc:hsqldb:mem:learning07-memory",
                        "spring.datasource.username=sa", "spring.datasource.password=",
                        "spring.ai.chat.memory.repository.jdbc.initialize-schema=always")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(JdbcChatMemoryRepository.class);
                    JdbcChatMemoryRepository repository = context.getBean(JdbcChatMemoryRepository.class);
                    ChatMemory memory = context.getBean(ChatMemory.class);

                    // When: 질문·답변 저장 후 같은 DB를 사용하는 메모리를 새로 생성
                    memory.add("customer-a:demo", List.of(
                            new UserMessage("예산은 3만원이야."), new AssistantMessage("예산을 확인했습니다.")));
                    ChatMemory reconnectedMemory = MessageWindowChatMemory.builder()
                            .chatMemoryRepository(repository)
                            .maxMessages(6)
                            .build();

                    // Then: 메모리 객체가 달라도 DB에 저장한 대화를 조회
                    assertThat(reconnectedMemory.get("customer-a:demo"))
                            .extracting(message -> message.getText())
                            .containsExactly("예산은 3만원이야.", "예산을 확인했습니다.");
                    Integer rowCount = context.getBean(JdbcTemplate.class)
                            .queryForObject("select count(*) from SPRING_AI_CHAT_MEMORY", Integer.class);
                    assertThat(rowCount).isEqualTo(2);
                    System.out.printf("자동 구성된 저장소: %s, DB 메시지 행 수: %d%n",
                            repository.getClass().getSimpleName(), rowCount);
                    System.out.println("새 메모리에서 조회: " + reconnectedMemory.get("customer-a:demo"));
                    memory.clear("customer-a:demo");
                });
    }
}

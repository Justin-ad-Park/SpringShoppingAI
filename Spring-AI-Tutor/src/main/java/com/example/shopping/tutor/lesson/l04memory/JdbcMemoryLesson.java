package com.example.shopping.tutor.lesson.l04memory;

import com.example.shopping.tutor.lesson.l04memory.support.LearningDatabaseFactory;

import org.springframework.ai.chat.memory.repository.jdbc.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

/** Repository를 새로 만들어도 같은 DB를 사용하면 저장된 대화가 남는다. */
public final class JdbcMemoryLesson {
    public List<Message> persistAndRestore(String question) {
        var dataSource = LearningDatabaseFactory.create();
        try {
            var first =
                    JdbcChatMemoryRepository.builder()
                            .dataSource(dataSource)
                            .dialect(new H2ChatMemoryRepositoryDialect())
                            .build();
            first.saveAll("demo", List.of(new UserMessage(question)));
            // 같은 DataSource로 새 인스턴스를 만들어 저장소와 메모리 객체의 수명을 비교한다.
            var second =
                    JdbcChatMemoryRepository.builder()
                            .dataSource(dataSource)
                            .dialect(new H2ChatMemoryRepositoryDialect())
                            .build();
            return second.findByConversationId("demo");
        } finally {
            try {
                new JdbcTemplate(dataSource).execute("SHUTDOWN");
            } finally {
                dataSource.destroy();
            }
        }
    }
}

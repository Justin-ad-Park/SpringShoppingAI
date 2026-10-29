package com.example.shopping.tutor.lesson.l04memory.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

/** 매 실행마다 독립 H2를 준비한다. 실제 저장/조회는 JdbcMemoryLesson에서 읽는다. */
public final class LearningDatabaseFactory {
    private LearningDatabaseFactory() {}

    public static DriverManagerDataSource create() {
        var dataSource =
                new DriverManagerDataSource(
                        "jdbc:h2:mem:tutor_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
        new ResourceDatabasePopulator(
                        new ClassPathResource(
                                "org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql"))
                .execute(dataSource);
        return dataSource;
    }
}

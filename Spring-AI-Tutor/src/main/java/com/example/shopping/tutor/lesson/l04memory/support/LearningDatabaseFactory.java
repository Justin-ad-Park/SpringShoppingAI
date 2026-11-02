package com.example.shopping.tutor.lesson.l04memory.support;

import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.util.UUID;

/** 매 실행마다 독립 H2를 준비한다. 스키마 생성부터 조회까지 한 연결을 유지한다.
 * H2 2.4에서 스키마 초기화 연결 종료 후 CHECK 표현식이 닫힌 세션을 참조하는 것을 피한다.
 * suppressClose=true이며 호출자가 작업 종료 시 destroy()한다.
 */
public final class LearningDatabaseFactory {
    private LearningDatabaseFactory() {}

    public static SingleConnectionDataSource create() {
        var dataSource =
                new SingleConnectionDataSource(
                        "jdbc:h2:mem:tutor_" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "", true);
        new ResourceDatabasePopulator(
                        new ClassPathResource(
                                "org/springframework/ai/chat/memory/repository/jdbc/schema-h2.sql"))
                .execute(dataSource);
        return dataSource;
    }
}

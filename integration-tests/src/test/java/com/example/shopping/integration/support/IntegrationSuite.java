package com.example.shopping.integration.support;

import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInstance;

/** 클래스별 서버 환경과 테스트별 쿠키 세션을 준비한다. 테스트 순서에는 의존하지 않는다. */
@Tag("integration")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class IntegrationSuite {
    protected TestEnvironment environment;
    protected ChatClient client;

    protected abstract TestMode defaultMode();

    @BeforeAll
    protected void startEnvironment() throws Exception {
        var mode = TestMode.valueOf(System.getProperty("shopping.test.mode", defaultMode().name()));
        Path root = Path.of(System.getProperty("shopping.root", ".." )).toAbsolutePath().normalize();
        environment = mode.open(root);
    }

    @BeforeEach
    protected void newSession() {
        client = new ChatClient(environment.baseUrl());
    }

    @AfterEach
    protected void closeSession() {
        client.close();
    }

    @AfterAll
    protected void stopEnvironment() throws Exception {
        if (environment != null) {
            environment.close();
        }
    }
}

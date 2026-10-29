package com.example.shopping.integration.support;

import java.nio.file.Path;
import java.util.List;

/** 모드별 준비 전략. 공통 실행기는 mock 여부를 판단하지 않는다. */
public enum TestMode {
    MOCK {
        @Override
        List<String> agentOptions(TestEnvironment environment) {
            return List.of("--spring.profiles.active=mock");
        }
    },
    GPT_STUB {
        @Override
        List<String> agentOptions(TestEnvironment environment) throws Exception {
            var stub = environment.startStub();
            return List.of(
                    "--spring.profiles.active=default",
                    "--spring.ai.openai.base-url=" + stub.baseUrl(),
                    "--spring.ai.openai.api-key=local-stub-key");
        }
    },
    EXTERNAL {
        @Override
        TestEnvironment open(Path root) {
            // 외부 서버는 소유하지 않으므로 테스트 종료 시에도 종료하지 않는다.
            return new TestEnvironment(root, System.getProperty("shopping.baseUrl", "http://127.0.0.1:8080"));
        }

        @Override
        List<String> agentOptions(TestEnvironment environment) {
            throw new UnsupportedOperationException("외부 서버는 실행하지 않습니다.");
        }
    };

    TestEnvironment open(Path root) throws Exception {
        var environment = new TestEnvironment(root, null);
        try {
            environment.startServices(this);
            return environment;
        } catch (Exception | Error failure) {
            try {
                environment.close();
            } catch (Exception cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    abstract List<String> agentOptions(TestEnvironment environment) throws Exception;
}

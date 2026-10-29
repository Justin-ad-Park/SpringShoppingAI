package com.example.shopping.integration.support;

import java.nio.file.Path;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/** mock 테스트에 성공한 서버를 그대로 브라우저에서 확인하고 Enter로 종료한다. */
public final class SmokePreview {
    public static void main(String[] args) throws Exception {
        Path root = Path.of(System.getProperty("shopping.root"));
        try (var environment = TestMode.MOCK.open(root)) {
            System.setProperty("shopping.test.mode", "EXTERNAL");
            System.setProperty("shopping.baseUrl", environment.baseUrl());
            var request = LauncherDiscoveryRequestBuilder.request().selectors(selectClass("com.example.shopping.integration.MockIntegrationTest")).build();
            var summary = new SummaryGeneratingListener();
            LauncherFactory.create().execute(request, summary);
            summary.getSummary().printTo(new java.io.PrintWriter(System.out, true));
            if (summary.getSummary().getTotalFailureCount() > 0) {
                summary.getSummary().printFailuresTo(new java.io.PrintWriter(System.out, true));
                throw new IllegalStateException("스모크 테스트 실패");
            }

            System.out.println("미리보기: " + environment.baseUrl() + " (Enter를 누르면 종료)");
            System.in.read();
        }
    }
}

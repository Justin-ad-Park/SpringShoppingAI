package com.example.ailearning.l11evaluation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

// 실제 OpenAI를 호출합니다. 저장소 루트 .env를 사용하며 API 사용량이 발생합니다.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.ai.openai.base-url=https://api.openai.com/v1",
        "spring.ai.openai.chat.timeout=30s",
        "spring.ai.openai.embedding.timeout=30s"
})
class EvaluationOpenAiLiveTest {
    @Autowired EvaluationRunner service;

    @Test
    @Timeout(180)
    void 실제_OpenAI_답변을_고정_사례로_평가하고_사용량을_출력한다(TestInfo info) {
        EvaluationRunner.Report report = service.run();
        String name = info.getTestMethod().orElseThrow().getName();
        System.out.printf("[%s] 전체 %d개, 통과 %d개, 비율 %.2f%n", name, report.total(), report.passed(), report.passRate());
        for (var result : report.cases()) {
            System.out.printf("[%s] 사례 %s: %s%n", name, result.id(), result);
        }
        assertThat(report.total()).isEqualTo(3);
        // 품질 판정은 보고서로 비교하고, 연결·응답 변환 실패는 테스트 실패로 확인합니다.
        assertThat(report.cases()).allSatisfy(result -> assertThat(result.actual().metrics().failureKind()).isNull());
    }
}

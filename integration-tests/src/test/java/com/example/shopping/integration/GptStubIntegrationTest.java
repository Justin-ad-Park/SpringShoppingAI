package com.example.shopping.integration;

import com.example.shopping.integration.support.IntegrationSuite;
import com.example.shopping.integration.support.TestMode;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static com.example.shopping.integration.support.ChatClient.ids;
import static com.example.shopping.integration.support.ChatClient.tools;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 GPT 연결 코드를 로컬 OpenAI 대역과 연결해 검증한다.
 * 대역의 고정 응답을 처리하는 경로를 검사하며 실제 모델의 언어 이해 품질은 평가하지 않는다.
 */
@Tag("gpt-path")
@DisplayName("로컬 GPT 대역과 실제 MCP 연결 테스트")
class GptStubIntegrationTest extends IntegrationSuite {
    @Override
    protected TestMode defaultMode() {
        return TestMode.GPT_STUB;
    }

    @AfterEach
    void verifyStub() {
        environment.stub().assertHealthy();
    }

    /** 첫 요청으로 예산을 설정한 뒤 후속 요청에서 같은 작업·예산이 유지되는지 확인한다. */
    @Test
    @DisplayName("GPT 의도 해석과 후속 요청에서 작업·예산을 유지한다")
    void intentAndFollowUp() throws Exception {
        long before = environment.stub().count("interpret");
        var conversation = client.conversation();

        var first = conversation.say("무료배송 추천, 예산 3만원");
        assertThat(first.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertThat(ids(first)).hasSize(3);

        var second = conversation.say("음료 카테고리만");
        assertThat(ids(second)).containsExactly("P004", "P005");
        assertThat(second.at("/task/taskId")).isEqualTo(first.at("/task/taskId"));
        assertThat(second.at("/task/constraints/maxPrice").asInt()).isEqualTo(30000);
        assertThat(environment.stub().count("interpret") - before).isEqualTo(2);
    }

    /** 기존 추천에서 새 검색으로 전환하고 GPT 도구 요청·MCP 실행·상품 선택을 확인한다. */
    @Test
    @DisplayName("새 일반 검색은 GPT 도구 호출과 실제 MCP를 거쳐 상품을 선택한다")
    void catalogToolLoop() throws Exception {
        var conversation = client.conversation();
        conversation.say("무료배송 추천, 예산 3만원");
        long interpretations = environment.stub().count("interpret");
        long toolsBefore = environment.stub().count("catalog-tool");
        long selections = environment.stub().count("catalog-selection");

        var result = conversation.say("새로 텀블러 3만원 이하 찾아줘");

        assertThat(ids(result)).containsExactly("P009");
        assertThat(result.at("/products/0/shipping").isNull()).isTrue();
        assertThat(result.path("execution").asText()).isEqualTo("AGENT");
        assertThat(tools(result)).containsExactly("searchProducts");
        assertThat(environment.stub().count("interpret") - interpretations).isEqualTo(1);
        assertThat(environment.stub().count("catalog-tool") - toolsBefore).isEqualTo(1);
        assertThat(environment.stub().count("catalog-selection") - selections).isEqualTo(1);
    }

    /** 대역이 잘못된 ID를 반환하게 해 검증 실패 후 대체 검색이 한 번만 실행되는지 확인한다. */
    @Test
    @DisplayName("GPT가 존재하지 않는 상품을 선택하면 워크플로로 한 번 대체 실행한다")
    void invalidSelectionFallsBack() throws Exception {
        var conversation = client.conversation();
        long interpretations = environment.stub().count("interpret");
        long toolsBefore = environment.stub().count("catalog-tool");
        long selections = environment.stub().count("catalog-selection");

        var result = conversation.say("새로 텀블러 3만원 이하 찾아줘. 잘못된 선택 테스트");

        assertThat(ids(result)).containsExactly("P009");
        assertThat(result.path("execution").asText()).isEqualTo("WORKFLOW_FALLBACK");
        // 에이전트 검색 1회와 대체 검색 1회다. 무한 재시도를 허용하지 않는다.
        assertThat(tools(result)).containsExactly("searchProducts", "searchProducts");
        assertThat(environment.stub().count("interpret") - interpretations).isEqualTo(1);
        assertThat(environment.stub().count("catalog-tool") - toolsBefore).isEqualTo(1);
        assertThat(environment.stub().count("catalog-selection") - selections).isEqualTo(1);
    }
}

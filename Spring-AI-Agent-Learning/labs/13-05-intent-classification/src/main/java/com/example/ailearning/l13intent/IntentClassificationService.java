package com.example.ailearning.l13intent;

import com.example.ailearning.l13intent.domain.IntentCatalog;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * AWS의 의도 트리·출력 예시 기반 분류를 Spring AI 구조화 응답으로 구현한다.
 * 참고: https://aws.amazon.com/blogs/machine-learning/information-extraction-with-llms-using-amazon-sagemaker-jumpstart/
 */
@Service
public class IntentClassificationService {
    private static final Logger log = LoggerFactory.getLogger(IntentClassificationService.class);

    // 모델의 분류 판단이다. MATCH도 의미의 정확성이나 업무 완료를 보장하지 않는다.
    public enum Decision { MATCH, CLARIFY, OUT_OF_SCOPE, MULTIPLE }

    // 한 번의 모델 응답에서 대분류와 소분류를 함께 받는다.
    public record Prediction(Decision decision,
                             @JsonProperty("main_intent") String mainIntent,
                             @JsonProperty("sub_intent") String subIntent) {}

    // API 응답은 모델의 분류에 안내 문구와 분류표 버전을 추가한다.
    public record Result(Decision decision,
                         @JsonProperty("main_intent") String mainIntent,
                         @JsonProperty("sub_intent") String subIntent,
                         String message, String taxonomyVersion) {}

    private final ChatClient client;
    private final IntentCatalog catalog;
    private final String classificationInstruction;

    public IntentClassificationService(ChatClient.Builder builder, IntentCatalog catalog,
                                       ObjectMapper mapper) throws IOException {
        this.client = builder.build();
        this.catalog = catalog;

        // AWS의 json.dumps(INTENTS)처럼 상하위 관계가 있는 전체 목록을 JSON으로 만든다.
        String intentsJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(catalog.intents());
        String template = new ClassPathResource("prompts/intent-classification.txt")
                .getContentAsString(StandardCharsets.UTF_8);
        // JSON 중괄호를 템플릿 문법으로 해석하지 않고 이 자리표시자 하나만 치환한다.
        this.classificationInstruction = template.replace("{intents_json}", intentsJson);
    }

    public Result classify(String question) {
        Prediction prediction;
        try {
            // 전체 의도 트리와 출력 예시는 system, 고객 문의는 user에 넣고 한 번 요청한다.
            prediction = client
                    .prompt()
                    .system(classificationInstruction)
                    .user(question)
                    .call()
                    .entity(Prediction.class);
            validatePrediction(prediction);
        } catch (RuntimeException error) {
            // 문의·응답 원문 대신 실패 위치와 예외 타입을 기록하고 원인 예외를 보존한다.
            log.warn("Intent classification failed: stage=CLASSIFY, errorType={}", error.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "모델 분류 응답을 처리하지 못했습니다.", error);
        }
        return createResult(prediction);
    }

    private void validatePrediction(Prediction prediction) {
        if (prediction == null || prediction.decision() == null) {
            throw new IllegalArgumentException("분류 판단이 없습니다.");
        }
        if (prediction.decision() == Decision.MATCH) {
            // JSON 변환에 성공했어도, 반환한 두 코드가 분류표의 같은 가지에 있어야 한다.
            if (!catalog.containsPair(prediction.mainIntent(), prediction.subIntent())) {
                throw new IllegalArgumentException("분류표에 없는 대분류·소분류 조합입니다.");
            }
        } else if (prediction.mainIntent() != null || prediction.subIntent() != null) {
            throw new IllegalArgumentException("미확정 판단에는 분류 코드를 넣을 수 없습니다.");
        }
    }

    // 안내 문구만 반환한다. 이후 고객 답변 수집과 업무 실행은 호출하는 애플리케이션의 책임이다.
    private Result createResult(Prediction prediction) {
        String message = switch (prediction.decision()) {
            case MATCH -> "분류 후보를 반환합니다.";
            case CLARIFY -> "어떤 도움이 필요한지 조금 더 구체적으로 알려 주세요.";
            case OUT_OF_SCOPE -> "현재 분류표로 처리할 수 없는 문의입니다.";
            case MULTIPLE -> "여러 요청 중 먼저 처리할 내용을 알려 주세요.";
        };
        return new Result(prediction.decision(), prediction.mainIntent(), prediction.subIntent(),
                message, catalog.version());
    }
}

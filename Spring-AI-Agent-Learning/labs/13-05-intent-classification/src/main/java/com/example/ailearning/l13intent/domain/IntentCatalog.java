package com.example.ailearning.l13intent.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** AWS 예제의 main_intent/sub_intents 트리를 쇼핑 업무에 맞춰 표현한다. */
public record IntentCatalog(String version, List<MainIntent> intents) {
    public record MainIntent(@JsonProperty("main_intent") String mainIntent,
                             String description,
                             @JsonProperty("sub_intents") List<SubIntent> subIntents) {}

    public record SubIntent(@JsonProperty("sub_intent") String subIntent, String description) {}

    // FEE처럼 같은 소분류 이름을 여러 업무에서 사용할 수 있으므로 두 코드를 함께 검사한다.
    public boolean containsPair(String mainIntent, String subIntent) {
        return intents.stream()
                .anyMatch(main -> main.mainIntent().equals(mainIntent)
                        && main.subIntents().stream()
                                .anyMatch(sub -> sub.subIntent().equals(subIntent)));
    }
}

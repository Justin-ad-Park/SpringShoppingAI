package com.example.shopping.tutor.lesson.l00setup;

import org.springframework.ai.chat.client.ChatClient;

/** 00: 외부 도구나 메모리 없이 첫 응답을 받는다. */
public final class SetupLesson {
    /** 같은 코드에 STUB 또는 실제 provider의 ChatClient를 주입해 비교한다. */
    public String greet(ChatClient chatClient, String question) {
        return chatClient.prompt().user(question).call().content();
    }

    public record EnvironmentInfo(String mode, String model, String notice) {}

    /** 실제 실행 환경은 모델에게 추측하게 하지 않고 서버 설정에서 읽는다. */
    public EnvironmentInfo inspectMode(String mode, String modelName) {
        if (mode.equals("stub")) {
            return new EnvironmentInfo(mode, "scripted", "준비된 시나리오를 재현하는 STUB입니다.");
        }
        return new EnvironmentInfo(mode, modelName, "실제 API를 호출하는 LIVE입니다. 사용량이 발생합니다.");
    }
}

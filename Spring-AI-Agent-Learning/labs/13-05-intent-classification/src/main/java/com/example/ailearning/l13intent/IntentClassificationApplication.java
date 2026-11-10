package com.example.ailearning.l13intent;

import com.example.ailearning.l13intent.domain.IntentCatalog;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;

@SpringBootApplication
public class IntentClassificationApplication {
    public static void main(String[] args) {
        SpringApplication.run(IntentClassificationApplication.class, args);
    }

    // 시작할 때 JSON 분류표를 한 번 읽어 공유한다. 모델을 학습시키는 작업은 아니다.
    @Bean
    IntentCatalog intentCatalog(ObjectMapper mapper) throws IOException {
        try (var input = new ClassPathResource("intent-catalog.json").getInputStream()) {
            return mapper.readValue(input, IntentCatalog.class);
        }
    }
}

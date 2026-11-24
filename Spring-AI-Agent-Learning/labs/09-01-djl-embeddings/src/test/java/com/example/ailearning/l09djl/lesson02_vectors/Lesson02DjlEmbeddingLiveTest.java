package com.example.ailearning.l09djl.lesson02_vectors;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import java.util.Arrays;

/** 실제 로컬 DJL 모델을 실행한다. 모델 준비가 필요하며 OpenAI·Ollama는 호출하지 않는다. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = "spring.profiles.active=embedding-only")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(180)
class Lesson02DjlEmbeddingLiveTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Autowired Lesson02DjlEmbeddingService embeddingService;

    @Test
    void 실제_로컬_모델로_384차원의_정규화된_벡터를_만든다() {
        float[] vector = embeddingService.embedQuery("배송비는 얼마인가요?");
        double squaredLength = 0;
        for (float value : vector) {
            assertThat(Float.isFinite(value)).isTrue();
            squaredLength += (double) value * value;
        }
        double vectorLength = Math.sqrt(squaredLength);
        System.out.printf("벡터 차원: %d, 앞 8개 값: %s, 벡터 길이: %.6f%n",
                vector.length, Arrays.toString(Arrays.copyOf(vector, 8)), vectorLength);
        assertThat(vector).hasSize(384);
        assertThat(vectorLength).isCloseTo(1.0, within(0.0001));
    }
}

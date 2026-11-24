package com.example.ailearning.l09djl;

import com.example.ailearning.l09djl.lesson02_vectors.Lesson02DjlEmbeddingService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;
import java.nio.file.Path;
import ai.djl.MalformedModelException;
import ai.djl.repository.zoo.ModelNotFoundException;

/** 앱 시작 때 로컬 모델을 한 번 로드하고, 종료 때 네이티브 메모리를 해제한다. */
@Configuration
public class DjlModelConfiguration {
    @Bean(destroyMethod = "close")
    Lesson02DjlEmbeddingService djlEmbeddingService(
            @Value("${learning.embedding.model-path}") String modelPath)
            throws IOException, ModelNotFoundException, MalformedModelException {
        return new Lesson02DjlEmbeddingService(Path.of(modelPath));
    }
}

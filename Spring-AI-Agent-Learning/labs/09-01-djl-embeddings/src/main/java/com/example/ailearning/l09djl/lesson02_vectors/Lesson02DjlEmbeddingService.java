package com.example.ailearning.l09djl.lesson02_vectors;

import ai.djl.Application;
import ai.djl.Device;
import ai.djl.MalformedModelException;
import ai.djl.huggingface.translator.TextEmbeddingTranslatorFactory;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ModelNotFoundException;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 09 보충 1-2: 로컬 E5 모델을 로드하고 질문·문서를 정규화된 벡터로 변환한다. */
public final class Lesson02DjlEmbeddingService implements AutoCloseable {
    private final ZooModel<String, float[]> embeddingModel;

    public Lesson02DjlEmbeddingService(Path modelDirectory)
            throws IOException, ModelNotFoundException, MalformedModelException {
        for (String fileName : new String[]{"multilingual-e5-small-ko-v2.pt", "tokenizer.json"}) {
            if (!Files.isRegularFile(modelDirectory.resolve(fileName))) {
                throw new IOException("모델 파일이 없습니다. modelShell/prepare.sh를 실행하세요: "
                        + modelDirectory.resolve(fileName));
            }
        }
        embeddingModel = Criteria.builder()
                .setTypes(String.class, float[].class)
                .optApplication(Application.NLP.TEXT_EMBEDDING)
                .optEngine("PyTorch")
                .optDevice(Device.cpu())
                .optModelPath(modelDirectory)
                .optModelName("multilingual-e5-small-ko-v2")
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory())
                .optArgument("pooling", "mean")
                .optArgument("normalize", true)
                .optArgument("padding", true)
                .optArgument("truncation", true)
                .optArgument("maxLength", 512)
                .build()
                .loadModel();
    }

    public float[] embedQuery(String text) {
        return predict(E5InputFormatter.formatQuery(text));
    }

    public float[] embedDocument(String text) {
        return predict(E5InputFormatter.formatDocument(text));
    }

    private float[] predict(String formattedText) {
        // Predictor는 스레드 사이에 공유하지 않는다. 모델은 재사용하고 요청별 Predictor는 닫는다.
        try (var predictor = embeddingModel.newPredictor()) {
            return predictor.predict(formattedText);
        } catch (TranslateException error) {
            throw new IllegalStateException("로컬 E5 임베딩 생성에 실패했습니다.", error);
        }
    }

    @Override
    public void close() {
        embeddingModel.close();
    }
}

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
        embeddingModel = Criteria.builder() // 모델 로딩 조건을 설정할 Builder를 만든다.
                .setTypes(String.class, float[].class) // 입력은 문자열, 출력은 실수 배열로 지정한다.
                .optApplication(Application.NLP.TEXT_EMBEDDING) // 텍스트 임베딩 작업에 사용할 모델임을 지정한다.
                .optEngine("PyTorch") // 모델의 연산을 실행할 엔진을 선택한다.
                .optDevice(Device.cpu()) // GPU 대신 CPU에서 실행한다.
                .optModelPath(modelDirectory) // 변환한 모델과 tokenizer.json이 있는 로컬 폴더를 지정한다.
                .optModelName("multilingual-e5-small-ko-v2") // 로드할 .pt 파일의 이름을 확장자 없이 지정한다.
                .optTranslatorFactory(new TextEmbeddingTranslatorFactory()) // 토큰화와 출력 벡터 변환을 담당할 변환기를 준비한다.
                .optArgument("pooling", "mean") // 패딩을 제외한 토큰별 벡터를 평균 내 문장 벡터를 만든다.
                .optArgument("normalize", true) // 문장 벡터의 크기를 1로 맞추는 L2 정규화를 사용한다.
                .optArgument("padding", true) // 입력을 묶어서 처리할 때 토큰 길이를 맞추는 패딩을 사용한다.
                .optArgument("truncation", true) // 최대 입력 길이를 넘는 토큰은 잘라낸다.
                .optArgument("maxLength", 512) // 최대 입력 길이를 문자 수가 아닌 토큰 수로 설정한다.
                .build() // 지금까지 지정한 조건을 Criteria 객체로 만든다.
                .loadModel(); // 조건에 따라 모델을 메모리에 로드하고 ZooModel을 반환한다.
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

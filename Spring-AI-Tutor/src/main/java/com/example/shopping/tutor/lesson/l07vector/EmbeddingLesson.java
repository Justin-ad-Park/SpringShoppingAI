package com.example.shopping.tutor.lesson.l07vector;

import org.springframework.ai.embedding.EmbeddingModel;

import java.util.Map;

/** 07 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class EmbeddingLesson {

    /** 벡터 숫자와 유사도를 확인합니다. STUB 벡터는 교육용 단어군입니다. */
    public Result compareSimilarity(String question, EmbeddingModel embeddingModel) {
        float[] query = embeddingModel.embed(question);
        float[] cup = embeddingModel.embed("따뜻하게 마시는 텀블러");
        float[] shipping = embeddingModel.embed("배송 문의");
        return Result.ok(
                "유사도는 확률이 아닙니다.",
                Map.of(
                        "dimensions",
                        query.length,
                        "cupSimilarity",
                        cosine(query, cup),
                        "shippingSimilarity",
                        cosine(query, shipping)));
    }

    /** 핵심 동작을 분리해 단원 테스트에서 직접 검증한다. */
    public static double cosine(float[] a, float[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("벡터 차원이 다릅니다. 재색인이 필요합니다.");
        double dot = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            aa += a[i] * a[i];
            bb += b[i] * b[i];
        }
        return aa == 0 || bb == 0 ? 0 : dot / Math.sqrt(aa * bb);
    }

    public record Result(String status, String answer, Map<String, Object> data) {
        public static Result ok(String answer, Map<String, Object> data) {
            return new Result("COMPLETED", answer, data);
        }

        public static Result text(String answer) {
            if (answer == null || answer.isBlank())
                throw new IllegalStateException("모델이 빈 응답을 반환했습니다.");
            return ok(answer, Map.of());
        }

        public static Result unsupported(String reason) {
            return new Result("UNSUPPORTED", reason, Map.of());
        }
    }
}

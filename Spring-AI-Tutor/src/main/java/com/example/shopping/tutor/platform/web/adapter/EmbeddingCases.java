package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l07vector.EmbeddingLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.stereotype.Component;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 EmbeddingLesson부터 읽는다. */
@Component
public class EmbeddingCases {
    @TutorCase(
            id = "L07-01",
            title = "임베딩과 cosine 유사도",
            sample = "보온용 컵",
            goal = "벡터 숫자와 유사도를 확인합니다. STUB 벡터는 교육용 단어군입니다.",
            lesson = EmbeddingLesson.class,
            learningMethod = "compareSimilarity")
    public Outcome compareSimilarity(LessonExecution execution) {
        var result =
                new EmbeddingLesson().compareSimilarity(execution.userInput(), execution.embedding);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}

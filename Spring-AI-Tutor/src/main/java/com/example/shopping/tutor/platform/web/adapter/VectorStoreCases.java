package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l07vector.VectorStoreLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.ai.vectorstore.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 VectorStoreLesson부터 읽는다. */
@Component
public class VectorStoreCases {
    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_URL:}")
    private String pgUrl = "";

    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_USER:tutor}")
    private String pgUser = "tutor";

    @org.springframework.beans.factory.annotation.Value("${TUTOR_PGVECTOR_PASSWORD:}")
    private String pgPassword = "";

    @TutorCase(
            id = "L07-02",
            title = "벡터 저장·검색·삭제",
            sample = "따뜻한 차 마실 용품",
            goal = "VectorStore에 저장하고 topK로 검색한 뒤 삭제합니다.",
            lesson = VectorStoreLesson.class,
            learningMethod = "indexAndSearch")
    public Outcome indexAndSearch(LessonExecution execution) {
        var result =
                new VectorStoreLesson()
                        .indexAndSearch(
                                execution.userInput(), execution.embedding, execution.topK());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L07-03",
            title = "메타데이터 필터",
            sample = "생활 카테고리 중 재고 있는 것",
            goal = "의미 검색과 재고·카테고리 필터를 조합합니다.",
            lesson = VectorStoreLesson.class,
            learningMethod = "filterMetadata")
    public Outcome filterMetadata(LessonExecution execution) {
        var result =
                new VectorStoreLesson()
                        .filterMetadata(
                                execution.userInput(), execution.embedding, execution.topK());
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L07-04",
            title = "pgvector 저장소 교체",
            sample = "생활용품을 pgvector에서 검색해줘",
            goal = "선택 DB를 명시적으로 설정하고 동일 VectorStore 계약을 적용합니다.",
            requires = "pgvector",
            lesson = VectorStoreLesson.class,
            learningMethod = "swapStore")
    public synchronized Outcome swapStore(LessonExecution execution) {
        var result =
                new VectorStoreLesson()
                        .swapStore(
                                execution.userInput(),
                                execution.embedding,
                                execution.topK(),
                                pgUrl,
                                pgUser,
                                pgPassword);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}

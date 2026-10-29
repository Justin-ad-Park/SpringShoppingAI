package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l09rag.PolicyRagLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.*;
import org.springframework.stereotype.Component;

import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 PolicyRagLesson부터 읽는다. */
@Component
public class RagCases {

    public List<Document> retrieve(LessonExecution execution, String query) {
        return new PolicyRagLesson()
                .retrieve(execution.experiment, execution.embedding, execution.topK(), query);
    }

    public Outcome answer(LessonExecution execution, String query) {
        var result =
                new PolicyRagLesson()
                        .answer(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit,
                                query);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L09-01",
            title = "수동 RAG 파이프라인",
            sample = "미개봉 상품 반품 조건은?",
            goal = "검색 → 컨텍스트 → 모델 호출의 각 단계를 직접 읽습니다.",
            lesson = PolicyRagLesson.class,
            learningMethod = "answerWithManualContext")
    public Outcome answerWithManualContext(LessonExecution execution) {
        var result =
                new PolicyRagLesson()
                        .answerWithManualContext(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L09-02",
            title = "QuestionAnswerAdvisor",
            sample = "미개봉 상품 반품 조건은?",
            goal = "동일 검색을 내장 RAG Advisor로 구성합니다.",
            lesson = PolicyRagLesson.class,
            learningMethod = "answerWithQaAdvisor")
    public Outcome answerWithQaAdvisor(LessonExecution execution) {
        var result =
                new PolicyRagLesson()
                        .answerWithQaAdvisor(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L09-03",
            title = "근거 없는 질문",
            sample = "해외 배송 관세는?",
            goal = "없는 정책을 지어내지 않고 모델 호출을 생략합니다.",
            lesson = PolicyRagLesson.class,
            learningMethod = "handleMissingEvidence")
    public Outcome handleMissingEvidence(LessonExecution execution) {
        var result =
                new PolicyRagLesson()
                        .handleMissingEvidence(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L09-04",
            title = "몰·버전 필터와 비신뢰 문서",
            sample = "현재 몰의 반품 조건은?",
            goal = "과거 정책과 다른 몰 문서가 컨텍스트에 들어가지 않는지 확인합니다.",
            lesson = PolicyRagLesson.class,
            learningMethod = "filterPolicyAndTreatUntrustedText")
    public Outcome filterPolicyAndTreatUntrustedText(LessonExecution execution) {
        var result =
                new PolicyRagLesson()
                        .filterPolicyAndTreatUntrustedText(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L09-05",
            title = "후속 질문 재작성",
            sample = "미개봉 상품 반품 조건은?",
            goal = "먼저 반품을 질문하고 다음 턴에 ‘그럼 뜯은 건?’을 입력하세요. 원 질문을 보존해 검색어를 재작성합니다.",
            lesson = PolicyRagLesson.class,
            learningMethod = "improveRetrieval")
    public Outcome improveRetrieval(LessonExecution execution) {
        var result =
                new PolicyRagLesson()
                        .improveRetrieval(
                                execution.client(),
                                execution.userInput(),
                                execution.experiment,
                                execution.conversationId,
                                execution.embedding,
                                execution.memory,
                                execution.topK(),
                                execution::emit);
        return new Outcome(result.status(), result.answer(), result.data());
    }
}

package com.example.shopping.tutor.platform.web.adapter;

import com.example.shopping.tutor.lesson.l08etl.DocumentEtlLesson;
import com.example.shopping.tutor.platform.execution.LessonExecution;
import com.example.shopping.tutor.platform.web.Outcome;
import com.example.shopping.tutor.platform.web.TutorCase;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.ai.reader.*;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/** 웹 caseId를 학습 메소드에 연결한다. 학습할 때는 DocumentEtlLesson부터 읽는다. */
@Component
public class EtlCases {
    @TutorCase(
            id = "L08-01",
            title = "정책 TXT·JSON 읽기",
            sample = "학습용 배송·반품 정책 읽기",
            goal = "DocumentReader로 원문과 메타데이터를 추출합니다.",
            lesson = DocumentEtlLesson.class,
            learningMethod = "readPolicyDocuments")
    public Outcome readPolicyDocuments(LessonExecution execution) {
        var result = new DocumentEtlLesson().readPolicyDocuments();
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L08-02",
            title = "청크 분할과 출처",
            sample = "긴 정책을 청크로 나눠줘",
            goal = "TokenTextSplitter 옵션의 실제 의미와 원문 메타데이터 보존을 확인합니다.",
            lesson = DocumentEtlLesson.class,
            learningMethod = "splitAndAttachMetadata")
    public Outcome splitAndAttachMetadata(LessonExecution execution) {
        var result = new DocumentEtlLesson().splitAndAttachMetadata();
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L08-03",
            title = "중복 없는 재적재",
            sample = "같은 정책을 두 번 적재해줘",
            goal = "안정 ID로 중복을 막고 새 색인이 성공한 후 교체합니다.",
            lesson = DocumentEtlLesson.class,
            learningMethod = "reindexIdempotently")
    public Outcome reindexIdempotently(LessonExecution execution) {
        var result = new DocumentEtlLesson().reindexIdempotently(execution.embedding);
        return new Outcome(result.status(), result.answer(), result.data());
    }

    @TutorCase(
            id = "L08-04",
            title = "PDF·HTML 문서 읽기",
            sample = "가상 설명서를 PDF와 HTML로 읽기",
            goal = "로컬 문서의 텍스트와 출처를 추출합니다. OCR은 제공하지 않습니다.",
            lesson = DocumentEtlLesson.class,
            learningMethod = "readPdfAndHtml")
    public Outcome readPdfAndHtml(LessonExecution execution) {
        var result = new DocumentEtlLesson().readPdfAndHtml();
        return new Outcome(result.status(), result.answer(), result.data());
    }
}

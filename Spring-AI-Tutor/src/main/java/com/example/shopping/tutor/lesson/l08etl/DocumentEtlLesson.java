package com.example.shopping.tutor.lesson.l08etl;

import com.example.shopping.tutor.lesson.l08etl.support.DemoManualFiles;
import com.example.shopping.tutor.lesson.l09rag.PolicyVectorIndex;
import com.example.shopping.tutor.lesson.l09rag.support.DocumentDisplay;
import com.example.shopping.tutor.lesson.l09rag.support.PolicyDocumentFixtures;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.reader.*;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.*;

import java.io.*;
import java.util.*;

/** 08 단원: 핵심 API를 직접 호출한다. 웹 실행·세션 확정은 platform에서 담당한다. */
public class DocumentEtlLesson {

    /** DocumentReader로 원문과 메타데이터를 추출합니다. */
    public Result readPolicyDocuments() {
        var docs =
                new ArrayList<>(
                        new TextReader(new ClassPathResource("knowledge/return-policy.txt")).get());
        docs.addAll(new JsonReader(new ClassPathResource("knowledge/shipping.json"), "text").get());
        if (docs.isEmpty() || docs.stream().anyMatch(d -> d.getText().isBlank()))
            throw new IllegalStateException("빈 문서입니다.");
        return Result.ok("로컬 가상 정책을 읽었습니다.", Map.of("documents", DocumentDisplay.views(docs)));
    }

    /** TokenTextSplitter 옵션의 실제 의미와 원문 메타데이터 보존을 확인합니다. */
    public Result splitAndAttachMetadata() {
        var doc =
                PolicyDocumentFixtures.doc(
                        "long-policy",
                        ("학습용 반품 정책입니다. 미개봉 상품의 반품 조건을 확인하세요.\n\n").repeat(40),
                        "TUTOR",
                        true,
                        "return");
        var chunks =
                TokenTextSplitter.builder()
                        .withChunkSize(120)
                        .withMinChunkSizeChars(20)
                        .withMinChunkLengthToEmbed(5)
                        .withMaxNumChunks(100)
                        .build()
                        .apply(List.of(doc));
        return Result.ok(
                "분할해도 source와 version을 유지합니다.",
                Map.of("chunks", DocumentDisplay.views(chunks), "count", chunks.size()));
    }

    /** 안정 ID로 중복을 막고 새 색인이 성공한 후 교체합니다. */
    public Result reindexIdempotently(EmbeddingModel embeddingModel) {
        var store = PolicyVectorIndex.create(embeddingModel, PolicyDocumentFixtures.policies());
        store.add(PolicyDocumentFixtures.policies());
        var next =
                PolicyVectorIndex.create(
                        embeddingModel,
                        List.of(
                                PolicyDocumentFixtures.doc(
                                        "return-v2-0",
                                        "새 가상 정책: 미개봉 반품 7일, 개봉 상담 필요.",
                                        "TUTOR",
                                        true,
                                        "return")));
        // 신규 적재가 성공한 이후에만 active 참조를 바꾼다.
        store = next;
        var result = store.similaritySearch(PolicyVectorIndex.request("반품", 10));
        return Result.ok(
                "새 색인으로 교체했으며 이전 버전은 검색되지 않습니다.",
                Map.of("documents", DocumentDisplay.views(result)));
    }

    /** 로컬 문서의 텍스트와 출처를 추출합니다. OCR은 제공하지 않습니다. */
    public Result readPdfAndHtml() {
        // 파일 준비는 support, 실제 Spring AI DocumentReader 호출은 이곳에서 확인한다.
        var pdfDocuments = new PagePdfDocumentReader(DemoManualFiles.pdf()).get();
        var htmlDocuments = new JsoupDocumentReader(DemoManualFiles.html()).get();
        return Result.ok(
                "PDF 페이지와 HTML 본문을 추출했습니다.",
                Map.of(
                        "pdf",
                        DocumentDisplay.views(pdfDocuments),
                        "html",
                        DocumentDisplay.views(htmlDocuments)));
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

package com.example.shopping.tutor.lesson;

import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.*;
import org.springframework.ai.reader.jsoup.JsoupDocumentReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.core.io.*;
import org.springframework.stereotype.Component;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import com.example.shopping.tutor.support.*;

@Component
public class EtlCases {
    @TutorCase(id="L08-01",title="정책 TXT·JSON 읽기",sample="학습용 배송·반품 정책 읽기",goal="DocumentReader로 원문과 메타데이터를 추출합니다.")
    public Outcome readPolicyDocuments(RunContext c) {
        var docs=new ArrayList<>(new TextReader(new ClassPathResource("knowledge/return-policy.txt")).get());
        docs.addAll(new JsonReader(new ClassPathResource("knowledge/shipping.json"),"text").get());
        if(docs.isEmpty() || docs.stream().anyMatch(d -> d.getText().isBlank())) throw new IllegalStateException("빈 문서입니다.");
        return Outcome.ok("로컬 가상 정책을 읽었습니다.",Map.of("documents",Knowledge.views(docs)));
    }
    @TutorCase(id="L08-02",title="청크 분할과 출처",sample="긴 정책을 청크로 나눠줘",goal="TokenTextSplitter 옵션의 실제 의미와 원문 메타데이터 보존을 확인합니다.")
    public Outcome splitAndAttachMetadata(RunContext c) {
        var doc=Knowledge.doc("long-policy",("학습용 반품 정책입니다. 미개봉 상품의 반품 조건을 확인하세요.\n\n").repeat(40),"TUTOR",true,"return");
        var chunks=TokenTextSplitter.builder().withChunkSize(120).withMinChunkSizeChars(20).withMinChunkLengthToEmbed(5).withMaxNumChunks(100).build().apply(List.of(doc));
        return Outcome.ok("분할해도 source와 version을 유지합니다.",Map.of("chunks",Knowledge.views(chunks),"count",chunks.size()));
    }
    @TutorCase(id="L08-03",title="중복 없는 재적재",sample="같은 정책을 두 번 적재해줘",goal="안정 ID로 중복을 막고 새 색인이 성공한 후 교체합니다.")
    public Outcome reindexIdempotently(RunContext c) {
        var store=Knowledge.store(c.embedding,Knowledge.policies());store.add(Knowledge.policies());
        var next=Knowledge.store(c.embedding,List.of(Knowledge.doc("return-v2-0","새 가상 정책: 미개봉 반품 7일, 개봉 상담 필요.","TUTOR",true,"return")));
        // 신규 적재가 성공한 이후에만 active 참조를 바꾼다.
        store=next;
        var result=store.similaritySearch(Knowledge.request("반품",10));
        return Outcome.ok("새 색인으로 교체했으며 이전 버전은 검색되지 않습니다.",Map.of("documents",Knowledge.views(result)));
    }
    @TutorCase(id="L08-04",title="PDF·HTML 문서 읽기",sample="가상 설명서를 PDF와 HTML로 읽기",goal="로컬 문서의 텍스트와 출처를 추출합니다. OCR은 제공하지 않습니다.")
    public Outcome readPdfAndHtml(RunContext c) {
        try(var pdf=new PDDocument();var bytes=new ByteArrayOutputStream()) {
            var page=new PDPage();pdf.addPage(page);
            try(var content=new PDPageContentStream(pdf,page)) {
                content.beginText();content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                content.newLineAtOffset(50,700);content.showText("Tutor product manual. Hand wash the tumbler.");content.endText();
            }
            pdf.save(bytes);
            var pdfDocs=new PagePdfDocumentReader(new ByteArrayResource(bytes.toByteArray()) { public String getFilename(){return "manual.pdf";} }).get();
            var htmlDocs=new JsoupDocumentReader(new ByteArrayResource("<html><body><h1>가상 텀블러 설명서</h1><p>손으로 세척하세요.</p></body></html>".getBytes(StandardCharsets.UTF_8))).get();
            return Outcome.ok("PDF 페이지와 HTML 본문을 추출했습니다.",Map.of("pdf",Knowledge.views(pdfDocs),"html",Knowledge.views(htmlDocs)));
        } catch(IOException e) { throw new IllegalStateException("학습 문서 생성 실패",e); }
    }
}

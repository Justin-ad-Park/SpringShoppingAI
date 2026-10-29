package com.example.shopping.tutor.lesson.l08etl.support;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.core.io.*;

import java.io.*;
import java.nio.charset.StandardCharsets;

/** reader 실습에서 사용할 작은 로컬 파일을 준비한다. OCR이나 외부 URL 요청은 하지 않는다. */
public final class DemoManualFiles {
    private DemoManualFiles() {}

    public static Resource pdf() {
        try (var pdf = new PDDocument();
                var bytes = new ByteArrayOutputStream()) {
            var page = new PDPage();
            pdf.addPage(page);
            try (var content = new PDPageContentStream(pdf, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText("Tutor product manual. Hand wash the tumbler.");
                content.endText();
            }
            pdf.save(bytes);
            return new ByteArrayResource(bytes.toByteArray()) {
                public String getFilename() {
                    return "manual.pdf";
                }
            };
        } catch (IOException error) {
            throw new IllegalStateException("학습 문서 생성 실패", error);
        }
    }

    public static Resource html() {
        return new ByteArrayResource(
                "<html><body><h1>가상 텀블러 설명서</h1><p>손으로 세척하세요.</p></body></html>"
                        .getBytes(StandardCharsets.UTF_8));
    }
}

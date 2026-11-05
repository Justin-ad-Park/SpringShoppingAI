package com.example.ailearning.l10advisor;

import com.example.ailearning.testsupport.TestOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// 문서 분할 기능은 Spring·임베딩·채팅 모델 없이 실행할 수 있다.
class TokenTextSplitterUnitTest {
    @Test
    void 긴_문서는_여러_조각으로_나눈다(TestInfo testInfo) {
        var splitter = TokenTextSplitter.builder()
                .build();
        var document = new Document("A sealed product may be returned within seven days. ".repeat(500));
        var chunks = splitter.apply(List.of(document));

        var output = new TestOutput(testInfo.getDisplayName());
        output.printValue("문서 조각 수", chunks.size());
        output.printValue("첫 조각 원문", chunks
                .getFirst()
                .getText());
        assertThat(chunks).hasSizeGreaterThan(1);
        assertThat(chunks).allSatisfy(chunk -> assertThat(chunk.getText()).isNotBlank());
    }
}

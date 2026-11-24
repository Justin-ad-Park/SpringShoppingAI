package com.example.ailearning.l16djl.lesson02_vectors;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;


class E5InputFormatterUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 질문과_문서의_입력_용도를_접두어로_구분한다() {
        String query = E5InputFormatter.formatQuery("배송비는 얼마인가요?");
        String document = E5InputFormatter.formatDocument("배송비는 3000원입니다.");
        System.out.println("질문 입력: " + query);
        System.out.println("문서 입력: " + document);
        assertThat(query).isEqualTo("query: 배송비는 얼마인가요?");
        assertThat(document).isEqualTo("passage: 배송비는 3000원입니다.");
    }

    @Test
    void 이미_붙은_접두어는_중복하지_않는다() {
        String input = E5InputFormatter.formatQuery("query: 배송비");
        System.out.println("모델 입력: " + input);
        assertThat(input).isEqualTo("query: 배송비");
        assertThat(E5InputFormatter.formatDocument("passage: 배송비")).isEqualTo("passage: 배송비");
    }

    @Test
    void 비어_있는_임베딩_입력은_거부한다() {
        System.out.println("공백·null 입력에서 IllegalArgumentException을 확인합니다.");
        assertThatThrownBy(() -> E5InputFormatter.formatQuery(" ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> E5InputFormatter.formatDocument(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

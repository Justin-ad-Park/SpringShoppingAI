package com.example.ailearning.l10rag.lesson03_indexing;

import com.example.ailearning.testsupport.output.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import static org.assertj.core.api.Assertions.assertThat;

/** 실제 정책 파일의 분할과 출처를 확인한다. 임베딩·채팅 모델은 호출하지 않는다. */
class Lesson03PolicyLoaderUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 폴더의_정책_파일들을_항목별로_나누고_출처를_유지한다() throws Exception {
        var documents = Lesson03PolicyLoader.loadDocuments();
        documents.forEach(document -> System.out.printf("정책 항목 [%s], 출처=%s%n%s%n%n",
                document.getId(), document.getMetadata().get("source"), document.getText()));
        assertThat(documents).extracting(document -> document.getId()).containsExactlyInAnyOrder("shipping", "delivery-schedule", "return-sealed", "return-opened", "refund", "coupon");
        var deliveryIds = java.util.Set.of("shipping", "delivery-schedule", "coupon");
        assertThat(documents).allSatisfy(document -> {
            String fileName = deliveryIds.contains(document.getId()) ? "delivery-policies.md" : "return-policies.md";
            assertThat(document.getMetadata().get("source")).isEqualTo("policies/" + fileName + "#" + document.getId());
        });
    }

    @Test
    void 반품_조건과_예외는_같은_문서_조각에_남긴다() throws Exception {
        var openedReturnPolicy = Lesson03PolicyLoader.loadDocuments().stream()
                .filter(document -> document.getId().equals("return-opened"))
                .findFirst()
                .orElseThrow();
        System.out.println("개봉 반품 정책 원문: " + openedReturnPolicy.getText());
        assertThat(openedReturnPolicy.getText()).contains("단순 변심으로 반품할 수 없습니다", "불량", "주문과 다른 상품", "주문 번호");
        assertThat(openedReturnPolicy.getText()).doesNotContain("무료배송", "쿠폰", "환불은 원래");
    }
}

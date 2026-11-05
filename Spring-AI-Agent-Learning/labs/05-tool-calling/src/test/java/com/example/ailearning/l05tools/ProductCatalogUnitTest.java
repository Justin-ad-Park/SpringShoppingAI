package com.example.ailearning.l05tools;

import com.example.ailearning.testsupport.TestOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

// Spring이나 모델 서버 없이 ProductCatalog의 검색·검증·가격 계산을 확인한다.
class ProductCatalogUnitTest {
    private final ProductCatalog productCatalog = new ProductCatalog();
    private TestOutput output;

    @BeforeEach
    void 테스트_이름_준비(TestInfo testInfo) {
        output = new TestOutput(testInfo.getDisplayName());
    }

    @Test
    void 검색_조건을_생략하면_전체_상품을_반환한다() {
        var products = productCatalog.search(null, null);

        output.printValue("전체 상품", products);
        assertThat(products).hasSize(3);
    }

    @Test
    void 허용하지_않는_카테고리를_거부한다() {
        Throwable error = catchThrowable(() -> productCatalog.search("전자", 1000));

        output.printValue("카테고리 검증 결과", error);
        assertThat(error).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 음수_가격_상한을_거부한다() {
        Throwable error = catchThrowable(() -> productCatalog.search(null, -1));

        output.printValue("가격 상한 검증 결과", error);
        assertThat(error).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 존재하지_않는_상품_ID를_거부한다() {
        Throwable error = catchThrowable(() -> productCatalog.calculatePriceDetails("invented"));

        output.printValue("상품 ID 검증 결과", error);
        assertThat(error).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 삼만원_이상_상품은_배송비가_무료다() {
        var priceDetails = productCatalog.calculatePriceDetails("P03");

        output.printValue("텀블러 배송비와 총액", priceDetails);
        assertThat(priceDetails.shippingFee()).isZero();
    }
}

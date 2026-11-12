package com.example.ailearning.l06decisions;

import com.example.ailearning.l06decisions.domain.ProductCatalog;
import com.example.ailearning.l06decisions.domain.ProductSearchCondition;
import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;

class BudgetProductToolsUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 상품가가_예산_안이어도_배송비로_초과하면_제외한다() {
        var tools = new BudgetProductTools(new ProductCatalog(), new ProductSearchCondition("음료", 30000));
        var products = tools.findProductsWithinBudget();
        System.out.println("배송비 포함 예산 안의 상품: " + products);
        assertThat(products).extracting(detail -> detail.product().id()).containsExactly("P01");
        assertThat(products.getFirst().total()).isEqualTo(15000);
    }

    /**
     * @CsvSource
     *  같은 테스트를 입력값별로 반복 실행합니다. 각 문자열의 값을 budget, count 순서로 전달합니다.
     *  "15000, 1"은 예산 15000원에 상품 1개, "14999, 0"은 예산 14999원에 상품 0개를 기대합니다.
     *  **/
    @ParameterizedTest
    @CsvSource({"15000, 1", "14999, 0"})
    void 배송비_포함_총액과_예산이_같으면_포함한다(int budget, int count) {
        var tools = new BudgetProductTools(new ProductCatalog(), new ProductSearchCondition("음료", budget));
        var products = tools.findProductsWithinBudget();
        System.out.println("예산 " + budget + "원 조회 결과: " + products);
        assertThat(products).hasSize(count);
    }

    @Test
    void 카테고리를_지정하면_해당_카테고리만_반환한다() {
        var tools = new BudgetProductTools(new ProductCatalog(), new ProductSearchCondition("생활", 40000));
        var products = tools.findProductsWithinBudget();
        assertThat(products).extracting(detail -> detail.product().id()).containsExactly("P03");
        assertThat(products.getFirst().shippingFee()).isZero();
    }

    @Test
    void 카테고리를_생략하면_모든_카테고리에서_조회한다() {
        var tools = new BudgetProductTools(new ProductCatalog(), new ProductSearchCondition(null, 40000));
        assertThat(tools.findProductsWithinBudget()).extracting(detail -> detail.product().id())
                .containsExactly("P01", "P02", "P03");
    }
}

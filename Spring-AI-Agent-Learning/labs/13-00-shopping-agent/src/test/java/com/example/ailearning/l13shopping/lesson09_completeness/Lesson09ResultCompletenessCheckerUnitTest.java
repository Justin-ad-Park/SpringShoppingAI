package com.example.ailearning.l13shopping.lesson09_completeness;

import com.example.ailearning.l13shopping.domain.*;
import com.example.ailearning.l13shopping.policy.PolicySearchResult;
import com.example.ailearning.l13shopping.workflow.CounselResult;
import com.example.ailearning.testsupport.output.TestNamePrinter;
import java.util.List;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class Lesson09ResultCompletenessCheckerUnitTest {
    private final Lesson09ResultCompletenessChecker checker = new Lesson09ResultCompletenessChecker();

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) { TestNamePrinter.printTestName(testInfo); }

    @Test
    void 금액과_ID가_검증돼도_요청한_정책이_빠지면_누락으로_표시한다() {
        var result = checker.check(new Lesson09RequiredResults(true, true), productOnlyResult());
        System.out.println("기본 결과와 추가 누락 검사: " + result);
        assertThat(result.counselResult().status()).isEqualTo("VERIFIED");
        assertThat(result.status()).isEqualTo("MISSING_RESULTS");
        assertThat(result.missingResults()).containsExactly("정책 안내");
    }

    @Test
    void 상품만_요청했다면_정책이_없어도_누락이_아니다() {
        var result = checker.check(new Lesson09RequiredResults(true, false), productOnlyResult());
        System.out.println("상품 단독 요청: " + result);
        assertThat(result.status()).isEqualTo("COMPLETE");
    }

    @Test
    void 요청한_상품과_정책이_모두_있으면_두_결과의_존재_검사를_통과한다() {
        var productResult = productOnlyResult();
        var policy = new PolicySearchResult("return-sealed", "미개봉은 7일 이내 반품", "policy.md#return-sealed", 1.0);
        var baseResult = new CounselResult("VERIFIED", "상품과 정책", productResult.intent(),
                productResult.offers(), List.of(policy));
        var result = checker.check(new Lesson09RequiredResults(true, true), baseResult);
        System.out.println("두 결과가 있는 요청: " + result);
        assertThat(result.status()).isEqualTo("COMPLETE");
        assertThat(result.missingResults()).isEmpty();
    }

    @Test
    void 기본_검증에서_보류한_결과를_완료로_바꾸지_않는다() {
        var baseResult = new CounselResult("NO_RESULTS", "후보 없음", new Intent("SHOPPING", "음료", 1000), List.of(), List.of());
        var result = checker.check(new Lesson09RequiredResults(true, true), baseResult);
        System.out.println("기본 보류 결과: " + result);
        assertThat(result.status()).isEqualTo("BASE_RESULT_NOT_VERIFIED");
        assertThat(result.counselResult().status()).isEqualTo("NO_RESULTS");
    }

    @Test
    void 필요한_결과가_하나도_없는_요청은_완료로_판정하지_않는다() {
        assertThatThrownBy(() -> checker.check(new Lesson09RequiredResults(false, false), productOnlyResult()))
                .isInstanceOf(IllegalArgumentException.class);
        System.out.println("처리 범위 밖 요청은 IllegalArgumentException으로 거부");
    }

    private static CounselResult productOnlyResult() {
        var product = new Product("P01", "허브티", "음료", 12000);
        var priceDetail = new ProductPriceDetail(product, 3000, 15000);
        return new CounselResult("VERIFIED", "허브티 총 15000원", new Intent("SHOPPING", "음료", 30000),
                List.of(priceDetail), List.of());
    }
}

package com.example.ailearning.l13intent.domain;

import com.example.ailearning.l13intent.domain.IntentCatalog.MainIntent;
import com.example.ailearning.l13intent.domain.IntentCatalog.SubIntent;
import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class IntentCatalogUnitTest {
    private final IntentCatalog catalog = new IntentCatalog("test", List.of(
            new MainIntent("RETURN", "반품", List.of(new SubIntent("FEE", "반품 배송비"))),
            new MainIntent("EXCHANGE", "교환", List.of(new SubIntent("FEE", "교환 배송비"))),
            new MainIntent("REFUND", "환불", List.of(new SubIntent("TIMING", "입금 기간")))));

    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @ParameterizedTest
    @CsvSource({
            "RETURN,FEE,true",
            "EXCHANGE,FEE,true",
            "RETURN,TIMING,false",
            "REFUND,FEE,false",
            "INVENTED,FEE,false"
    })
    void 같은_소분류명도_대분류와_한쌍으로_등록되어야_한다(String main, String sub, boolean expected) {
        boolean registered = catalog.containsPair(main, sub);
        System.out.printf("분류 조합 %s:%s 등록 여부: %s%n", main, sub, registered);
        assertThat(registered).isEqualTo(expected);
    }
}

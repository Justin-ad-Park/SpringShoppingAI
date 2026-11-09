package com.example.ailearning.l11mcp.domain;

import com.example.ailearning.testsupport.TestNamePrinter;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;

class SearchConditionsUnitTest {
    @BeforeEach
    void 테스트_이름_출력(TestInfo testInfo) {
        TestNamePrinter.printTestName(testInfo);
    }

    @Test
    void 음수_가격과_지원하지_않는_카테고리를_거부한다() {
        System.out.println("거부할 조건: 상품 상한 -1, 총예산 -1, 카테고리 전자");
        assertThatThrownBy(() -> new SearchConditions("음료", -1, null).validate())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchConditions("음료", 15000, -1L).validate())
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SearchConditions("전자", 15000, null).validate())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 가격_0원과_선택_총예산_생략은_유효한_조건이다() {
        SearchConditions conditions = new SearchConditions("음료", 0, null);
        System.out.println("검증 조건: " + conditions);
        assertThatCode(conditions::validate).doesNotThrowAnyException();
        assertThat(conditions.isComplete()).isTrue();
    }
}

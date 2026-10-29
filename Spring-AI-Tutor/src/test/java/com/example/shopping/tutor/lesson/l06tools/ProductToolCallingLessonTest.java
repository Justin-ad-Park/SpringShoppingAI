package com.example.shopping.tutor.lesson.l06tools;

import static com.example.shopping.tutor.platform.support.LessonExecutionFixture.context;

import static org.assertj.core.api.Assertions.*;

import com.example.shopping.tutor.lesson.l04memory.*;
import com.example.shopping.tutor.lesson.l06tools.support.DemoCartRepository;
import com.example.shopping.tutor.lesson.l06tools.support.Product;
import com.example.shopping.tutor.lesson.l06tools.support.SandboxCart;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingPolicy;
import com.example.shopping.tutor.lesson.l06tools.support.ShippingQuoteCalculator;
import com.example.shopping.tutor.platform.web.adapter.ToolCallingCases;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.ai.chat.memory.*;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.*;
import org.springframework.ai.chat.prompt.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;

/** 단원 핵심 API를 직접 검증하고, 웹 연결 동작도 회귀 확인한다. */
class ProductToolCallingLessonTest {
    @ParameterizedTest
    @CsvSource({"0,0", "49999,3000", "50000,0", "50001,0"})
    void shippingBoundary(long subtotal, long expected) {
        assertThat(ShippingPolicy.shippingFee(subtotal)).isEqualTo(expected);
    }

    @Test
    void quoteChecksStockGroupAndQuantityFacts() {
        assertThat(ShippingQuoteCalculator.quote("standard", List.of("P004")))
                .containsEntry("projectedSubtotal", 51900L)
                .containsEntry("shippingFee", 0L);
        for (String bad : List.of("P006", "P007", "P999"))
            assertThatThrownBy(() -> ShippingQuoteCalculator.quote("standard", List.of(bad)))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThat(DemoCartRepository.subtotal("standard")).isEqualTo(42000);
    }

    @Test
    void trustedContextCannotBeOmitted() {
        assertThatThrownBy(
                        () ->
                                new CartSummaryTools("standard")
                                        .getCartSummary(new ToolContext(Map.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void toolLoopExecutesActualCallbackAndRecordsIt() {
        var c = context("1만5천원 이하 음료 찾아줘");
        new ToolCallingCases(new SandboxCart()).searchProducts(c);
        assertThat(c.toolCalls).hasSize(1);
        assertThat(c.toolCalls.getFirst().get("result").toString())
                .contains("P004", "P005")
                .doesNotContain("P007");
    }

    @Test
    void sandboxRequiresOwnerAndConfirmationAndIsIdempotent() {
        var cart = new SandboxCart();
        var preview = cart.preview("owner", "P004");
        String token = (String) preview.get("confirmationToken");
        assertThatThrownBy(() -> cart.confirm("other", token))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(cart.confirm("owner", token)).containsEntry("P004", 1);
        assertThat(cart.confirm("owner", token)).containsEntry("P004", 1);
        cart.reset("owner");
        assertThatThrownBy(() -> cart.confirm("owner", token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expiredConfirmationCannotChangeCart() {
        var current = new AtomicReference<>(Instant.parse("2026-10-29T00:00:00Z"));
        Clock clock =
                new Clock() {
                    public java.time.ZoneId getZone() {
                        return ZoneOffset.UTC;
                    }

                    public Clock withZone(java.time.ZoneId z) {
                        return this;
                    }

                    public Instant instant() {
                        return current.get();
                    }
                };
        var cart = new SandboxCart(clock);
        String token = (String) cart.preview("a", "P004").get("confirmationToken");
        current.set(current.get().plusSeconds(121));
        assertThatThrownBy(() -> cart.confirm("a", token))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void searchToolHonorsStockAndBudget() {
        assertThat(new ProductSearchTools().searchProducts("음료", 10000))
                .extracting(Product::id)
                .containsExactly("P004");
    }
}

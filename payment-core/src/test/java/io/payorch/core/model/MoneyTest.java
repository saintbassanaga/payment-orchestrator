package io.payorch.core.model;

import io.payorch.core.exception.InvalidPaymentRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void should_create_money_when_amount_and_currency_are_valid() {
        Money money = new Money(new BigDecimal("5000"), "xaf");

        assertThat(money.amount()).isEqualByComparingTo("5000");
        assertThat(money.currency()).isEqualTo("XAF");
    }

    @Test
    void should_normalize_currency_to_uppercase() {
        Money money = Money.of(new BigDecimal("100"), "eur");

        assertThat(money.currency()).isEqualTo("EUR");
    }

    @Test
    void should_throw_when_amount_is_null() {
        assertThatThrownBy(() -> new Money(null, "XAF"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void should_throw_when_currency_is_null() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void should_throw_when_amount_is_zero() {
        assertThatThrownBy(() -> new Money(BigDecimal.ZERO, "XAF"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    void should_throw_when_amount_is_negative() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "XAF"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("strictly positive");
    }

    @Test
    void should_throw_when_currency_is_blank() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "   "))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("ISO 4217");
    }

    @Test
    void should_throw_when_currency_has_wrong_length() {
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "EU"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("ISO 4217");

        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "EURO"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("ISO 4217");
    }

    @Test
    void should_create_from_string_amount() {
        Money money = Money.of("2500.50", "XOF");

        assertThat(money.amount()).isEqualByComparingTo("2500.50");
        assertThat(money.currency()).isEqualTo("XOF");
    }

    @Test
    void should_throw_when_string_amount_is_null() {
        assertThatThrownBy(() -> Money.of((String) null, "XAF"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }
}
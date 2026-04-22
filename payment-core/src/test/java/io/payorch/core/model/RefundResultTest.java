package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundResultTest {

    private static final Money MONEY = Money.of(new BigDecimal("2500"), "XAF");

    @Test
    void should_construct_with_all_fields() {
        RefundResult result = new RefundResult(
                "REFUND-001", "TX-001", PaymentStatus.REFUNDED,
                MONEY, "pawapay", Optional.of("Refund processed"), Instant.now());

        assertThat(result.refundId()).isEqualTo("REFUND-001");
        assertThat(result.originalTransactionId()).isEqualTo("TX-001");
        assertThat(result.status()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(result.amount()).isEqualTo(MONEY);
        assertThat(result.providerName()).isEqualTo("pawapay");
        assertThat(result.providerMessage()).contains("Refund processed");
    }

    @Test
    void should_default_empty_optional_when_provider_message_is_null() {
        RefundResult result = new RefundResult(
                "REFUND-001", "TX-001", PaymentStatus.INITIATED,
                MONEY, "pawapay", null, Instant.now());

        assertThat(result.providerMessage()).isEmpty();
    }

    @Test
    void should_throw_on_null_required_fields() {
        assertThatThrownBy(() -> new RefundResult(
                null, "TX", PaymentStatus.REFUNDED, MONEY, "p", Optional.empty(), Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RefundResult(
                "R", "TX", null, MONEY, "p", Optional.empty(), Instant.now()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RefundResult(
                "R", "TX", PaymentStatus.REFUNDED, null, "p", Optional.empty(), Instant.now()))
                .isInstanceOf(NullPointerException.class);
    }
}

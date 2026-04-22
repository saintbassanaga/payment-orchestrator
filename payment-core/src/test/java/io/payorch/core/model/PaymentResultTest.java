package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentResultTest {

    private static final Money MONEY = Money.of(new BigDecimal("5000"), "XAF");

    @Test
    void should_construct_with_all_fields() {
        PaymentResult result = new PaymentResult(
                "TX-001", "PROV-001", PaymentStatus.SUCCESS,
                MONEY, "pawapay", Optional.of("OK"), Instant.now(), Map.of("k", "v"));

        assertThat(result.transactionId()).isEqualTo("TX-001");
        assertThat(result.providerTransactionId()).isEqualTo("PROV-001");
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount()).isEqualTo(MONEY);
        assertThat(result.providerName()).isEqualTo("pawapay");
        assertThat(result.providerMessage()).contains("OK");
    }

    @Test
    void should_default_empty_optional_when_provider_message_is_null() {
        PaymentResult result = new PaymentResult(
                "TX-001", "PROV-001", PaymentStatus.PENDING,
                MONEY, "pawapay", null, Instant.now(), Map.of());

        assertThat(result.providerMessage()).isEmpty();
    }

    @Test
    void should_make_metadata_immutable() {
        java.util.HashMap<String, String> mutable = new java.util.HashMap<>();
        mutable.put("key", "value");
        PaymentResult result = new PaymentResult(
                "TX-001", "PROV-001", PaymentStatus.PENDING,
                MONEY, "pawapay", Optional.empty(), Instant.now(), mutable);

        assertThatThrownBy(() -> result.metadata().put("extra", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_default_empty_metadata_when_null() {
        PaymentResult result = new PaymentResult(
                "TX-001", "PROV-001", PaymentStatus.PENDING,
                MONEY, "pawapay", Optional.empty(), Instant.now(), null);

        assertThat(result.metadata()).isEmpty();
    }

    @Test
    void should_throw_on_null_required_fields() {
        assertThatThrownBy(() -> new PaymentResult(
                null, "PROV", PaymentStatus.PENDING, MONEY, "p", Optional.empty(), Instant.now(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentResult(
                "TX", null, PaymentStatus.PENDING, MONEY, "p", Optional.empty(), Instant.now(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentResult(
                "TX", "PROV", null, MONEY, "p", Optional.empty(), Instant.now(), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_cover_all_payment_statuses() {
        assertThat(PaymentStatus.values()).containsExactlyInAnyOrder(
                PaymentStatus.INITIATED, PaymentStatus.PENDING, PaymentStatus.SUCCESS,
                PaymentStatus.FAILED, PaymentStatus.CANCELLED, PaymentStatus.EXPIRED,
                PaymentStatus.REFUNDED, PaymentStatus.PARTIAL_REFUND);
    }
}

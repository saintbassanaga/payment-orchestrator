package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentEventTest {

    private static final Money MONEY = Money.of(new BigDecimal("5000"), "XAF");

    @Test
    void should_construct_with_all_fields() {
        Instant now = Instant.now();
        PaymentEvent event = new PaymentEvent(
                "TX-001", "PROV-001", PaymentStatus.SUCCESS,
                MONEY, "pawapay", now, Map.of("k", "v"));

        assertThat(event.transactionId()).isEqualTo("TX-001");
        assertThat(event.providerTransactionId()).isEqualTo("PROV-001");
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(event.amount()).isEqualTo(MONEY);
        assertThat(event.providerName()).isEqualTo("pawapay");
        assertThat(event.occurredAt()).isEqualTo(now);
        assertThat(event.metadata()).containsEntry("k", "v");
    }

    @Test
    void should_make_metadata_immutable() {
        HashMap<String, String> mutable = new HashMap<>();
        mutable.put("key", "value");
        PaymentEvent event = new PaymentEvent(
                "TX-001", "PROV-001", PaymentStatus.SUCCESS,
                MONEY, "pawapay", Instant.now(), mutable);

        assertThatThrownBy(() -> event.metadata().put("extra", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_default_empty_metadata_when_null() {
        PaymentEvent event = new PaymentEvent(
                "TX-001", "PROV-001", PaymentStatus.SUCCESS,
                MONEY, "pawapay", Instant.now(), null);

        assertThat(event.metadata()).isEmpty();
    }

    @Test
    void should_throw_on_null_required_fields() {
        assertThatThrownBy(() -> new PaymentEvent(
                null, "PROV", PaymentStatus.SUCCESS, MONEY, "p", Instant.now(), Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentEvent(
                "TX", "PROV", PaymentStatus.SUCCESS, MONEY, "p", null, Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new PaymentEvent(
                "TX", "PROV", null, MONEY, "p", Instant.now(), Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}

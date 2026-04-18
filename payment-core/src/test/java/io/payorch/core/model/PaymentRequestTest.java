package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestTest {

    private static final Money AMOUNT = Money.of(new BigDecimal("5000"), "XAF");

    @Test
    void should_create_request_with_all_fields() {
        PaymentRequest request = new PaymentRequest(
                "TX-001", AMOUNT, "+237600000000", "Test payment",
                Optional.of("https://example.com/return"), Map.of("orderId", "42")
        );

        assertThat(request.transactionId()).isEqualTo("TX-001");
        assertThat(request.amount()).isEqualTo(AMOUNT);
        assertThat(request.phoneNumber()).isEqualTo("+237600000000");
        assertThat(request.description()).isEqualTo("Test payment");
        assertThat(request.returnUrl()).contains("https://example.com/return");
        assertThat(request.metadata()).containsEntry("orderId", "42");
    }

    @Test
    void should_create_minimal_request_via_factory() {
        PaymentRequest request = PaymentRequest.of("TX-002", AMOUNT, "+237600000000", "desc");

        assertThat(request.returnUrl()).isEmpty();
        assertThat(request.metadata()).isEmpty();
    }

    @Test
    void should_make_metadata_immutable() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("key", "value");
        PaymentRequest request = new PaymentRequest(
                "TX-003", AMOUNT, "+237600000000", "desc", Optional.empty(), mutable
        );

        mutable.put("injected", "evil");

        assertThat(request.metadata()).doesNotContainKey("injected");
    }

    @Test
    void should_use_empty_map_when_metadata_is_null() {
        PaymentRequest request = new PaymentRequest(
                "TX-004", AMOUNT, "+237600000000", "desc", Optional.empty(), null
        );

        assertThat(request.metadata()).isEmpty();
    }

    @Test
    void should_throw_when_transaction_id_is_null() {
        assertThatThrownBy(() -> new PaymentRequest(
                null, AMOUNT, "+237600000000", "desc", Optional.empty(), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("transactionId");
    }

    @Test
    void should_throw_when_amount_is_null() {
        assertThatThrownBy(() -> new PaymentRequest(
                "TX-005", null, "+237600000000", "desc", Optional.empty(), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
    }

    @Test
    void should_throw_when_phone_number_is_null() {
        assertThatThrownBy(() -> new PaymentRequest(
                "TX-006", AMOUNT, null, "desc", Optional.empty(), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("phoneNumber");
    }

    @Test
    void should_throw_when_description_is_null() {
        assertThatThrownBy(() -> new PaymentRequest(
                "TX-007", AMOUNT, "+237600000000", null, Optional.empty(), Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("description");
    }
}

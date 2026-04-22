package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RefundRequestTest {

    private static final Money MONEY = Money.of(new BigDecimal("2500"), "XAF");

    @Test
    void should_construct_with_all_fields() {
        RefundRequest request = new RefundRequest(
                "REFUND-001", "TX-001", MONEY, "Duplicate charge", Map.of("k", "v"));

        assertThat(request.transactionId()).isEqualTo("REFUND-001");
        assertThat(request.originalTransactionId()).isEqualTo("TX-001");
        assertThat(request.amount()).isEqualTo(MONEY);
        assertThat(request.reason()).isEqualTo("Duplicate charge");
        assertThat(request.metadata()).containsEntry("k", "v");
    }

    @Test
    void should_make_metadata_immutable() {
        HashMap<String, String> mutable = new HashMap<>();
        mutable.put("key", "value");
        RefundRequest request = new RefundRequest("R-001", "TX-001", MONEY, "reason", mutable);

        assertThatThrownBy(() -> request.metadata().put("extra", "x"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_default_empty_metadata_when_null() {
        RefundRequest request = new RefundRequest("R-001", "TX-001", MONEY, "reason", null);
        assertThat(request.metadata()).isEmpty();
    }

    @Test
    void should_throw_on_null_required_fields() {
        assertThatThrownBy(() -> new RefundRequest(null, "TX", MONEY, "r", Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RefundRequest("R", null, MONEY, "r", Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RefundRequest("R", "TX", null, "r", Map.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RefundRequest("R", "TX", MONEY, null, Map.of()))
                .isInstanceOf(NullPointerException.class);
    }
}

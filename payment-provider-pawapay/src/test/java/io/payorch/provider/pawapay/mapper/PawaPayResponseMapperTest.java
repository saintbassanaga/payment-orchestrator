package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.RefundResult;
import io.payorch.provider.pawapay.dto.PawaPayDepositResponse;
import io.payorch.provider.pawapay.dto.PawaPayRefundResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PawaPayResponseMapperTest {

    private static final Money FALLBACK = Money.of(new BigDecimal("5000"), "XAF");

    // ── Deposit status mapping ────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "ACCEPTED,  INITIATED",
        "SUBMITTED, PENDING",
        "COMPLETED, SUCCESS",
        "FAILED,    FAILED",
        "REJECTED,  CANCELLED",
        "EXPIRED,   EXPIRED"
    })
    void should_map_deposit_statuses(String pawaPayStatus, PaymentStatus expected) {
        PawaPayDepositResponse response = depositResponse("TX-001", pawaPayStatus, null, null);
        PaymentResult result = PawaPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK);
        assertThat(result.status()).isEqualTo(expected);
    }

    @Test
    void should_throw_on_unknown_deposit_status() {
        PawaPayDepositResponse response = depositResponse("TX-001", "UNKNOWN", null, null);
        assertThatThrownBy(() -> PawaPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ── Money resolution ──────────────────────────────────────────────────────

    @Test
    void should_use_response_money_when_present() {
        PawaPayDepositResponse response = depositResponse("TX-001", "COMPLETED", "7500", "GHS");
        PaymentResult result = PawaPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK);
        assertThat(result.amount().amount()).isEqualByComparingTo("7500");
        assertThat(result.amount().currency()).isEqualTo("GHS");
    }

    @Test
    void should_use_fallback_money_when_response_has_no_amount() {
        PawaPayDepositResponse response = depositResponse("TX-001", "ACCEPTED", null, null);
        PaymentResult result = PawaPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK);
        assertThat(result.amount()).isEqualTo(FALLBACK);
    }

    // ── Refund status mapping ─────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "ACCEPTED,  INITIATED",
        "SUBMITTED, PENDING",
        "COMPLETED, REFUNDED",
        "FAILED,    FAILED",
        "REJECTED,  CANCELLED"
    })
    void should_map_refund_statuses(String pawaPayStatus, PaymentStatus expected) {
        PawaPayRefundResponse response = new PawaPayRefundResponse(
                "REFUND-001", "TX-001", pawaPayStatus, null, null, null);
        RefundResult result = PawaPayResponseMapper.toRefundResult(response, FALLBACK);
        assertThat(result.status()).isEqualTo(expected);
    }

    @Test
    void should_throw_on_unknown_refund_status() {
        PawaPayRefundResponse response = new PawaPayRefundResponse(
                "REFUND-001", "TX-001", "BOGUS", null, null, null);
        assertThatThrownBy(() -> PawaPayResponseMapper.toRefundResult(response, FALLBACK))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("BOGUS");
    }

    @Test
    void should_use_fallback_for_refund_when_amount_is_null() {
        PawaPayRefundResponse response = new PawaPayRefundResponse(
                "REFUND-001", "TX-001", "COMPLETED", null, null, null);
        RefundResult result = PawaPayResponseMapper.toRefundResult(response, FALLBACK);
        assertThat(result.amount()).isEqualTo(FALLBACK);
    }

    // ── Payment event ─────────────────────────────────────────────────────────

    @Test
    void should_map_to_payment_event() {
        PawaPayDepositResponse response = depositResponse("TX-001", "COMPLETED", "5000", "XAF");
        var event = PawaPayResponseMapper.toPaymentEvent(response, "TX-001", FALLBACK);
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(event.providerName()).isEqualTo("pawapay");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static PawaPayDepositResponse depositResponse(
            String id, String status, String amount, String currency) {
        return new PawaPayDepositResponse(id, status, amount, currency, null, null, null);
    }
}

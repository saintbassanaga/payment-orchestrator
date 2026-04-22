package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.provider.cinetpay.dto.CinetPayCheckData;
import io.payorch.provider.cinetpay.dto.CinetPayCheckResponse;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentResponse;
import io.payorch.provider.cinetpay.dto.CinetPayWebhookPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CinetPayResponseMapperTest {

    private static final Money FALLBACK = Money.of(new BigDecimal("5000"), "XAF");

    // ── Payment initiation ────────────────────────────────────────────────────

    @Test
    void should_map_payment_result_to_initiated() {
        CinetPayPaymentResponse response = new CinetPayPaymentResponse(
                "201", "CREATED", new io.payorch.provider.cinetpay.dto.CinetPayPaymentData(
                        "pay-token-001", "https://payment.url/pay"));

        PaymentResult result = CinetPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK);

        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.providerTransactionId()).isEqualTo("pay-token-001");
        assertThat(result.providerMessage()).contains("https://payment.url/pay");
    }

    @Test
    void should_fallback_transaction_id_when_data_is_null() {
        CinetPayPaymentResponse response = new CinetPayPaymentResponse("201", "CREATED", null);

        PaymentResult result = CinetPayResponseMapper.toPaymentResult(response, "TX-001", FALLBACK);

        assertThat(result.providerTransactionId()).isEqualTo("TX-001");
    }

    // ── Status check mapping ──────────────────────────────────────────────────

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
        "CREATED,   INITIATED",
        "PENDING,   PENDING",
        "ACCEPTED,  SUCCESS",
        "REFUSED,   FAILED",
        "CANCELLED, CANCELLED",
        "EXPIRED,   EXPIRED"
    })
    void should_map_check_statuses(String cinetPayStatus, PaymentStatus expected) {
        CinetPayCheckData data = new CinetPayCheckData(
                "PROV-001", "5000", "XAF", cinetPayStatus, null);
        CinetPayCheckResponse response = new CinetPayCheckResponse("200", "OK", data);

        PaymentResult result = CinetPayResponseMapper.toStatusResult(response, "TX-001", FALLBACK);

        assertThat(result.status()).isEqualTo(expected);
    }

    @Test
    void should_throw_on_unknown_status() {
        CinetPayCheckData data = new CinetPayCheckData("PROV-001", "5000", "XAF", "UNKNOWN", null);
        CinetPayCheckResponse response = new CinetPayCheckResponse("200", "OK", data);

        assertThatThrownBy(() -> CinetPayResponseMapper.toStatusResult(response, "TX-001", FALLBACK))
                .isInstanceOf(ProviderUnavailableException.class)
                .hasMessageContaining("UNKNOWN");
    }

    @Test
    void should_use_response_money_when_present() {
        CinetPayCheckData data = new CinetPayCheckData("PROV-001", "7500", "GHS", "ACCEPTED", null);
        CinetPayCheckResponse response = new CinetPayCheckResponse("200", "OK", data);

        PaymentResult result = CinetPayResponseMapper.toStatusResult(response, "TX-001", FALLBACK);

        assertThat(result.amount().amount()).isEqualByComparingTo("7500");
        assertThat(result.amount().currency()).isEqualTo("GHS");
    }

    @Test
    void should_use_fallback_when_data_is_null() {
        CinetPayCheckResponse response = new CinetPayCheckResponse("200", "OK", null);

        PaymentResult result = CinetPayResponseMapper.toStatusResult(response, "TX-001", FALLBACK);

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.amount()).isEqualTo(FALLBACK);
    }

    // ── Webhook event ─────────────────────────────────────────────────────────

    @Test
    void should_map_webhook_status_00_to_success() {
        CinetPayWebhookPayload payload = new CinetPayWebhookPayload(
                "TX-001", "5000", "XAF", "00", "MOBILE_MONEY");

        PaymentEvent event = CinetPayResponseMapper.toPaymentEvent(payload, FALLBACK);

        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(event.transactionId()).isEqualTo("TX-001");
    }

    @Test
    void should_map_webhook_non_00_to_failed() {
        CinetPayWebhookPayload payload = new CinetPayWebhookPayload(
                "TX-001", "5000", "XAF", "01", "MOBILE_MONEY");

        PaymentEvent event = CinetPayResponseMapper.toPaymentEvent(payload, FALLBACK);

        assertThat(event.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void should_use_fallback_when_webhook_has_no_amount() {
        CinetPayWebhookPayload payload = new CinetPayWebhookPayload(
                "TX-001", null, null, "00", null);

        PaymentEvent event = CinetPayResponseMapper.toPaymentEvent(payload, FALLBACK);

        assertThat(event.amount()).isEqualTo(FALLBACK);
    }
}

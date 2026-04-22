package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentRequest;
import io.payorch.provider.cinetpay.dto.CinetPayCheckRequest;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CinetPayRequestMapperTest {

    private static final String API_KEY = "test-api-key";
    private static final String SITE_ID = "test-site-id";

    @Test
    void should_map_payment_request_fields() {
        PaymentRequest request = new PaymentRequest(
                "TX-001",
                Money.of(new BigDecimal("5000"), "XAF"),
                "+237671000001",
                "Test payment",
                Optional.empty(),
                Map.of());

        CinetPayPaymentRequest dto = CinetPayRequestMapper.toPaymentRequest(request, API_KEY, SITE_ID);

        assertThat(dto.apikey()).isEqualTo(API_KEY);
        assertThat(dto.siteId()).isEqualTo(SITE_ID);
        assertThat(dto.transactionId()).isEqualTo("TX-001");
        assertThat(dto.amount()).isEqualTo(5000);
        assertThat(dto.currency()).isEqualTo("XAF");
        assertThat(dto.description()).isEqualTo("Test payment");
    }

    @Test
    void should_use_notify_url_from_metadata() {
        PaymentRequest request = new PaymentRequest(
                "TX-001",
                Money.of(new BigDecimal("1000"), "XOF"),
                "+221701234567",
                "Payment",
                Optional.empty(),
                Map.of(CinetPayRequestMapper.NOTIFY_URL_KEY, "https://example.com/webhook"));

        CinetPayPaymentRequest dto = CinetPayRequestMapper.toPaymentRequest(request, API_KEY, SITE_ID);

        assertThat(dto.notifyUrl()).isEqualTo("https://example.com/webhook");
    }

    @Test
    void should_default_empty_notify_url_when_absent() {
        PaymentRequest request = new PaymentRequest(
                "TX-001",
                Money.of(new BigDecimal("1000"), "XOF"),
                "+221701234567",
                "Payment",
                Optional.empty(),
                Map.of());

        CinetPayPaymentRequest dto = CinetPayRequestMapper.toPaymentRequest(request, API_KEY, SITE_ID);

        assertThat(dto.notifyUrl()).isEmpty();
    }

    @Test
    void should_set_return_url_from_request() {
        PaymentRequest request = new PaymentRequest(
                "TX-001",
                Money.of(new BigDecimal("1000"), "XOF"),
                "+221701234567",
                "Payment",
                Optional.of("https://example.com/return"),
                Map.of());

        CinetPayPaymentRequest dto = CinetPayRequestMapper.toPaymentRequest(request, API_KEY, SITE_ID);

        assertThat(dto.returnUrl()).isEqualTo("https://example.com/return");
    }

    @Test
    void should_map_check_request() {
        CinetPayCheckRequest dto = CinetPayRequestMapper.toCheckRequest("TX-001", API_KEY, SITE_ID);

        assertThat(dto.apikey()).isEqualTo(API_KEY);
        assertThat(dto.siteId()).isEqualTo(SITE_ID);
        assertThat(dto.transactionId()).isEqualTo("TX-001");
    }
}

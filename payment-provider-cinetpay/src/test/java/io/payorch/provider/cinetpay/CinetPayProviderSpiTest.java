package io.payorch.provider.cinetpay;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.payorch.core.exception.TransactionNotFoundException;
import io.payorch.core.exception.UnsupportedProviderOperationException;
import io.payorch.core.model.Environment;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.WebhookRequest;
import io.payorch.http.HttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CinetPayProviderSpiTest {

    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_SITE_ID = "site-001";

    private WireMockServer wireMock;
    private CinetPayProviderSpi provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        HttpClientConfig config = new HttpClientConfig(
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(2), 0);
        provider = CinetPayProviderSpi.withBaseUrl(
                "http://localhost:" + wireMock.port(), config, TEST_API_KEY, TEST_SITE_ID);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void should_return_provider_name() {
        assertThat(provider.providerName()).isEqualTo("cinetpay");
    }

    @Test
    void should_initiate_payment_and_return_initiated_status() {
        wireMock.stubFor(post(urlEqualTo("/payment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "201",
                                    "message": "CREATED",
                                    "data": {
                                        "payment_token": "tok-abc123",
                                        "payment_url": "https://checkout.cinetpay.com/pay/tok-abc123"
                                    }
                                }
                                """)));

        PaymentRequest request = new PaymentRequest(
                "TX-001", Money.of(new BigDecimal("5000"), "XAF"),
                "+237600000000", "Test payment",
                Optional.of("https://example.com/return"), Map.of()
        );

        PaymentResult result = provider.initiate(request);

        assertThat(result.transactionId()).isEqualTo("TX-001");
        assertThat(result.providerTransactionId()).isEqualTo("tok-abc123");
        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.providerName()).isEqualTo("cinetpay");
        assertThat(result.providerMessage()).isPresent()
                .hasValueSatisfying(url -> assertThat(url).contains("tok-abc123"));
    }

    @Test
    void should_initiate_payment_without_data_uses_transaction_id_as_token() {
        wireMock.stubFor(post(urlEqualTo("/payment"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "201",
                                    "message": "CREATED"
                                }
                                """)));

        PaymentRequest request = new PaymentRequest(
                "TX-002", Money.of(new BigDecimal("1000"), "XOF"),
                "+2250700000000", "Fallback token test",
                Optional.empty(), Map.of()
        );

        PaymentResult result = provider.initiate(request);

        assertThat(result.providerTransactionId()).isEqualTo("TX-002");
        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
    }

    @Test
    void should_get_status_and_return_success() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "00",
                                    "message": "ACCEPTED",
                                    "data": {
                                        "transaction_id": "PROV-TX-002",
                                        "amount": "5000",
                                        "currency_code": "XAF",
                                        "status": "ACCEPTED"
                                    }
                                }
                                """)));

        PaymentResult result = provider.getStatus("TX-002");

        assertThat(result.transactionId()).isEqualTo("TX-002");
        assertThat(result.providerTransactionId()).isEqualTo("PROV-TX-002");
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount().amount()).isEqualByComparingTo("5000");
        assertThat(result.amount().currency()).isEqualTo("XAF");
    }

    @Test
    void should_get_status_and_return_pending() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "600",
                                    "message": "PENDING",
                                    "data": {
                                        "transaction_id": "TX-003",
                                        "amount": "2000",
                                        "currency_code": "XOF",
                                        "status": "PENDING"
                                    }
                                }
                                """)));

        PaymentResult result = provider.getStatus("TX-003");

        assertThat(result.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void should_get_status_and_return_failed() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "642",
                                    "message": "REFUSED",
                                    "data": {
                                        "transaction_id": "TX-004",
                                        "amount": "3000",
                                        "currency_code": "XAF",
                                        "status": "REFUSED"
                                    }
                                }
                                """)));

        PaymentResult result = provider.getStatus("TX-004");

        assertThat(result.status()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void should_get_status_and_return_cancelled() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "643",
                                    "message": "CANCELLED",
                                    "data": {
                                        "transaction_id": "TX-005",
                                        "amount": "1500",
                                        "currency_code": "XAF",
                                        "status": "CANCELLED"
                                    }
                                }
                                """)));

        PaymentResult result = provider.getStatus("TX-005");

        assertThat(result.status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void should_get_status_and_return_expired() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "629",
                                    "message": "EXPIRED",
                                    "data": {
                                        "transaction_id": "TX-006",
                                        "amount": "7500",
                                        "currency_code": "GNF",
                                        "status": "EXPIRED"
                                    }
                                }
                                """)));

        PaymentResult result = provider.getStatus("TX-006");

        assertThat(result.status()).isEqualTo(PaymentStatus.EXPIRED);
    }

    @Test
    void should_throw_transaction_not_found_when_check_returns_404() {
        wireMock.stubFor(post(urlEqualTo("/payment/check"))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> provider.getStatus("TX-UNKNOWN"))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining("TX-UNKNOWN");
    }

    @Test
    void should_initiate_payout_and_return_initiated_status() {
        wireMock.stubFor(post(urlEqualTo("/transfer/contact/bulk/add"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "code": "0",
                                    "message": "SUCCESS",
                                    "lot": "LOT-123"
                                }
                                """)));

        io.payorch.core.model.PayoutRequest request = new io.payorch.core.model.PayoutRequest(
                "PO-001",
                Money.of(new BigDecimal("5000"), "XAF"),
                "+237671000001", "Salary payout", Map.of()
        );

        io.payorch.core.model.PayoutResult result = provider.payout(request);

        assertThat(result.payoutId()).isEqualTo("PO-001");
        assertThat(result.providerPayoutId()).isEqualTo("LOT-123");
        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.providerName()).isEqualTo("cinetpay");
    }

    @Test
    void should_throw_unsupported_for_refund() {
        RefundRequest request = new RefundRequest(
                "RF-001", "TX-001",
                Money.of(new BigDecimal("5000"), "XAF"),
                "duplicate", Map.of()
        );

        assertThatThrownBy(() -> provider.refund(request))
                .isInstanceOf(UnsupportedProviderOperationException.class)
                .hasMessageContaining("cinetpay")
                .hasMessageContaining("refund");
    }

    @Test
    void should_parse_webhook_and_return_payment_event() {
        WebhookRequest request = new WebhookRequest("cinetpay", Map.of(), """
                {
                    "cpm_trans_id": "TX-999",
                    "cpm_amount": "5000",
                    "cpm_currency": "XAF",
                    "cpm_result": "00"
                }
                """);

        var event = provider.parseWebhook(request);

        assertThat(event.transactionId()).isEqualTo("TX-999");
        assertThat(event.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(event.providerName()).isEqualTo("cinetpay");
    }

    @Test
    void should_throw_when_initiate_receives_null() {
        assertThatThrownBy(() -> provider.initiate(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_get_status_receives_null() {
        assertThatThrownBy(() -> provider.getStatus(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_return_capabilities() {
        assertThat(provider.capabilities().supportsRefund()).isFalse();
        assertThat(provider.capabilities().supportsStatusCheck()).isTrue();
        assertThat(provider.capabilities().supportedCurrencies()).containsExactlyInAnyOrder(
                "XAF", "XOF", "CDF", "GNF");
        assertThat(provider.capabilities().supportedEnvironments())
                .contains(Environment.SANDBOX, Environment.PRODUCTION);
    }
}

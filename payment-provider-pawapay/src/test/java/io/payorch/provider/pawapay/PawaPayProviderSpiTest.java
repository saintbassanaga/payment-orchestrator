package io.payorch.provider.pawapay;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.exception.TransactionNotFoundException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.http.HttpClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PawaPayProviderSpiTest {

    private WireMockServer wireMock;
    private PawaPayProviderSpi provider;

    @BeforeEach
    void setUp() {
        wireMock = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMock.start();

        HttpClientConfig config = new HttpClientConfig(
                Duration.ofSeconds(2), Duration.ofSeconds(2), Duration.ofSeconds(2), 0);
        provider = PawaPayProviderSpi.withBaseUrl("http://localhost:" + wireMock.port(), config);
    }

    @AfterEach
    void tearDown() {
        wireMock.stop();
    }

    @Test
    void should_return_provider_name() {
        assertThat(provider.providerName()).isEqualTo("pawapay");
    }

    @Test
    void should_initiate_deposit_and_return_initiated_status() {
        wireMock.stubFor(post(urlEqualTo("/v2/deposits"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "depositId": "TX-001",
                                    "status": "ACCEPTED"
                                }
                                """)));

        PaymentRequest request = new PaymentRequest(
                "TX-001", Money.of(new BigDecimal("5000"), "XAF"),
                "+237600000000", "Test payment",
                Optional.empty(), Map.of("correspondent", "MTN_MOMO_CMR")
        );

        PaymentResult result = provider.initiate(request);

        assertThat(result.transactionId()).isEqualTo("TX-001");
        assertThat(result.providerTransactionId()).isEqualTo("TX-001");
        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.providerName()).isEqualTo("pawapay");
    }

    @Test
    void should_get_status_and_return_success() {
        // getStatus uses the transactionId directly as the URL path — must be a valid UUID
        String txId = "a1b2c3d4-e5f6-7890-abcd-ef1234567890";
        wireMock.stubFor(get(urlEqualTo("/v2/deposits/" + txId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "depositId": "%s",
                                    "status": "COMPLETED",
                                    "requestedAmount": "5000",
                                    "currency": "XAF",
                                    "correspondent": "MTN_MOMO_CMR",
                                    "created": "2026-04-18T10:00:00Z"
                                }
                                """.formatted(txId))));

        PaymentResult result = provider.getStatus(txId);

        assertThat(result.transactionId()).isEqualTo(txId);
        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(result.amount().amount()).isEqualByComparingTo("5000");
        assertThat(result.amount().currency()).isEqualTo("XAF");
    }

    @Test
    void should_throw_transaction_not_found_when_deposit_returns_404() {
        String txId = "b2c3d4e5-f6a7-8901-bcde-f12345678901";
        wireMock.stubFor(get(urlEqualTo("/v2/deposits/" + txId))
                .willReturn(aResponse().withStatus(404)));

        assertThatThrownBy(() -> provider.getStatus(txId))
                .isInstanceOf(TransactionNotFoundException.class)
                .hasMessageContaining(txId);
    }

    @Test
    void should_initiate_refund_and_return_result() {
        wireMock.stubFor(post(urlEqualTo("/v2/refunds"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "refundId": "RF-001",
                                    "depositId": "TX-003",
                                    "status": "ACCEPTED",
                                    "amount": "5000",
                                    "currency": "XAF"
                                }
                                """)));

        RefundRequest request = new RefundRequest(
                "RF-001", "TX-003",
                Money.of(new BigDecimal("5000"), "XAF"),
                "Duplicate payment", Map.of()
        );

        RefundResult result = provider.refund(request);

        assertThat(result.refundId()).isEqualTo("RF-001");
        assertThat(result.originalTransactionId()).isEqualTo("TX-003");
        assertThat(result.status()).isEqualTo(PaymentStatus.INITIATED);
        assertThat(result.providerName()).isEqualTo("pawapay");
    }

    @Test
    void should_throw_when_correspondent_is_missing_from_metadata() {
        PaymentRequest request = new PaymentRequest(
                "TX-004", Money.of(new BigDecimal("5000"), "XAF"),
                "+237600000000", "Test payment",
                Optional.empty(), Map.of()
        );

        assertThatThrownBy(() -> provider.initiate(request))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("correspondent");
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
    void should_return_capabilities_with_supported_currencies() {
        assertThat(provider.capabilities().supportsRefund()).isTrue();
        assertThat(provider.capabilities().supportsStatusCheck()).isTrue();
        assertThat(provider.capabilities().supportedCurrencies()).contains("XAF", "XOF", "GHS");
    }
}

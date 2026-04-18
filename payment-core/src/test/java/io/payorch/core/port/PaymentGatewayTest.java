package io.payorch.core.port;

import io.payorch.core.exception.ProviderNotFoundException;
import io.payorch.core.model.Environment;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.CommunityPaymentProviderSpi;
import io.payorch.core.spi.ProviderRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentGatewayTest {

    @Mock
    private CommunityPaymentProviderSpi mockProvider;

    private ProviderRegistry registry;
    private PaymentGateway gateway;
    private ProviderCredentials credentials;
    private Money amount;

    @BeforeEach
    void setUp() {
        when(mockProvider.providerName()).thenReturn("fake");
        registry = ProviderRegistry.empty();
        registry.register(mockProvider);
        gateway = PaymentGateway.builder().registry(registry).build();
        credentials = new ProviderCredentials("fake", Environment.SANDBOX, Map.of());
        amount = Money.of(new BigDecimal("1000"), "XAF");
    }

    @Test
    void should_throw_when_registry_not_set() {
        assertThatThrownBy(() -> PaymentGateway.builder().build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ProviderRegistry");
    }

    @Test
    void should_expose_registry() {
        assertThat(gateway.registry()).isSameAs(registry);
    }

    @Test
    void should_delegate_initiate_to_provider() {
        PaymentRequest request = PaymentRequest.of("TX-01", amount, "+237600000000", "test");
        PaymentResult expected = paymentResult("TX-01", PaymentStatus.INITIATED);
        when(mockProvider.initiate(request)).thenReturn(expected);

        PaymentResult result = gateway.initiate("fake", credentials, request);

        assertThat(result).isSameAs(expected);
        verify(mockProvider).configure(credentials);
        verify(mockProvider).initiate(request);
    }

    @Test
    void should_delegate_get_status_to_provider() {
        PaymentResult expected = paymentResult("TX-02", PaymentStatus.SUCCESS);
        when(mockProvider.getStatus("TX-02")).thenReturn(expected);

        PaymentResult result = gateway.getStatus("fake", credentials, "TX-02");

        assertThat(result).isSameAs(expected);
        verify(mockProvider).configure(credentials);
        verify(mockProvider).getStatus("TX-02");
    }

    @Test
    void should_delegate_refund_to_provider() {
        RefundRequest request = new RefundRequest("RF-01", "TX-03", amount, "duplicate", Map.of());
        RefundResult expected = new RefundResult(
                "RF-01", "TX-03", PaymentStatus.REFUNDED, amount, "fake", Optional.empty(), Instant.now()
        );
        when(mockProvider.refund(request)).thenReturn(expected);

        RefundResult result = gateway.refund("fake", credentials, request);

        assertThat(result).isSameAs(expected);
        verify(mockProvider).configure(credentials);
        verify(mockProvider).refund(request);
    }

    @Test
    void should_delegate_parse_webhook_to_provider() {
        WebhookRequest webhookRequest = new WebhookRequest("fake", Map.of(), "{}");
        PaymentEvent expected = new PaymentEvent(
                "TX-04", "EXT-04", PaymentStatus.SUCCESS, amount, "fake", Instant.now(), Map.of()
        );
        when(mockProvider.parseWebhook(webhookRequest)).thenReturn(expected);

        PaymentEvent event = gateway.parseWebhook("fake", credentials, webhookRequest);

        assertThat(event).isSameAs(expected);
        verify(mockProvider).configure(credentials);
        verify(mockProvider).parseWebhook(webhookRequest);
    }

    @Test
    void should_throw_when_provider_not_found() {
        assertThatThrownBy(() -> gateway.initiate(
                "unknown", credentials, PaymentRequest.of("TX-05", amount, "+237", "test")))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void should_throw_when_provider_name_is_null_in_initiate() {
        PaymentRequest request = PaymentRequest.of("TX-06", amount, "+237", "test");
        assertThatThrownBy(() -> gateway.initiate(null, credentials, request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_credentials_are_null_in_initiate() {
        PaymentRequest request = PaymentRequest.of("TX-07", amount, "+237", "test");
        assertThatThrownBy(() -> gateway.initiate("fake", null, request))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_request_is_null_in_initiate() {
        assertThatThrownBy(() -> gateway.initiate("fake", credentials, (PaymentRequest) null))
                .isInstanceOf(NullPointerException.class);
    }

    private static PaymentResult paymentResult(String transactionId, PaymentStatus status) {
        return new PaymentResult(
                transactionId, "EXT-" + transactionId, status,
                Money.of(new BigDecimal("1000"), "XAF"), "fake",
                Optional.empty(), Instant.now(), Map.of()
        );
    }
}
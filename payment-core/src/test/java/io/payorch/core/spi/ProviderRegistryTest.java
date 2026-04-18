package io.payorch.core.spi;

import io.payorch.core.exception.ProviderNotFoundException;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderRegistryTest {

    private ProviderRegistry registry;

    @BeforeEach
    void setUp() {
        registry = ProviderRegistry.empty();
    }

    @Test
    void should_register_and_retrieve_provider() {
        PaymentProviderSpi provider = new FakeProvider("pawapay");
        registry.register(provider);

        assertThat(registry.get("pawapay")).isSameAs(provider);
    }

    @Test
    void should_throw_when_provider_not_found() {
        assertThatThrownBy(() -> registry.get("unknown"))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining("unknown");
    }

    @Test
    void should_return_true_when_provider_is_registered() {
        registry.register(new FakeProvider("cinetpay"));

        assertThat(registry.contains("cinetpay")).isTrue();
    }

    @Test
    void should_return_false_when_provider_is_absent() {
        assertThat(registry.contains("monetbill")).isFalse();
    }

    @Test
    void should_replace_provider_with_same_name() {
        FakeProvider first = new FakeProvider("pawapay");
        FakeProvider second = new FakeProvider("pawapay");
        registry.register(first);
        registry.register(second);

        assertThat(registry.get("pawapay")).isSameAs(second);
    }

    @Test
    void should_return_all_registered_providers() {
        registry.register(new FakeProvider("pawapay"));
        registry.register(new FakeProvider("cinetpay"));

        assertThat(registry.all()).hasSize(2);
    }

    @Test
    void should_throw_when_registering_null_provider() {
        assertThatThrownBy(() -> registry.register(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_getting_null_provider_name() {
        assertThatThrownBy(() -> registry.get(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_when_contains_null_provider_name() {
        assertThatThrownBy(() -> registry.contains(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_load_from_service_loader_without_throwing() {
        ProviderRegistry loaded = ProviderRegistry.loadFromServiceLoader();

        assertThat(loaded).isNotNull();
    }

    static final class FakeProvider implements CommunityPaymentProviderSpi {

        private final String name;

        FakeProvider(String name) {
            this.name = name;
        }

        @Override
        public String providerName() {
            return name;
        }

        @Override
        public void configure(ProviderCredentials credentials) {
        }

        @Override
        public PaymentResult initiate(PaymentRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaymentResult getStatus(String transactionId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public RefundResult refund(RefundRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaymentEvent parseWebhook(WebhookRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ProviderCapabilities capabilities() {
            return new ProviderCapabilities(false, false, false, List.of(), List.of());
        }
    }
}

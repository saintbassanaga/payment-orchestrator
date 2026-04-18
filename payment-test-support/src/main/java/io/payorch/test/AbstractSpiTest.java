package io.payorch.test;

import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.spi.PaymentProviderSpi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Abstract contract test that every {@link PaymentProviderSpi} implementation must satisfy.
 *
 * <p>Extend this class in the adapter's test module and implement the two abstract
 * factory methods. The inherited tests will verify that the adapter respects the SPI contract.
 *
 * <pre>{@code
 * class PawaPayProviderSpiTest extends AbstractSpiTest {
 *
 *     \@Override
 *     protected PaymentProviderSpi createProvider() {
 *         return new PawaPayProviderSpi();
 *     }
 *
 *     \@Override
 *     protected ProviderCredentials validCredentials() {
 *         return PaymentTestFixtures.sandboxCredentials("pawapay", Map.of("apiKey", "test-key"));
 *     }
 * }
 * }</pre>
 *
 * @since 0.1.0
 */
public abstract class AbstractSpiTest {

    private PaymentProviderSpi provider;

    /**
     * Creates a fresh instance of the provider under test.
     *
     * @return a new, unconfigured provider instance
     */
    protected abstract PaymentProviderSpi createProvider();

    /**
     * Returns valid sandbox credentials for the provider under test.
     *
     * @return credentials that allow successful configuration
     */
    protected abstract ProviderCredentials validCredentials();

    /**
     * Initializes a fresh provider before each test.
     */
    @BeforeEach
    final void initProvider() {
        provider = createProvider();
    }

    /**
     * Verifies that {@link PaymentProviderSpi#providerName()} returns a non-blank value.
     */
    @Test
    void should_return_non_blank_provider_name() {
        assertThat(provider.providerName())
                .isNotNull()
                .isNotBlank();
    }

    /**
     * Verifies that {@link PaymentProviderSpi#configure(ProviderCredentials)} accepts valid credentials.
     */
    @Test
    void should_configure_without_throwing_when_credentials_are_valid() {
        provider.configure(validCredentials());
    }

    /**
     * Verifies that {@link PaymentProviderSpi#configure(ProviderCredentials)} rejects null credentials.
     */
    @Test
    void should_throw_when_configure_receives_null_credentials() {
        assertThatThrownBy(() -> provider.configure(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that {@link PaymentProviderSpi#capabilities()} returns a non-null descriptor
     * with non-null collection fields.
     */
    @Test
    void should_return_non_null_capabilities() {
        ProviderCapabilities caps = provider.capabilities();

        assertThat(caps).isNotNull();
        assertThat(caps.supportedCurrencies()).isNotNull();
        assertThat(caps.supportedEnvironments()).isNotNull();
    }

    /**
     * Verifies that {@link PaymentProviderSpi#initiate} rejects a null request.
     */
    @Test
    void should_throw_when_initiate_receives_null_request() {
        provider.configure(validCredentials());

        assertThatThrownBy(() -> provider.initiate(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that {@link PaymentProviderSpi#getStatus} rejects a null transaction ID.
     */
    @Test
    void should_throw_when_get_status_receives_null_transaction_id() {
        provider.configure(validCredentials());

        assertThatThrownBy(() -> provider.getStatus(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that {@link PaymentProviderSpi#refund} rejects a null request.
     */
    @Test
    void should_throw_when_refund_receives_null_request() {
        provider.configure(validCredentials());

        assertThatThrownBy(() -> provider.refund(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Verifies that {@link PaymentProviderSpi#parseWebhook} rejects a null request.
     */
    @Test
    void should_throw_when_parse_webhook_receives_null_request() {
        provider.configure(validCredentials());

        assertThatThrownBy(() -> provider.parseWebhook(null))
                .isInstanceOf(NullPointerException.class);
    }

    /**
     * Exposes the provider under test for use in subclass-specific test methods.
     *
     * @return the current provider instance
     */
    protected final PaymentProviderSpi provider() {
        return provider;
    }
}

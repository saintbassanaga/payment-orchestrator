package io.payorch.core.spi;

import io.payorch.core.model.ProviderCredentials;
import java.util.Objects;

/**
 * Base class for all official PayOrch provider adapters.
 *
 * <p>Subclasses must call {@link #requireConfigured()} at the start of each
 * operation to enforce that {@link #configure(ProviderCredentials)} was called first.
 *
 * @since 0.1.0
 */
public abstract non-sealed class AbstractOfficialProviderSpi implements PaymentProviderSpi {

    private ProviderCredentials credentials;

    /**
     * Initialises the adapter with no credentials yet configured.
     *
     * @since 0.1.0
     */
    protected AbstractOfficialProviderSpi() {
    }

    /**
     * Stores the credentials supplied by the caller.
     *
     * @param credentials the provider credentials, never null
     * @throws NullPointerException if {@code credentials} is null
     */
    @Override
    public void configure(ProviderCredentials credentials) {
        this.credentials = Objects.requireNonNull(credentials, "credentials must not be null");
    }

    /**
     * Returns the credentials stored by the last call to {@link #configure}.
     *
     * @return the configured credentials, never null
     * @throws IllegalStateException if {@link #configure} has not been called yet
     */
    protected ProviderCredentials credentials() {
        if (credentials == null) {
            throw new IllegalStateException(
                    "Provider '%s' has not been configured — call configure() first"
                            .formatted(providerName()));
        }
        return credentials;
    }

    /**
     * Asserts that {@link #configure} has been called before proceeding with an operation.
     *
     * @throws IllegalStateException if the adapter is not yet configured
     */
    protected void requireConfigured() {
        credentials();
    }
}

package io.payorch.core.port;

import io.payorch.core.exception.ProviderNotFoundException;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.PaymentProviderSpi;
import io.payorch.core.spi.ProviderRegistry;
import java.util.Objects;

/**
 * Single entry point for all payment operations.
 *
 * <p>Resolves the requested provider from the {@link ProviderRegistry}, delegates
 * every operation to the corresponding {@link PaymentProviderSpi}, and returns
 * normalized results. The caller never interacts with a provider adapter directly.
 *
 * <p>Instances are created via {@link PaymentGateway#builder()}.
 *
 * <pre>{@code
 * PaymentGateway gateway = PaymentGateway.builder()
 *     .registry(ProviderRegistry.loadFromServiceLoader())
 *     .build();
 *
 * PaymentResult result = gateway.initiate("pawapay", credentials, request);
 * }</pre>
 *
 * @since 0.1.0
 */
public final class PaymentGateway {

    private final ProviderRegistry registry;

    private PaymentGateway(Builder builder) {
        this.registry = builder.registry;
    }

    /**
     * Returns a new builder for constructing a {@code PaymentGateway}.
     *
     * @return a fresh builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Initiates a payment with the specified provider.
     *
     * @param providerName the canonical name of the provider to use
     * @param credentials  the credentials to configure the provider with
     * @param request      the normalized payment request
     * @return the normalized payment result, never null
     * @throws NullPointerException  if any argument is null
     * @throws ProviderNotFoundException if the provider is not registered
     * @throws io.payorch.core.exception.PaymentException for any provider-level error
     */
    public PaymentResult initiate(
            String providerName, ProviderCredentials credentials, PaymentRequest request) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(request, "request must not be null");
        PaymentProviderSpi provider = resolve(providerName, credentials);
        return provider.initiate(request);
    }

    /**
     * Retrieves the current status of an existing transaction from the specified provider.
     *
     * @param providerName  the canonical name of the provider to query
     * @param credentials   the credentials to configure the provider with
     * @param transactionId the caller-supplied transaction identifier
     * @return the normalized payment result, never null
     * @throws NullPointerException  if any argument is null
     * @throws ProviderNotFoundException if the provider is not registered
     * @throws io.payorch.core.exception.PaymentException for any provider-level error
     */
    public PaymentResult getStatus(
            String providerName, ProviderCredentials credentials, String transactionId) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        PaymentProviderSpi provider = resolve(providerName, credentials);
        return provider.getStatus(transactionId);
    }

    /**
     * Issues a refund for a completed transaction via the specified provider.
     *
     * @param providerName the canonical name of the provider to use
     * @param credentials  the credentials to configure the provider with
     * @param request      the normalized refund request
     * @return the normalized refund result, never null
     * @throws NullPointerException  if any argument is null
     * @throws ProviderNotFoundException if the provider is not registered
     * @throws io.payorch.core.exception.PaymentException for any provider-level error
     */
    public RefundResult refund(
            String providerName, ProviderCredentials credentials, RefundRequest request) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(request, "request must not be null");
        PaymentProviderSpi provider = resolve(providerName, credentials);
        return provider.refund(request);
    }

    /**
     * Parses an inbound webhook payload into a normalized event via the specified provider.
     *
     * @param providerName the canonical name of the provider that sent the webhook
     * @param credentials  the credentials to configure the provider with
     * @param request      the raw webhook request
     * @return the normalized payment event, never null
     * @throws NullPointerException  if any argument is null
     * @throws ProviderNotFoundException if the provider is not registered
     * @throws io.payorch.core.exception.PaymentException for any provider-level error
     */
    public PaymentEvent parseWebhook(
            String providerName, ProviderCredentials credentials, WebhookRequest request) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(credentials, "credentials must not be null");
        Objects.requireNonNull(request, "request must not be null");
        PaymentProviderSpi provider = resolve(providerName, credentials);
        return provider.parseWebhook(request);
    }

    /**
     * Returns the registry backing this gateway.
     *
     * @return the provider registry, never null
     */
    public ProviderRegistry registry() {
        return registry;
    }

    private PaymentProviderSpi resolve(String providerName, ProviderCredentials credentials) {
        PaymentProviderSpi provider = registry.get(providerName);
        provider.configure(credentials);
        return provider;
    }

    /**
     * Builder for {@link PaymentGateway}.
     *
     * @since 0.1.0
     */
    public static final class Builder {

        private ProviderRegistry registry;

        private Builder() {
        }

        /**
         * Sets the provider registry to use.
         *
         * @param registry the registry containing the available provider adapters, never null
         * @return this builder
         * @throws NullPointerException if {@code registry} is null
         */
        public Builder registry(ProviderRegistry registry) {
            this.registry = Objects.requireNonNull(registry, "registry must not be null");
            return this;
        }

        /**
         * Builds and returns the configured {@link PaymentGateway}.
         *
         * @return a new gateway instance
         * @throws IllegalStateException if no registry was set
         */
        public PaymentGateway build() {
            if (registry == null) {
                throw new IllegalStateException("A ProviderRegistry must be set before building");
            }
            return new PaymentGateway(this);
        }
    }
}
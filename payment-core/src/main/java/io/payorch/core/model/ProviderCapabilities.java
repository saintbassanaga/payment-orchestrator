package io.payorch.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Describes the capabilities and constraints of a payment provider adapter.
 *
 * @param supportsRefund        whether the provider supports refund operations
 * @param supportsWebhook       whether the provider can emit and validate webhooks
 * @param supportsStatusCheck   whether the provider supports polling for transaction status
 * @param supportedCurrencies   the ISO 4217 currency codes accepted by the provider
 * @param supportedEnvironments the deployment environments available for this provider
 * @since 0.1.0
 */
public record ProviderCapabilities(
        boolean supportsRefund,
        boolean supportsWebhook,
        boolean supportsStatusCheck,
        List<String> supportedCurrencies,
        List<Environment> supportedEnvironments) {

    /**
     * Validates and defensively copies capability data at construction time.
     *
     * @throws NullPointerException if {@code supportedCurrencies} or
     *                              {@code supportedEnvironments} is null
     */
    public ProviderCapabilities {
        Objects.requireNonNull(supportedCurrencies, "supportedCurrencies must not be null");
        Objects.requireNonNull(supportedEnvironments, "supportedEnvironments must not be null");
        supportedCurrencies = List.copyOf(supportedCurrencies);
        supportedEnvironments = List.copyOf(supportedEnvironments);
    }
}

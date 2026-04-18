package io.payorch.core.model;

import io.payorch.core.exception.InvalidPaymentRequestException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds the credentials and configuration injected into a payment provider adapter.
 *
 * <p>Properties are stored as an immutable key-value map. Use {@link #require(String)}
 * to retrieve a mandatory property or {@link #get(String)} for an optional one.
 *
 * @param providerName the canonical name of the provider (e.g. {@code "pawapay"})
 * @param environment  the deployment environment ({@link Environment#SANDBOX} or
 *                     {@link Environment#PRODUCTION})
 * @param properties   the provider-specific configuration key-value pairs
 * @since 0.1.0
 */
public record ProviderCredentials(
        String providerName,
        Environment environment,
        Map<String, String> properties) {

    /**
     * Validates and defensively copies credentials at construction time.
     *
     * @throws NullPointerException if any parameter is null
     */
    public ProviderCredentials {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(environment, "environment must not be null");
        Objects.requireNonNull(properties, "properties must not be null");
        properties = Map.copyOf(properties);
    }

    /**
     * Returns the value of an optional configuration property.
     *
     * @param key the property key
     * @return an {@link Optional} containing the value, or empty if absent
     * @throws NullPointerException if {@code key} is null
     */
    public Optional<String> get(String key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(properties.get(key));
    }

    /**
     * Returns the value of a mandatory configuration property.
     *
     * @param key the property key
     * @return the property value
     * @throws NullPointerException           if {@code key} is null
     * @throws InvalidPaymentRequestException if the property is absent
     */
    public String require(String key) {
        return get(key).orElseThrow(() ->
                new InvalidPaymentRequestException(
                        "Missing required credential property '%s' for provider '%s'"
                                .formatted(key, providerName)));
    }
}

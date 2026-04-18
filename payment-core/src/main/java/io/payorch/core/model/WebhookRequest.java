package io.payorch.core.model;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the raw inbound webhook payload received from a payment provider.
 *
 * <p>The {@code rawBody} preserves the original payload exactly as received,
 * which is required for HMAC signature verification before any parsing occurs.
 *
 * @param providerName the name of the provider that sent the webhook
 * @param headers      the HTTP request headers, used for signature verification
 * @param rawBody      the unmodified raw body of the HTTP request
 * @since 0.1.0
 */
public record WebhookRequest(
        String providerName,
        Map<String, String> headers,
        String rawBody) {

    /**
     * Validates and defensively copies the webhook request at construction time.
     *
     * @throws NullPointerException if any field is null
     */
    public WebhookRequest {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(rawBody, "rawBody must not be null");
        headers = Map.copyOf(headers);
    }

    /**
     * Returns the value of a specific header, case-insensitively.
     *
     * @param name the header name
     * @return an {@link Optional} containing the header value, or empty if absent
     * @throws NullPointerException if {@code name} is null
     */
    public Optional<String> header(String name) {
        Objects.requireNonNull(name, "name must not be null");
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}

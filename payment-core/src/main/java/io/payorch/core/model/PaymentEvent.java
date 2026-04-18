package io.payorch.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a normalized payment event produced by parsing an inbound webhook.
 *
 * <p>Every webhook received from any provider is eventually translated into a
 * {@code PaymentEvent} by the corresponding {@code XxxResponseMapper}.
 *
 * @param transactionId         the caller-supplied transaction identifier
 * @param providerTransactionId the provider's own transaction reference
 * @param status                the normalized payment status reported by the webhook
 * @param amount                the amount involved in the transaction
 * @param providerName          the name of the provider that emitted the event
 * @param occurredAt            the timestamp at which the event occurred at the provider
 * @param metadata              additional provider-specific key-value pairs
 * @since 0.1.0
 */
public record PaymentEvent(
        String transactionId,
        String providerTransactionId,
        PaymentStatus status,
        Money amount,
        String providerName,
        Instant occurredAt,
        Map<String, String> metadata) {

    /**
     * Validates and defensively copies the payment event at construction time.
     *
     * @throws NullPointerException if any required field is null
     */
    public PaymentEvent {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(providerTransactionId, "providerTransactionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}

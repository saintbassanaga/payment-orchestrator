package io.payorch.core.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the normalized response returned after a payment initiation or status check.
 *
 * @param transactionId         the caller-supplied transaction identifier
 * @param providerTransactionId the provider's own transaction reference
 * @param status                the normalized payment status
 * @param amount                the amount involved in the transaction
 * @param providerName          the name of the provider that processed the payment
 * @param providerMessage       an optional human-readable message from the provider
 * @param createdAt             the timestamp at which the result was produced
 * @param metadata              additional provider-specific key-value pairs
 * @since 0.1.0
 */
public record PaymentResult(
        String transactionId,
        String providerTransactionId,
        PaymentStatus status,
        Money amount,
        String providerName,
        Optional<String> providerMessage,
        Instant createdAt,
        Map<String, String> metadata) {

    /**
     * Validates and defensively copies the payment result at construction time.
     *
     * @throws NullPointerException if any required field is null
     */
    public PaymentResult {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(providerTransactionId, "providerTransactionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        providerMessage = providerMessage != null ? providerMessage : Optional.empty();
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}
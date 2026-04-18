package io.payorch.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the normalized response returned after a refund operation.
 *
 * @param refundId              the provider's reference for this refund
 * @param originalTransactionId the identifier of the original payment that was refunded
 * @param status                the normalized refund status
 * @param amount                the amount that was refunded
 * @param providerName          the name of the provider that processed the refund
 * @param providerMessage       an optional human-readable message from the provider
 * @param createdAt             the timestamp at which the refund result was produced
 * @since 0.1.0
 */
public record RefundResult(
        String refundId,
        String originalTransactionId,
        PaymentStatus status,
        Money amount,
        String providerName,
        Optional<String> providerMessage,
        Instant createdAt) {

    /**
     * Validates the refund result at construction time.
     *
     * @throws NullPointerException if any required field is null
     */
    public RefundResult {
        Objects.requireNonNull(refundId, "refundId must not be null");
        Objects.requireNonNull(originalTransactionId, "originalTransactionId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        providerMessage = providerMessage != null ? providerMessage : Optional.empty();
    }
}
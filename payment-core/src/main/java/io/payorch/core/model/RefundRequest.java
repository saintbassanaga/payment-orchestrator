package io.payorch.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to refund a previously completed payment.
 *
 * @param transactionId         a unique identifier for this refund request
 * @param originalTransactionId the identifier of the payment to refund
 * @param amount                the amount to refund (may be less than the original for partial refunds)
 * @param reason                a human-readable explanation for the refund
 * @param metadata              arbitrary key-value pairs forwarded to the provider where supported
 * @since 0.1.0
 */
public record RefundRequest(
        String transactionId,
        String originalTransactionId,
        Money amount,
        String reason,
        Map<String, String> metadata) {

    /**
     * Validates and defensively copies the refund request at construction time.
     *
     * @throws NullPointerException if any required field is null
     */
    public RefundRequest {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        Objects.requireNonNull(originalTransactionId, "originalTransactionId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(reason, "reason must not be null");
        metadata = metadata != null ? Map.copyOf(metadata) : Map.of();
    }
}
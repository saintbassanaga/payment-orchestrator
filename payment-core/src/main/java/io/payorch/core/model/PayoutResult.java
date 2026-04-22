package io.payorch.core.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents the normalized response returned after a payout (disbursement) operation.
 *
 * @param payoutId         the caller-supplied payout identifier
 * @param providerPayoutId the provider's own reference for this payout
 * @param status           the normalized payout status
 * @param amount           the amount that was disbursed
 * @param providerName     the name of the provider that processed the payout
 * @param providerMessage  an optional human-readable message from the provider
 * @param createdAt        the timestamp at which the payout result was produced
 * @since 0.1.0
 */
public record PayoutResult(
        String payoutId,
        String providerPayoutId,
        PaymentStatus status,
        Money amount,
        String providerName,
        Optional<String> providerMessage,
        Instant createdAt) {

    /**
     * Validates all required fields at construction time.
     *
     * @throws NullPointerException if any required field is null
     */
    public PayoutResult {
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        Objects.requireNonNull(providerPayoutId, "providerPayoutId must not be null");
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        providerMessage = providerMessage != null ? providerMessage : Optional.empty();
    }
}

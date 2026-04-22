package io.payorch.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a request to disburse money to a recipient (payout / disbursement).
 *
 * <p>A payout sends money <em>from</em> the merchant <em>to</em> a mobile-money subscriber,
 * as opposed to a {@link PaymentRequest} which collects money from a subscriber.
 *
 * @param payoutId       the caller-supplied unique identifier for this payout
 * @param amount         the amount and currency to disburse
 * @param recipientPhone the recipient's phone number in E.164 format
 * @param description    a short human-readable reason (may be truncated by the provider)
 * @param metadata       provider-specific extras (e.g. {@code "correspondent"} for PawaPay)
 * @since 0.1.0
 */
public record PayoutRequest(
        String payoutId,
        Money amount,
        String recipientPhone,
        String description,
        Map<String, String> metadata) {

    /**
     * Validates all required fields at construction time.
     *
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if payoutId or recipientPhone is blank
     */
    public PayoutRequest {
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(recipientPhone, "recipientPhone must not be null");
        Objects.requireNonNull(description, "description must not be null");
        Objects.requireNonNull(metadata, "metadata must not be null");
        if (payoutId.isBlank()) {
            throw new IllegalArgumentException("payoutId must not be blank");
        }
        if (recipientPhone.isBlank()) {
            throw new IllegalArgumentException("recipientPhone must not be blank");
        }
        metadata = Map.copyOf(metadata);
    }
}

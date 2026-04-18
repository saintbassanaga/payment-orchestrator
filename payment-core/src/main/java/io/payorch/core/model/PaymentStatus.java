package io.payorch.core.model;

/**
 * Represents the normalized lifecycle status of a payment or refund transaction.
 *
 * <p>Provider-specific statuses are always mapped to one of these values by the
 * corresponding {@code XxxResponseMapper}.
 *
 * @since 0.1.0
 */
public enum PaymentStatus {

    /** Payment initiation request has been submitted to the provider. */
    INITIATED,

    /** Payment is awaiting confirmation from the payer or the provider. */
    PENDING,

    /** Payment was completed successfully. */
    SUCCESS,

    /** Payment failed definitively. */
    FAILED,

    /** Payment was cancelled before completion. */
    CANCELLED,

    /** Payment window expired before the payer acted. */
    EXPIRED,

    /** Full refund was issued for the original transaction. */
    REFUNDED,

    /** Partial refund was issued for the original transaction. */
    PARTIAL_REFUND
}
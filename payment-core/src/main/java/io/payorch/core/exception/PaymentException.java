package io.payorch.core.exception;

import java.io.Serial;

/**
 * Base class for all PayOrch payment exceptions.
 *
 * <p>This hierarchy is sealed: no subtype can be created outside of
 * {@code payment-core}. All subclasses are unchecked (extend {@link RuntimeException}).
 *
 * @since 0.1.0
 */
public abstract sealed class PaymentException extends RuntimeException
        permits ProviderAuthException,
        ProviderUnavailableException,
        ProviderNotFoundException,
        InvalidPaymentRequestException,
        WebhookValidationException,
        TransactionNotFoundException,
        UnsupportedProviderOperationException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new payment exception with the specified detail message.
     *
     * @param message the detail message
     */
    protected PaymentException(String message) {
        super(message);
    }

    /**
     * Constructs a new payment exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    protected PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}
package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when a payment request contains invalid or missing data.
 *
 * @since 0.1.0
 */
public final class InvalidPaymentRequestException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new invalid payment request exception with a descriptive message.
     *
     * @param message description of what makes the request invalid
     */
    public InvalidPaymentRequestException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid payment request exception with a message and cause.
     *
     * @param message description of what makes the request invalid
     * @param cause   the underlying cause
     */
    public InvalidPaymentRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
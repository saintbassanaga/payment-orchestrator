package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when an incoming webhook payload fails signature or structure validation.
 *
 * @since 0.1.0
 */
public final class WebhookValidationException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new webhook validation exception.
     *
     * @param provider the name of the provider whose webhook was rejected
     * @param reason   the reason for the validation failure
     */
    public WebhookValidationException(String provider, String reason) {
        super("Webhook validation failed for provider '%s': %s".formatted(provider, reason));
    }

    /**
     * Constructs a new webhook validation exception with a cause.
     *
     * @param provider the name of the provider whose webhook was rejected
     * @param reason   the reason for the validation failure
     * @param cause    the underlying cause
     */
    public WebhookValidationException(String provider, String reason, Throwable cause) {
        super("Webhook validation failed for provider '%s': %s".formatted(provider, reason), cause);
    }
}
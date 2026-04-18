package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when authentication with a payment provider fails.
 *
 * @since 0.1.0
 */
public final class ProviderAuthException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new provider authentication exception.
     *
     * @param provider the name of the provider that rejected the credentials
     * @param cause    the underlying cause
     */
    public ProviderAuthException(String provider, Throwable cause) {
        super("Authentication failed for provider '%s'".formatted(provider), cause);
    }

    /**
     * Constructs a new provider authentication exception with a custom message.
     *
     * @param provider the name of the provider that rejected the credentials
     * @param message  additional context about the failure
     */
    public ProviderAuthException(String provider, String message) {
        super("Authentication failed for provider '%s': %s".formatted(provider, message));
    }
}

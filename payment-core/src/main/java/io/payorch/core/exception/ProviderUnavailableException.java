package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when a payment provider is temporarily unavailable or returns an unexpected response.
 *
 * @since 0.1.0
 */
public final class ProviderUnavailableException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new provider unavailable exception.
     *
     * @param provider the name of the unavailable provider
     * @param cause    the underlying cause
     */
    public ProviderUnavailableException(String provider, Throwable cause) {
        super("Provider '%s' is unavailable".formatted(provider), cause);
    }

    /**
     * Constructs a new provider unavailable exception with a custom message.
     *
     * @param message description of the unavailability
     */
    public ProviderUnavailableException(String message) {
        super(message);
    }
}
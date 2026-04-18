package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when an operation is invoked on a provider that does not support it.
 *
 * @since 0.1.0
 */
public final class UnsupportedProviderOperationException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new unsupported provider operation exception.
     *
     * @param provider  the name of the provider
     * @param operation the name of the unsupported operation
     */
    public UnsupportedProviderOperationException(String provider, String operation) {
        super("Provider '%s' does not support operation '%s'".formatted(provider, operation));
    }
}
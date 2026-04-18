package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when no registered provider matches the requested provider name.
 *
 * @since 0.1.0
 */
public final class ProviderNotFoundException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new provider not found exception.
     *
     * @param providerName the name of the provider that was not found
     */
    public ProviderNotFoundException(String providerName) {
        super("Provider '%s' not found in registry. Did you add the dependency?"
                .formatted(providerName));
    }
}

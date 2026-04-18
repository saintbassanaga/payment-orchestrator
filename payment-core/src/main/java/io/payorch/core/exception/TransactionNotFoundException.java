package io.payorch.core.exception;

import java.io.Serial;

/**
 * Thrown when a transaction cannot be found at the provider.
 *
 * @since 0.1.0
 */
public final class TransactionNotFoundException extends PaymentException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new transaction not found exception.
     *
     * @param provider      the name of the provider that was queried
     * @param transactionId the transaction identifier that was not found
     */
    public TransactionNotFoundException(String provider, String transactionId) {
        super("Transaction '%s' not found at provider '%s'".formatted(transactionId, provider));
    }
}
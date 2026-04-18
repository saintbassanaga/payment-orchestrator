package io.payorch.core.model;

import io.payorch.core.exception.InvalidPaymentRequestException;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Represents a monetary amount with its ISO 4217 currency code.
 *
 * <p>Both {@code amount} and {@code currency} are validated at construction time.
 * The amount must be strictly positive and the currency must be a three-letter ISO 4217 code.
 *
 * @param amount   the positive monetary amount
 * @param currency the ISO 4217 currency code (e.g. {@code "XAF"}, {@code "EUR"})
 * @since 0.1.0
 */
public record Money(BigDecimal amount, String currency) {

    /**
     * Validates and normalizes a {@code Money} instance at construction time.
     *
     * @throws NullPointerException            if {@code amount} or {@code currency} is null
     * @throws InvalidPaymentRequestException  if {@code amount} is not strictly positive
     *                                         or {@code currency} is not a valid ISO 4217 code
     */
    public Money {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(currency, "currency must not be null");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentRequestException(
                    "Amount must be strictly positive, got: " + amount);
        }
        if (currency.isBlank() || currency.length() != 3) {
            throw new InvalidPaymentRequestException(
                    "Currency must be a valid ISO 4217 code (3 letters), got: " + currency);
        }
        currency = currency.toUpperCase();
    }

    /**
     * Creates a {@code Money} instance from a {@link BigDecimal} amount and currency code.
     *
     * @param amount   the positive monetary amount
     * @param currency the ISO 4217 currency code
     * @return a validated {@code Money} instance
     * @throws InvalidPaymentRequestException if amount or currency is invalid
     */
    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    /**
     * Creates a {@code Money} instance from a string amount and currency code.
     *
     * @param amount   the positive monetary amount as a string
     * @param currency the ISO 4217 currency code
     * @return a validated {@code Money} instance
     * @throws InvalidPaymentRequestException if amount or currency is invalid
     * @throws NumberFormatException           if {@code amount} cannot be parsed
     */
    public static Money of(String amount, String currency) {
        Objects.requireNonNull(amount, "amount must not be null");
        return new Money(new BigDecimal(amount), currency);
    }
}

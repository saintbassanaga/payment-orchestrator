package io.payorch.phone;

import java.util.Objects;

/**
 * Represents a validated and normalized phone number.
 *
 * @param e164           the phone number in E.164 format including the leading '+' (e.g. {@code "+237600000000"})
 * @param e164Digits     the E.164 digits without the leading '+' — required by PawaPay (e.g. {@code "237600000000"})
 * @param countryCode    the ISO 3166-1 alpha-2 region code (e.g. {@code "CM"})
 * @param nationalNumber the national significant number without the country calling code
 * @since 0.1.0
 */
public record ParsedPhoneNumber(
        String e164,
        String e164Digits,
        String countryCode,
        String nationalNumber) {

    /**
     * Validates the parsed phone number at construction time.
     *
     * @throws NullPointerException if any field is null
     */
    public ParsedPhoneNumber {
        Objects.requireNonNull(e164, "e164 must not be null");
        Objects.requireNonNull(e164Digits, "e164Digits must not be null");
        Objects.requireNonNull(countryCode, "countryCode must not be null");
        Objects.requireNonNull(nationalNumber, "nationalNumber must not be null");
    }
}

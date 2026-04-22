package io.payorch.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.payorch.core.exception.InvalidPaymentRequestException;

import java.util.Objects;

/**
 * Parses and normalizes phone numbers to E.164 format using Google libphonenumber.
 *
 * <p>Accepts any common format (national, international, with/without '+') and
 * returns a {@link ParsedPhoneNumber} with all representations pre-computed.
 * Throws {@link InvalidPaymentRequestException} for invalid numbers so callers
 * never receive a silently malformed result.
 *
 * <p>E.164 numbers (starting with {@code +}) can be parsed without a region hint
 * via {@link #parse(String)}.
 *
 * @since 0.1.0
 */
public final class PhoneNumberParser {

    private static final PhoneNumberUtil UTIL = PhoneNumberUtil.getInstance();

    private PhoneNumberParser() {
    }

    /**
     * Parses {@code rawNumber} in E.164 format (e.g. {@code +237671234567}).
     * The country is auto-detected from the international prefix — no region hint needed.
     *
     * @param rawNumber the phone number in E.164 format (must start with {@code +})
     * @return a fully normalized {@link ParsedPhoneNumber}, never null
     * @throws NullPointerException          if {@code rawNumber} is null
     * @throws InvalidPaymentRequestException if the number cannot be parsed or is invalid
     * @since 0.1.0
     */
    public static ParsedPhoneNumber parse(String rawNumber) {
        Objects.requireNonNull(rawNumber, "rawNumber must not be null");
        return parseInternal(rawNumber, null);
    }

    /**
     * Parses {@code rawNumber} using {@code defaultRegion} as the fallback country
     * when the number has no international prefix.
     *
     * @param rawNumber     the phone number string in any common format
     * @param defaultRegion the ISO 3166-1 alpha-2 region code used when the number
     *                      lacks an international prefix (e.g. {@code "CM"} for Cameroon)
     * @return a fully normalized {@link ParsedPhoneNumber}, never null
     * @throws NullPointerException          if any argument is null
     * @throws InvalidPaymentRequestException if the number is invalid for the given region
     * @since 0.1.0
     */
    public static ParsedPhoneNumber parse(String rawNumber, String defaultRegion) {
        Objects.requireNonNull(rawNumber, "rawNumber must not be null");
        Objects.requireNonNull(defaultRegion, "defaultRegion must not be null");
        return parseInternal(rawNumber, defaultRegion.toUpperCase());
    }

    private static ParsedPhoneNumber parseInternal(String rawNumber, String regionHint) {
        Phonenumber.PhoneNumber number;
        try {
            number = UTIL.parse(rawNumber, regionHint);
        } catch (NumberParseException e) {
            throw new InvalidPaymentRequestException(
                    "Invalid phone number '%s' (region=%s): %s"
                            .formatted(rawNumber, regionHint, e.getMessage()));
        }

        if (!UTIL.isValidNumber(number)) {
            throw new InvalidPaymentRequestException(
                    "Phone number '%s' is not valid (region=%s)"
                            .formatted(rawNumber, regionHint));
        }

        String region = UTIL.getRegionCodeForNumber(number);
        String e164 = UTIL.format(number, PhoneNumberUtil.PhoneNumberFormat.E164);
        String e164Digits = e164.startsWith("+") ? e164.substring(1) : e164;
        String national = UTIL.getNationalSignificantNumber(number);
        String fallbackRegion = region != null ? region : (regionHint != null ? regionHint : "ZZ");

        return new ParsedPhoneNumber(e164, e164Digits, fallbackRegion, national);
    }
}

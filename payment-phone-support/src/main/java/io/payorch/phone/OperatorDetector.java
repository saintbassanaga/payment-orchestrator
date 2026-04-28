package io.payorch.phone;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberToCarrierMapper;
import com.google.i18n.phonenumbers.PhoneNumberUtil;

import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * Detects the {@link MobileOperator} from a {@link ParsedPhoneNumber} using
 * the Google libphonenumber carrier database.
 *
 * <p>Works with any E.164 number — no region hint or manual prefix table required.
 * The carrier name returned by the database is matched case-insensitively against
 * known operator brands.
 *
 * <p>Returns {@link Optional#empty()} when the carrier database has no entry for
 * the number — the caller decides whether to fall back to a default or reject the request.
 *
 * @since 0.1.0
 */
public final class OperatorDetector {

    private static final PhoneNumberUtil PHONE_UTIL = PhoneNumberUtil.getInstance();
    private static final PhoneNumberToCarrierMapper CARRIER_MAPPER = PhoneNumberToCarrierMapper.getInstance();

    private OperatorDetector() {
    }

    /**
     * Detects the mobile operator for the given phone number.
     *
     * @param phone the normalized phone number (from {@link PhoneNumberParser})
     * @return the detected operator, or empty if the carrier is unrecognized
     * @throws NullPointerException if {@code phone} is null
     * @since 0.1.0
     */
    public static Optional<MobileOperator> detect(ParsedPhoneNumber phone) {
        Objects.requireNonNull(phone, "phone must not be null");
        try {
            com.google.i18n.phonenumbers.Phonenumber.PhoneNumber parsed =
                    PHONE_UTIL.parse(phone.e164(), null);
            String carrier = CARRIER_MAPPER.getNameForNumber(parsed, Locale.ENGLISH)
                    .toLowerCase(Locale.ROOT);
            if (carrier.isEmpty()) {
                return Optional.empty();
            }
            return Arrays.stream(MobileOperator.values())
                    .filter(op -> carrier.contains(op.name().toLowerCase(Locale.ROOT)))
                    .findFirst();
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }
}

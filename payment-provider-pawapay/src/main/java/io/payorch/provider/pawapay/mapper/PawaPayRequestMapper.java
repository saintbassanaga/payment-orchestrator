package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PayoutRequest;
import io.payorch.core.model.RefundRequest;
import io.payorch.phone.MobileOperator;
import io.payorch.phone.OperatorDetector;
import io.payorch.phone.ParsedPhoneNumber;
import io.payorch.phone.PhoneNumberParser;
import io.payorch.provider.pawapay.dto.PawaPayAccountDetails;
import io.payorch.provider.pawapay.dto.PawaPayDepositRequest;
import io.payorch.provider.pawapay.dto.PawaPayPayer;
import io.payorch.provider.pawapay.dto.PawaPayPayoutRequest;
import io.payorch.provider.pawapay.dto.PawaPayRecipient;
import io.payorch.provider.pawapay.dto.PawaPayRecipientAddress;
import io.payorch.provider.pawapay.dto.PawaPayRefundRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Pure mapping functions from PayOrch request models to PawaPay v2 request DTOs.
 *
 * <p>No network calls, no side effects, no mutable state.
 *
 * <p>The PawaPay correspondent (mobile operator) is resolved in priority order:
 * <ol>
 *   <li>Explicit value in {@link PaymentRequest#metadata()} under key {@code "correspondent"}</li>
 *   <li>Auto-detected via the libphonenumber carrier database (no region hint required)</li>
 *   <li>Fallback to curated prefix tables</li>
 * </ol>
 *
 * <p>The phone number region is auto-detected from the E.164 country prefix.
 * No {@code "region"} metadata key is required.
 *
 * @since 0.1.0
 */
public final class PawaPayRequestMapper {

    /**
     * Optional metadata key for the PawaPay correspondent override.
     * When present, operator auto-detection is skipped.
     * Example value: {@code "MTN_MOMO_CMR"}.
     */
    public static final String CORRESPONDENT_KEY = "correspondent";

    private static final int CUSTOMER_MESSAGE_MAX_LEN = 22;

    private PawaPayRequestMapper() {
    }

    /**
     * Converts a {@link PaymentRequest} to a {@link PawaPayDepositRequest} for the v2 API.
     *
     * <p>The {@code transactionId} is converted to a deterministic UUID when it is not
     * already a valid UUID — PawaPay requires UUID format for {@code depositId}.
     *
     * <p>The phone number region is inferred automatically from the E.164 country prefix.
     *
     * @param request the normalized payment request
     * @return the PawaPay v2 deposit request DTO
     * @throws InvalidPaymentRequestException if the phone number is invalid or the
     *                                        correspondent cannot be resolved
     */
    public static PawaPayDepositRequest toDepositRequest(PaymentRequest request) {
        ParsedPhoneNumber phone = PhoneNumberParser.parse(request.phoneNumber());
        String correspondent = resolveCorrespondent(request.metadata(), phone);

        PawaPayPayer payer = new PawaPayPayer("MMO",
                new PawaPayAccountDetails(phone.e164Digits(), correspondent));

        return new PawaPayDepositRequest(
                toUuid(request.transactionId()),
                formatAmount(request.amount().amount()),
                request.amount().currency(),
                payer,
                request.transactionId(),
                truncate(request.description())
        );
    }

    /**
     * Converts a {@link PayoutRequest} to a {@link PawaPayPayoutRequest} for the v2 API.
     *
     * @param request the normalized payout request
     * @return the PawaPay v2 payout request DTO
     * @throws InvalidPaymentRequestException if the phone number is invalid or the
     *                                        correspondent cannot be resolved
     */
    public static PawaPayPayoutRequest toPayoutRequest(PayoutRequest request) {
        ParsedPhoneNumber phone = PhoneNumberParser.parse(request.recipientPhone());
        String correspondent = resolveCorrespondent(request.metadata(), phone);

        PawaPayRecipient recipient = new PawaPayRecipient(
                "MSISDN", new PawaPayRecipientAddress(phone.e164Digits()));

        return new PawaPayPayoutRequest(
                toUuid(request.payoutId()),
                formatAmount(request.amount().amount()),
                request.amount().currency(),
                correspondent,
                recipient,
                Instant.now().toString(),
                truncate(request.description())
        );
    }

    /**
     * Converts a {@link RefundRequest} to a {@link PawaPayRefundRequest}.
     *
     * @param request the normalized refund request
     * @return the PawaPay refund request DTO
     */
    public static PawaPayRefundRequest toRefundRequest(RefundRequest request) {
        return new PawaPayRefundRequest(
                toUuid(request.transactionId()),
                toUuid(request.originalTransactionId()),
                formatAmount(request.amount().amount()),
                request.amount().currency()
        );
    }

    private static String resolveCorrespondent(Map<String, String> metadata, ParsedPhoneNumber phone) {
        return Optional.ofNullable(metadata.get(CORRESPONDENT_KEY))
                .filter(Predicate.not(String::isBlank))
                .orElseGet(() -> pawapayCorrespondent(
                        OperatorDetector.detect(phone).orElseThrow(() ->
                                new InvalidPaymentRequestException(
                                        "Cannot determine PawaPay correspondent for phone '%s' (country=%s) — "
                                                .formatted(phone.e164(), phone.countryCode())
                                                + "set metadata key '%s' explicitly (e.g. MTN_MOMO_CMR)"
                                                .formatted(CORRESPONDENT_KEY))),
                        phone.countryCode()));
    }

    private static String pawapayCorrespondent(MobileOperator operator, String alpha2) {
        String country = ALPHA2_TO_ALPHA3.getOrDefault(alpha2, alpha2);
        return switch (operator) {
            case MTN     -> "MTN_MOMO_" + country;
            case ORANGE  -> "ORANGE_" + country;
            case AIRTEL  -> "AIRTEL_" + country;
            case MOOV    -> "MOOV_" + country;
            case WAVE    -> "WAVE_" + country;
            case VODACOM -> "VODACOM_" + country;
            case VODAFONE -> "VODAFONE_" + country;
            case TIGO    -> "TIGO_" + country;
            case ZAMTEL  -> "ZAMTEL_" + country;
        };
    }

    private static String toUuid(String ref) {
        try {
            return UUID.fromString(ref).toString();
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(ref.getBytes()).toString();
        }
    }

    /**
     * Formats a {@link BigDecimal} amount as a plain string conforming to PawaPay's
     * amount regex: {@code ^([0]|([1-9][0-9]{0,17}))([.][0-9]{0,3}[1-9])?$}.
     *
     * <p>Strips trailing decimal zeros so {@code 5000.00} becomes {@code "5000"} and
     * {@code 5.50} becomes {@code "5.5"}. Avoids scientific notation via
     * {@code toPlainString()}.
     */
    private static String formatAmount(BigDecimal amount) {
        return amount.stripTrailingZeros().toPlainString();
    }

    private static String truncate(String s) {
        return s == null ? null
                : s.length() <= CUSTOMER_MESSAGE_MAX_LEN ? s
                : s.substring(0, CUSTOMER_MESSAGE_MAX_LEN);
    }

    private static final Map<String, String> ALPHA2_TO_ALPHA3 = Map.ofEntries(
            Map.entry("CM", "CMR"), Map.entry("GH", "GHA"),
            Map.entry("SN", "SEN"), Map.entry("CI", "CIV"),
            Map.entry("BF", "BFA"), Map.entry("ML", "MLI"),
            Map.entry("NE", "NER"), Map.entry("TG", "TGO"),
            Map.entry("BJ", "BEN"), Map.entry("CD", "COD"),
            Map.entry("TZ", "TZA"), Map.entry("UG", "UGA"),
            Map.entry("KE", "KEN"), Map.entry("RW", "RWA"),
            Map.entry("ZM", "ZMB"), Map.entry("MZ", "MOZ"),
            Map.entry("MW", "MWI"), Map.entry("MG", "MDG"),
            Map.entry("NG", "NGA")
    );
}

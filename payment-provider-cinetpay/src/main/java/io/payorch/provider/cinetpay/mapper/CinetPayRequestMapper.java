package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.model.PaymentRequest;

import java.util.List;
import io.payorch.core.model.PayoutRequest;
import io.payorch.provider.cinetpay.dto.CinetPayCheckRequest;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentRequest;
import io.payorch.provider.cinetpay.dto.CinetPayTransferContact;
import io.payorch.provider.cinetpay.dto.CinetPayTransferRequest;

/**
 * Pure mapping functions from PayOrch request models to CinetPay request DTOs.
 *
 * <p>No network calls, no side effects, no mutable state.
 *
 * @since 0.1.0
 */
public final class CinetPayRequestMapper {

    /** Metadata key for the CinetPay notify (webhook) URL. */
    public static final String NOTIFY_URL_KEY = "notifyUrl";

    /** Language sent to CinetPay for notification messages. */
    private static final String DEFAULT_LANG = "fr";

    private CinetPayRequestMapper() {
    }

    /**
     * Converts a {@link PaymentRequest} to a {@link CinetPayPaymentRequest}.
     *
     * @param request  the normalized payment request
     * @param apiKey   the CinetPay API key
     * @param siteId   the CinetPay site identifier
     * @return the CinetPay payment request DTO
     */
    public static CinetPayPaymentRequest toPaymentRequest(
            PaymentRequest request, String apiKey, String siteId) {
        String returnUrl = request.returnUrl().orElse("");
        String notifyUrl = request.metadata().getOrDefault(NOTIFY_URL_KEY, "");

        return new CinetPayPaymentRequest(
                apiKey,
                siteId,
                request.transactionId(),
                request.amount().amount().intValue(),
                request.amount().currency(),
                request.description(),
                returnUrl,
                notifyUrl,
                request.phoneNumber(),
                "MOBILE_MONEY"
        );
    }

    /**
     * Converts a {@link PayoutRequest} to a {@link CinetPayTransferRequest}.
     *
     * <p>The phone number is split into country prefix and national number.
     * CinetPay expects the prefix (e.g. {@code "237"}) and the national number separately.
     *
     * @param request the normalized payout request
     * @param apiKey  the CinetPay API key
     * @return the CinetPay transfer request DTO
     */
    public static CinetPayTransferRequest toPayoutRequest(PayoutRequest request, String apiKey) {
        String notifyUrl = request.metadata().getOrDefault(NOTIFY_URL_KEY, "");
        String rawPhone  = request.recipientPhone();

        // E.164 phones start with '+' followed by country code then national number.
        // CinetPay requires them split: prefix="237", phone_number="671000001".
        String[] parts   = splitE164(rawPhone);
        String prefix    = parts[0];
        String national  = parts[1];

        CinetPayTransferContact contact = new CinetPayTransferContact(
                national, prefix,
                request.amount().amount().intValue(),
                request.description(),
                notifyUrl);

        return new CinetPayTransferRequest(apiKey, List.of(contact), DEFAULT_LANG);
    }

    private static String[] splitE164(String e164) {
        // Remove leading '+' then try to identify prefix by known country codes (1-3 digits).
        // Fall back to 3-digit prefix if ambiguous.
        String digits = e164.startsWith("+") ? e164.substring(1) : e164;
        // Use 3-digit prefix for African numbers; the remainder is the national number.
        // For the rare 1-digit (US) or 2-digit prefix, the integrator can override via metadata.
        String prefix  = digits.substring(0, 3);
        String national = digits.substring(3);
        return new String[]{prefix, national};
    }

    /**
     * Builds a status check request for the given transaction.
     *
     * @param transactionId the caller-supplied transaction identifier
     * @param apiKey        the CinetPay API key
     * @param siteId        the CinetPay site identifier
     * @return the CinetPay check request DTO
     */
    public static CinetPayCheckRequest toCheckRequest(
            String transactionId, String apiKey, String siteId) {
        return new CinetPayCheckRequest(apiKey, siteId, transactionId);
    }
}

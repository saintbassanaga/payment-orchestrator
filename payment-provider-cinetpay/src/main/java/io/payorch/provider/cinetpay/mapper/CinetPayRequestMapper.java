package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.model.PaymentRequest;
import io.payorch.provider.cinetpay.dto.CinetPayCheckRequest;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentRequest;

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

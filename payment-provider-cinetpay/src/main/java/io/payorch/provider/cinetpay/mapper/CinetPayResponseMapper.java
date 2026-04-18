package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.provider.cinetpay.dto.CinetPayCheckData;
import io.payorch.provider.cinetpay.dto.CinetPayCheckResponse;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Pure mapping functions from CinetPay response DTOs to PayOrch result models.
 *
 * <p>Status translation from CinetPay strings to {@link PaymentStatus} lives exclusively here.
 *
 * @since 0.1.0
 */
public final class CinetPayResponseMapper {

    static final String PROVIDER_NAME = "cinetpay";

    private CinetPayResponseMapper() {
    }

    /**
     * Converts a {@link CinetPayPaymentResponse} to a {@link PaymentResult}.
     *
     * @param response      the CinetPay payment initiation response
     * @param transactionId the caller-supplied transaction identifier
     * @param amount        the requested amount
     * @return the normalized payment result
     */
    public static PaymentResult toPaymentResult(
            CinetPayPaymentResponse response, String transactionId, Money amount) {
        String token = response.data() != null ? response.data().paymentToken() : transactionId;
        return new PaymentResult(
                transactionId,
                token != null ? token : transactionId,
                PaymentStatus.INITIATED,
                amount,
                PROVIDER_NAME,
                Optional.ofNullable(response.data() != null ? response.data().paymentUrl() : null),
                Instant.now(),
                Map.of()
        );
    }

    /**
     * Converts a {@link CinetPayCheckResponse} to a {@link PaymentResult}.
     *
     * @param response      the CinetPay status check response
     * @param transactionId the caller-supplied transaction identifier
     * @param fallbackMoney the amount to use if the response omits amount/currency
     * @return the normalized payment result
     */
    public static PaymentResult toStatusResult(
            CinetPayCheckResponse response, String transactionId, Money fallbackMoney) {
        CinetPayCheckData data = response.data();
        Money money = resolveCheckMoney(data, fallbackMoney);
        PaymentStatus status = data != null ? mapStatus(data.status()) : PaymentStatus.PENDING;
        String providerTxId = data != null && data.transactionId() != null
                ? data.transactionId() : transactionId;

        return new PaymentResult(
                transactionId,
                providerTxId,
                status,
                money,
                PROVIDER_NAME,
                Optional.empty(),
                Instant.now(),
                Map.of()
        );
    }

    /**
     * Converts a {@link CinetPayCheckData} webhook payload to a {@link PaymentEvent}.
     *
     * @param data          the CinetPay check data parsed from the webhook
     * @param transactionId the caller-supplied transaction identifier
     * @param fallbackMoney the amount to use if the data omits amount/currency
     * @return the normalized payment event
     */
    public static PaymentEvent toPaymentEvent(
            CinetPayCheckData data, String transactionId, Money fallbackMoney) {
        Money money = resolveCheckMoney(data, fallbackMoney);
        return new PaymentEvent(
                transactionId,
                data.transactionId() != null ? data.transactionId() : transactionId,
                mapStatus(data.status()),
                money,
                PROVIDER_NAME,
                Instant.now(),
                Map.of()
        );
    }

    private static PaymentStatus mapStatus(String cinetPayStatus) {
        if (cinetPayStatus == null) {
            return PaymentStatus.PENDING;
        }
        return switch (cinetPayStatus) {
            case "CREATED"   -> PaymentStatus.INITIATED;
            case "PENDING"   -> PaymentStatus.PENDING;
            case "ACCEPTED"  -> PaymentStatus.SUCCESS;
            case "REFUSED"   -> PaymentStatus.FAILED;
            case "CANCELLED" -> PaymentStatus.CANCELLED;
            case "EXPIRED"   -> PaymentStatus.EXPIRED;
            default -> throw new ProviderUnavailableException(
                    "CinetPay returned unknown status '%s' — adapter update required"
                            .formatted(cinetPayStatus));
        };
    }

    private static Money resolveCheckMoney(CinetPayCheckData data, Money fallback) {
        if (data != null && data.amount() != null && data.currencyCode() != null) {
            return Money.of(new BigDecimal(data.amount()), data.currencyCode());
        }
        return fallback;
    }
}
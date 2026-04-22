package io.payorch.provider.cinetpay.mapper;

import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.PayoutResult;
import io.payorch.provider.cinetpay.dto.CinetPayCheckData;
import io.payorch.provider.cinetpay.dto.CinetPayCheckResponse;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentResponse;
import io.payorch.provider.cinetpay.dto.CinetPayTransferResponse;
import io.payorch.provider.cinetpay.dto.CinetPayWebhookPayload;

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
        Optional<String> providerRef = Optional.ofNullable(response.data())
                .map(d -> d.paymentToken() != null ? d.paymentToken() : transactionId);
        Optional<String> paymentUrl = Optional.ofNullable(response.data())
                .flatMap(d -> Optional.ofNullable(d.paymentUrl()));
        return new PaymentResult(
                transactionId,
                providerRef.orElse(transactionId),
                PaymentStatus.INITIATED,
                amount,
                PROVIDER_NAME,
                paymentUrl,
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
        PaymentStatus status = Optional.ofNullable(data)
                .map(d -> mapStatus(d.status()))
                .orElse(PaymentStatus.PENDING);
        String providerTxId = Optional.ofNullable(data)
                .map(CinetPayCheckData::transactionId)
                .orElse(transactionId);

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
     * Converts a {@link CinetPayTransferResponse} to a {@link PayoutResult}.
     *
     * <p>CinetPay transfer always returns INITIATED on success — the final status is
     * delivered asynchronously via the notify URL webhook.
     *
     * @param response the CinetPay transfer response
     * @param payoutId the caller-supplied payout identifier
     * @param amount   the disbursed amount
     * @return the normalized payout result
     */
    public static PayoutResult toPayoutResult(
            CinetPayTransferResponse response, String payoutId, Money amount) {
        String providerPayoutId = response.lot() != null ? response.lot() : payoutId;
        PaymentStatus status = "0".equals(response.code())
                ? PaymentStatus.INITIATED
                : PaymentStatus.FAILED;
        return new PayoutResult(
                payoutId,
                providerPayoutId,
                status,
                amount,
                PROVIDER_NAME,
                Optional.ofNullable(response.message()),
                Instant.now()
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

    /**
     * Converts a {@link CinetPayWebhookPayload} to a {@link PaymentEvent}.
     *
     * <p>CinetPay uses {@code cpm_result = "00"} for accepted payments.
     *
     * @param payload       the CinetPay webhook notification payload
     * @param fallbackMoney the amount to use if the payload omits amount/currency
     * @return the normalized payment event
     */
    public static PaymentEvent toPaymentEvent(CinetPayWebhookPayload payload, Money fallbackMoney) {
        Money money = resolveWebhookMoney(payload, fallbackMoney);
        String transactionId = Optional.ofNullable(payload.transactionId()).orElse("");
        return new PaymentEvent(
                transactionId,
                transactionId,
                mapWebhookStatus(payload.status()),
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
        return Optional.ofNullable(data)
                .filter(d -> d.amount() != null && d.currencyCode() != null)
                .map(d -> Money.of(new BigDecimal(d.amount()), d.currencyCode()))
                .orElse(fallback);
    }

    private static Money resolveWebhookMoney(CinetPayWebhookPayload payload, Money fallback) {
        return Optional.ofNullable(payload.amount())
                .filter(a -> payload.currencyCode() != null)
                .map(a -> Money.of(new BigDecimal(a), payload.currencyCode()))
                .orElse(fallback);
    }

    /** CinetPay webhook status: {@code "00"} = success, anything else = failed. */
    private static PaymentStatus mapWebhookStatus(String cpmResult) {
        return "00".equals(cpmResult) ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
    }
}

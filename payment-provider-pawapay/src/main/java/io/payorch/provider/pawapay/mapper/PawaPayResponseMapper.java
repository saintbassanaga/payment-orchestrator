package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.RefundResult;
import io.payorch.provider.pawapay.dto.PawaPayDepositResponse;
import io.payorch.provider.pawapay.dto.PawaPayRefundResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * Pure mapping functions from PawaPay response DTOs to PayOrch result models.
 *
 * <p>This class contains no network calls, no side effects, and no mutable state.
 * Status translation from PawaPay strings to {@link PaymentStatus} lives exclusively here.
 *
 * @since 0.1.0
 */
public final class PawaPayResponseMapper {

    static final String PROVIDER_NAME = "pawapay";

    private PawaPayResponseMapper() {
    }

    /**
     * Converts a {@link PawaPayDepositResponse} to a {@link PaymentResult}.
     *
     * @param response      the PawaPay deposit response
     * @param transactionId the caller-supplied transaction identifier
     * @param fallbackMoney the money to use when the response omits amount/currency
     * @return the normalized payment result
     */
    public static PaymentResult toPaymentResult(
            PawaPayDepositResponse response, String transactionId, Money fallbackMoney) {
        Money money = resolveDepositMoney(response, fallbackMoney);
        return new PaymentResult(
                transactionId,
                response.depositId(),
                mapDepositStatus(response.status()),
                money,
                PROVIDER_NAME,
                Optional.empty(),
                Instant.now(),
                Map.of()
        );
    }

    /**
     * Converts a {@link PawaPayRefundResponse} to a {@link RefundResult}.
     *
     * @param response      the PawaPay refund response
     * @param fallbackMoney the money to use when the response omits amount/currency
     * @return the normalized refund result
     */
    public static RefundResult toRefundResult(PawaPayRefundResponse response, Money fallbackMoney) {
        Money money = resolveRefundMoney(response, fallbackMoney);
        return new RefundResult(
                response.refundId(),
                response.depositId(),
                mapRefundStatus(response.status()),
                money,
                PROVIDER_NAME,
                Optional.empty(),
                Instant.now()
        );
    }

    /**
     * Converts a {@link PawaPayDepositResponse} (from a webhook payload) to a {@link PaymentEvent}.
     *
     * @param response      the PawaPay deposit response parsed from the webhook body
     * @param transactionId the caller-supplied transaction identifier
     * @param fallbackMoney the money to use when the response omits amount/currency
     * @return the normalized payment event
     */
    public static PaymentEvent toPaymentEvent(
            PawaPayDepositResponse response, String transactionId, Money fallbackMoney) {
        Money money = resolveDepositMoney(response, fallbackMoney);
        return new PaymentEvent(
                transactionId,
                response.depositId(),
                mapDepositStatus(response.status()),
                money,
                PROVIDER_NAME,
                Instant.now(),
                Map.of()
        );
    }

    private static PaymentStatus mapDepositStatus(String pawaPayStatus) {
        return switch (pawaPayStatus) {
            case "ACCEPTED"  -> PaymentStatus.INITIATED;
            case "SUBMITTED" -> PaymentStatus.PENDING;
            case "COMPLETED" -> PaymentStatus.SUCCESS;
            case "FAILED"    -> PaymentStatus.FAILED;
            case "REJECTED"  -> PaymentStatus.CANCELLED;
            case "EXPIRED"   -> PaymentStatus.EXPIRED;
            default -> throw new ProviderUnavailableException(
                    "PawaPay returned unknown deposit status '%s' — adapter update required"
                            .formatted(pawaPayStatus));
        };
    }

    private static PaymentStatus mapRefundStatus(String pawaPayStatus) {
        return switch (pawaPayStatus) {
            case "ACCEPTED"  -> PaymentStatus.INITIATED;
            case "SUBMITTED" -> PaymentStatus.PENDING;
            case "COMPLETED" -> PaymentStatus.REFUNDED;
            case "FAILED"    -> PaymentStatus.FAILED;
            case "REJECTED"  -> PaymentStatus.CANCELLED;
            default -> throw new ProviderUnavailableException(
                    "PawaPay returned unknown refund status '%s' — adapter update required"
                            .formatted(pawaPayStatus));
        };
    }

    private static Money resolveDepositMoney(PawaPayDepositResponse response, Money fallback) {
        if (response.requestedAmount() != null && response.currency() != null) {
            return Money.of(new BigDecimal(response.requestedAmount()), response.currency());
        }
        return fallback;
    }

    private static Money resolveRefundMoney(PawaPayRefundResponse response, Money fallback) {
        if (response.amount() != null && response.currency() != null) {
            return Money.of(new BigDecimal(response.amount()), response.currency());
        }
        return fallback;
    }
}

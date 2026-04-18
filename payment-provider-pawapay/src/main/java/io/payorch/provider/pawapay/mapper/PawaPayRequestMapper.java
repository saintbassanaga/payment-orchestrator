package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.RefundRequest;
import io.payorch.provider.pawapay.dto.PawaPayAddress;
import io.payorch.provider.pawapay.dto.PawaPayDepositRequest;
import io.payorch.provider.pawapay.dto.PawaPayPayer;
import io.payorch.provider.pawapay.dto.PawaPayRefundRequest;

import java.time.Instant;

/**
 * Pure mapping functions from PayOrch request models to PawaPay request DTOs.
 *
 * <p>This class contains no network calls, no side effects, and no mutable state.
 * All methods are stateless transformations.
 *
 * @since 0.1.0
 */
public final class PawaPayRequestMapper {

    /**
     * Key in {@link PaymentRequest#metadata()} identifying the PawaPay correspondent
     * (mobile money operator), e.g. {@code "MTN_MOMO_CMR"}.
     */
    public static final String CORRESPONDENT_KEY = "correspondent";

    private PawaPayRequestMapper() {
    }

    /**
     * Converts a {@link PaymentRequest} to a {@link PawaPayDepositRequest}.
     *
     * <p>The {@code transactionId} is used directly as PawaPay's {@code depositId},
     * enabling status polling without a separate ID mapping layer.
     *
     * @param request the normalized payment request
     * @return the PawaPay deposit request DTO
     * @throws InvalidPaymentRequestException if the {@code correspondent} metadata key is absent
     */
    public static PawaPayDepositRequest toDepositRequest(PaymentRequest request) {
        String correspondent = request.metadata().get(CORRESPONDENT_KEY);
        if (correspondent == null || correspondent.isBlank()) {
            throw new InvalidPaymentRequestException(
                    "Missing required metadata key '%s' for PawaPay — "
                            .formatted(CORRESPONDENT_KEY)
                            + "provide the mobile operator code (e.g. MTN_MOMO_CMR)");
        }

        String phoneNumber = normalizePhone(request.phoneNumber());
        PawaPayPayer payer = new PawaPayPayer("MSISDN", new PawaPayAddress(phoneNumber));

        return new PawaPayDepositRequest(
                request.transactionId(),
                request.amount().amount().toPlainString(),
                request.amount().currency(),
                correspondent,
                payer,
                Instant.now().toString(),
                request.description()
        );
    }

    /**
     * Converts a {@link RefundRequest} to a {@link PawaPayRefundRequest}.
     *
     * <p>The {@code originalTransactionId} must be PawaPay's deposit ID, which is
     * the {@code transactionId} used when initiating the original payment.
     *
     * @param request the normalized refund request
     * @return the PawaPay refund request DTO
     */
    public static PawaPayRefundRequest toRefundRequest(RefundRequest request) {
        return new PawaPayRefundRequest(
                request.transactionId(),
                request.originalTransactionId(),
                request.amount().amount().toPlainString(),
                request.amount().currency()
        );
    }

    private static String normalizePhone(String phoneNumber) {
        return phoneNumber.startsWith("+")
                ? phoneNumber.substring(1)
                : phoneNumber;
    }
}

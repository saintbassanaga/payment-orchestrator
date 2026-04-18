package io.payorch.provider.pawapay.mapper;

import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.RefundRequest;
import io.payorch.provider.pawapay.dto.PawaPayAccountDetails;
import io.payorch.provider.pawapay.dto.PawaPayDepositRequest;
import io.payorch.provider.pawapay.dto.PawaPayPayer;
import io.payorch.provider.pawapay.dto.PawaPayRefundRequest;

import java.util.UUID;

/**
 * Pure mapping functions from PayOrch request models to PawaPay v2 request DTOs.
 *
 * <p>No network calls, no side effects, no mutable state.
 *
 * @since 0.1.0
 */
public final class PawaPayRequestMapper {

    /** Metadata key for the PawaPay correspondent (mobile operator), e.g. {@code "MTN_MOMO_CMR"}. */
    public static final String CORRESPONDENT_KEY = "correspondent";

    private static final int CUSTOMER_MESSAGE_MAX_LEN = 22;

    private PawaPayRequestMapper() {
    }

    /**
     * Converts a {@link PaymentRequest} to a {@link PawaPayDepositRequest} for the v2 API.
     *
     * <p>The {@code transactionId} is converted to a deterministic UUID v3 when it is not
     * already a valid UUID — PawaPay requires UUID format for {@code depositId}.
     * The correspondent moves into {@code payer.accountDetails.provider} as required by v2.
     *
     * @param request the normalized payment request
     * @return the PawaPay v2 deposit request DTO
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

        String depositId = toUuid(request.transactionId());
        String phoneNumber = normalizePhone(request.phoneNumber());
        PawaPayPayer payer = new PawaPayPayer("MMO",
                new PawaPayAccountDetails(phoneNumber, correspondent));

        return new PawaPayDepositRequest(
                depositId,
                request.amount().amount().toPlainString(),
                request.amount().currency(),
                payer,
                request.transactionId(),
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
                request.amount().amount().toPlainString(),
                request.amount().currency()
        );
    }

    private static String toUuid(String ref) {
        try {
            return UUID.fromString(ref).toString();
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(ref.getBytes()).toString();
        }
    }

    private static String normalizePhone(String phoneNumber) {
        return phoneNumber.startsWith("+") ? phoneNumber.substring(1) : phoneNumber;
    }

    private static String truncate(String s) {
        return s == null ? null
                : s.length() <= CUSTOMER_MESSAGE_MAX_LEN ? s
                : s.substring(0, CUSTOMER_MESSAGE_MAX_LEN);
    }
}

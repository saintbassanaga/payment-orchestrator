package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for initiating a payout (disbursement) via the PawaPay v2 API.
 *
 * @param payoutId             unique identifier for this payout (UUID format)
 * @param amount               the amount as a plain string (no trailing zeros)
 * @param currency             the ISO 4217 currency code
 * @param correspondent        the mobile operator code (e.g. {@code "MTN_MOMO_CMR"})
 * @param recipient            the recipient's MSISDN address
 * @param customerTimestamp    the ISO 8601 timestamp of the payout initiation
 * @param statementDescription a short description shown to the recipient (max 22 chars)
 * @since 0.1.0
 */
public record PawaPayPayoutRequest(
        @JsonProperty("payoutId")             String payoutId,
        @JsonProperty("amount")               String amount,
        @JsonProperty("currency")             String currency,
        @JsonProperty("correspondent")        String correspondent,
        @JsonProperty("recipient")            PawaPayRecipient recipient,
        @JsonProperty("customerTimestamp")    String customerTimestamp,
        @JsonProperty("statementDescription") String statementDescription) {
}

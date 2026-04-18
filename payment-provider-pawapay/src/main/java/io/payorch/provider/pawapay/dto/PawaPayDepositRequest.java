package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for initiating a deposit (payment collection) via the PawaPay API.
 *
 * @param depositId            unique identifier for this deposit (caller-supplied)
 * @param amount               the amount as a plain string (e.g. {@code "5000"})
 * @param currency             the ISO 4217 currency code
 * @param correspondent        the mobile money operator code (e.g. {@code "MTN_MOMO_CMR"})
 * @param payer                the payer's MSISDN details
 * @param customerTimestamp    the ISO 8601 timestamp of the customer action
 * @param statementDescription a short description shown on the customer's statement
 * @since 0.1.0
 */
public record PawaPayDepositRequest(
        @JsonProperty("depositId") String depositId,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("correspondent") String correspondent,
        @JsonProperty("payer") PawaPayPayer payer,
        @JsonProperty("customerTimestamp") String customerTimestamp,
        @JsonProperty("statementDescription") String statementDescription) {
}

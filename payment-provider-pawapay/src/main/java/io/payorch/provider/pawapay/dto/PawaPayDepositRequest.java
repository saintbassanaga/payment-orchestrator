package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for initiating a deposit (payment collection) via the PawaPay v2 API.
 *
 * <p>In v2, {@code correspondent} moves into {@code payer.accountDetails.provider}.
 * {@code customerMessage} replaces {@code statementDescription} and is capped at
 * 22 characters by PawaPay.
 *
 * @param depositId         unique UUID for this deposit
 * @param amount            the amount as a plain string (e.g. {@code "5000"})
 * @param currency          the ISO 4217 currency code
 * @param payer             the payer's MMO account details (phone + operator)
 * @param clientReferenceId the caller's internal reference stored by PawaPay
 * @param customerMessage   short description shown to the customer (max 22 chars)
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PawaPayDepositRequest(
        @JsonProperty("depositId") String depositId,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("payer") PawaPayPayer payer,
        @JsonProperty("clientReferenceId") String clientReferenceId,
        @JsonProperty("customerMessage") String customerMessage) {
}

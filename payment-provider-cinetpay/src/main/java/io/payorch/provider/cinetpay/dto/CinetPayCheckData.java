package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data payload returned by CinetPay on a payment status check.
 *
 * @param transactionId the transaction identifier
 * @param amount        the amount as a string
 * @param currencyCode  the ISO 4217 currency code
 * @param status        the CinetPay status string
 * @param createdDate   the payment creation date
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayCheckData(
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency_code") String currencyCode,
        @JsonProperty("status") String status,
        @JsonProperty("created_date") String createdDate) {
}
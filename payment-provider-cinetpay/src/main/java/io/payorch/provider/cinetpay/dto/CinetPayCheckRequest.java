package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for checking a payment status via the CinetPay v2 API.
 *
 * @param apikey        the CinetPay API key
 * @param siteId        the CinetPay site identifier
 * @param transactionId the transaction to look up
 * @since 0.1.0
 */
public record CinetPayCheckRequest(
        @JsonProperty("apikey") String apikey,
        @JsonProperty("site_id") String siteId,
        @JsonProperty("transaction_id") String transactionId) {
}

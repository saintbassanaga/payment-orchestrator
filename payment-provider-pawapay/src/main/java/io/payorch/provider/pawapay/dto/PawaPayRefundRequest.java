package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for initiating a refund via the PawaPay API.
 *
 * @param refundId  unique identifier for this refund (caller-supplied)
 * @param depositId the PawaPay deposit ID to refund
 * @param amount    the amount to refund as a plain string
 * @param currency  the ISO 4217 currency code
 * @since 0.1.0
 */
public record PawaPayRefundRequest(
        @JsonProperty("refundId") String refundId,
        @JsonProperty("depositId") String depositId,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency) {
}
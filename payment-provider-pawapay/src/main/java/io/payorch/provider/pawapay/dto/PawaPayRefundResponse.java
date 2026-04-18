package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the PawaPay refund response.
 *
 * @param refundId  the PawaPay refund identifier
 * @param depositId the original deposit that was refunded
 * @param status    the refund status (e.g. {@code "ACCEPTED"}, {@code "COMPLETED"})
 * @param amount    the refunded amount
 * @param currency  the currency code
 * @param created   the refund creation timestamp
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PawaPayRefundResponse(
        @JsonProperty("refundId") String refundId,
        @JsonProperty("depositId") String depositId,
        @JsonProperty("status") String status,
        @JsonProperty("amount") String amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("created") String created) {
}
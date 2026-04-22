package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the CinetPay transfer (payout) response.
 *
 * <p>CinetPay returns {@code "0"} in {@code code} for success.
 *
 * @param code    the response code ({@code "0"} = success)
 * @param message the human-readable status message
 * @param lot     the batch identifier assigned to this transfer
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayTransferResponse(
        @JsonProperty("code")    String code,
        @JsonProperty("message") String message,
        @JsonProperty("lot")     String lot) {
}

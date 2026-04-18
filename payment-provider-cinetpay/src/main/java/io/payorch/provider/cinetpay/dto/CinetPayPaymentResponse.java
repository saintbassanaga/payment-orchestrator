package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level response wrapper from CinetPay for payment initiation.
 *
 * @param code    CinetPay response code ({@code "201"} = created)
 * @param message human-readable status label
 * @param data    the payment details (token + URL)
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayPaymentResponse(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("data") CinetPayPaymentData data) {
}

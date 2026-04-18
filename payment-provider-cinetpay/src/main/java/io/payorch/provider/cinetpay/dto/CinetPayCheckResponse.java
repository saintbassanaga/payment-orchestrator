package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Top-level response wrapper from CinetPay for payment status checks.
 *
 * @param code    CinetPay response code ({@code "00"} = success)
 * @param message human-readable status label
 * @param data    the payment status details
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayCheckResponse(
        @JsonProperty("code") String code,
        @JsonProperty("message") String message,
        @JsonProperty("data") CinetPayCheckData data) {
}
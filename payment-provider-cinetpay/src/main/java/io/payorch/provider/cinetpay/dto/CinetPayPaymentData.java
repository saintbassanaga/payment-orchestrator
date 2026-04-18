package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Data payload returned by CinetPay on payment initiation.
 *
 * @param paymentToken the token identifying this payment session
 * @param paymentUrl   the hosted checkout URL to redirect the payer to
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayPaymentData(
        @JsonProperty("payment_token") String paymentToken,
        @JsonProperty("payment_url") String paymentUrl) {
}

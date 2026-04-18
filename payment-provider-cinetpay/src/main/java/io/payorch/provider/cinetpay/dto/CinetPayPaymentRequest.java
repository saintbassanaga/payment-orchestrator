package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for initiating a payment via the CinetPay v2 API.
 *
 * @param apikey            the CinetPay API key (sent in body)
 * @param siteId            the CinetPay site identifier
 * @param transactionId     unique identifier for this transaction
 * @param amount            the amount as an integer
 * @param currency          the ISO 4217 currency code
 * @param description       a short description of the payment
 * @param returnUrl         the URL to redirect the payer after completion
 * @param notifyUrl         the URL for asynchronous webhook notifications
 * @param customerPhone     the customer's phone number
 * @param channels          payment channels to enable (e.g. {@code "MOBILE_MONEY"})
 * @since 0.1.0
 */
public record CinetPayPaymentRequest(
        @JsonProperty("apikey") String apikey,
        @JsonProperty("site_id") String siteId,
        @JsonProperty("transaction_id") String transactionId,
        @JsonProperty("amount") int amount,
        @JsonProperty("currency") String currency,
        @JsonProperty("description") String description,
        @JsonProperty("return_url") String returnUrl,
        @JsonProperty("notify_url") String notifyUrl,
        @JsonProperty("customer_phone_number") String customerPhone,
        @JsonProperty("channels") String channels) {
}

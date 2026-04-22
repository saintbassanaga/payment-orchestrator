package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Raw CinetPay webhook notification payload.
 *
 * <p>CinetPay POSTs this to the {@code notify_url} after a payment attempt.
 * Field names use the {@code cpm_} prefix specific to the webhook format,
 * which differs from the status-check API response.
 *
 * @param transactionId the CinetPay transaction identifier ({@code cpm_trans_id})
 * @param amount        the payment amount as a string ({@code cpm_amount})
 * @param currencyCode  the ISO 4217 currency code ({@code cpm_currency})
 * @param status        the payment status ({@code cpm_result}) — {@code "00"} = accepted
 * @param paymentConfig the payment channel/config ({@code cpm_payment_config})
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CinetPayWebhookPayload(
        @JsonProperty("cpm_trans_id")       String transactionId,
        @JsonProperty("cpm_amount")         String amount,
        @JsonProperty("cpm_currency")       String currencyCode,
        @JsonProperty("cpm_result")         String status,
        @JsonProperty("cpm_payment_config") String paymentConfig) {
}
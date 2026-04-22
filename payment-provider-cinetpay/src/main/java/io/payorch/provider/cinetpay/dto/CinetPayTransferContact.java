package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single transfer recipient entry in a CinetPay bulk transfer request.
 *
 * @param phoneNumber the recipient's national phone number (without country prefix)
 * @param prefix      the country calling code (e.g. {@code "237"} for Cameroon)
 * @param amount      the amount to transfer (integer)
 * @param description a short description shown to the recipient
 * @param notifUrl    the webhook URL for transfer status notifications (empty if none)
 * @since 0.1.0
 */
public record CinetPayTransferContact(
        @JsonProperty("phone_number") String phoneNumber,
        @JsonProperty("prefix")       String prefix,
        @JsonProperty("amount")       int amount,
        @JsonProperty("description")  String description,
        @JsonProperty("notifUrl")     String notifUrl) {
}

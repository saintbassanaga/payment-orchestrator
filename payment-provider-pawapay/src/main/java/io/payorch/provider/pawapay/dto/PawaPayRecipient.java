package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The recipient of a PawaPay v2 payout.
 *
 * @param type    always {@code "MSISDN"} for mobile-money payouts
 * @param address the recipient's phone number address
 * @since 0.1.0
 */
public record PawaPayRecipient(
        @JsonProperty("type") String type,
        @JsonProperty("address") PawaPayRecipientAddress address) {
}

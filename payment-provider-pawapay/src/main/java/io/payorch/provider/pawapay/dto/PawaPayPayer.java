package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PawaPay payer object describing who is making the payment.
 *
 * @param type    always {@code "MSISDN"} for mobile money
 * @param address the payer's phone address
 * @since 0.1.0
 */
public record PawaPayPayer(
        @JsonProperty("type") String type,
        @JsonProperty("address") PawaPayAddress address) {
}

package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PawaPay v2 account details identifying the payer's phone and mobile operator.
 *
 * @param phoneNumber the E.164 phone number without the leading '+' (e.g. {@code "237600000000"})
 * @param provider    the PawaPay correspondent code (e.g. {@code "MTN_MOMO_CMR"})
 * @since 0.1.0
 */
public record PawaPayAccountDetails(
        @JsonProperty("phoneNumber") String phoneNumber,
        @JsonProperty("provider") String provider) {
}

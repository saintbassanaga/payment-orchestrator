package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PawaPay v2 payer object embedded in a deposit request.
 *
 * @param type           always {@code "MMO"} for mobile money in the v2 API
 * @param accountDetails the payer's phone number and operator details
 * @since 0.1.0
 */
public record PawaPayPayer(
        @JsonProperty("type") String type,
        @JsonProperty("accountDetails") PawaPayAccountDetails accountDetails) {
}

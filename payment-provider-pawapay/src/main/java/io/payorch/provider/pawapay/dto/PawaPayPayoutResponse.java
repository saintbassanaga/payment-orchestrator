package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the PawaPay v2 payout response.
 *
 * @param payoutId      the PawaPay payout identifier
 * @param status        the payout status (e.g. {@code "ACCEPTED"}, {@code "COMPLETED"})
 * @param amount        the disbursed amount
 * @param currency      the currency code
 * @param correspondent the mobile operator code
 * @param created       the payout creation timestamp
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PawaPayPayoutResponse(
        @JsonProperty("payoutId")      String payoutId,
        @JsonProperty("status")        String status,
        @JsonProperty("amount")        String amount,
        @JsonProperty("currency")      String currency,
        @JsonProperty("correspondent") String correspondent,
        @JsonProperty("created")       String created) {
}

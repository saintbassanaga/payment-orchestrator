package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The MSISDN address of a payout recipient in the PawaPay v2 payout request.
 *
 * @param value the phone number digits without leading {@code +}
 * @since 0.1.0
 */
public record PawaPayRecipientAddress(@JsonProperty("value") String value) {
}

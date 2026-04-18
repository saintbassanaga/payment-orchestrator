package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * PawaPay address object identifying the payer's MSISDN.
 *
 * @param value the phone number in international format without the leading '+'
 * @since 0.1.0
 */
public record PawaPayAddress(@JsonProperty("value") String value) {
}
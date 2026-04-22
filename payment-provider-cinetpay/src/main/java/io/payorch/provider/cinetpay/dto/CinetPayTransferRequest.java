package io.payorch.provider.cinetpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DTO for initiating a bulk transfer payout via the CinetPay transfer API.
 *
 * @param apikey   the CinetPay API key
 * @param data     the list of transfer recipients (one entry per payout)
 * @param lang     the language for notifications ({@code "fr"} or {@code "en"})
 * @since 0.1.0
 */
public record CinetPayTransferRequest(
        @JsonProperty("apikey") String apikey,
        @JsonProperty("data")   List<CinetPayTransferContact> data,
        @JsonProperty("lang")   String lang) {
}

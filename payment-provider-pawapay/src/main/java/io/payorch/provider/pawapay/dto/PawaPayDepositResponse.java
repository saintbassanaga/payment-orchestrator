package io.payorch.provider.pawapay.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO for the PawaPay deposit response (both initiation and status check).
 *
 * <p>Unknown fields from PawaPay are silently ignored to remain compatible
 * with future API additions.
 *
 * @param depositId       the PawaPay deposit identifier
 * @param status          the deposit status (e.g. {@code "ACCEPTED"}, {@code "COMPLETED"})
 * @param requestedAmount the originally requested amount (null on initiation response)
 * @param currency        the currency code (null on initiation response)
 * @param correspondent   the mobile operator code (null on initiation response)
 * @param payer           the payer details (null on initiation response)
 * @param created         the deposit creation timestamp (null on initiation response)
 * @since 0.1.0
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PawaPayDepositResponse(
        @JsonProperty("depositId") String depositId,
        @JsonProperty("status") String status,
        @JsonProperty("requestedAmount") String requestedAmount,
        @JsonProperty("currency") String currency,
        @JsonProperty("correspondent") String correspondent,
        @JsonProperty("payer") PawaPayPayer payer,
        @JsonProperty("created") String created) {
}
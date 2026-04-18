package io.payorch.provider.pawapay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.payorch.core.exception.ProviderAuthException;
import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.exception.TransactionNotFoundException;
import io.payorch.http.HttpClientConfig;
import io.payorch.http.HttpResponse;
import io.payorch.http.PayOrchHttpClient;
import io.payorch.provider.pawapay.dto.PawaPayDepositRequest;
import io.payorch.provider.pawapay.dto.PawaPayDepositResponse;
import io.payorch.provider.pawapay.dto.PawaPayRefundRequest;
import io.payorch.provider.pawapay.dto.PawaPayRefundResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Low-level HTTP client for the PawaPay API.
 *
 * <p>Handles only raw HTTP communication: serialization, deserialization,
 * authentication headers, and HTTP error mapping. No business logic.
 *
 * @since 0.1.0
 */
final class PawaPayClient {

    private static final String PROVIDER = "pawapay";

    private final PayOrchHttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;
    private final String apiKey;

    /**
     * Constructs a PawaPay client.
     *
     * @param baseUrl the PawaPay API base URL
     * @param apiKey  the Bearer token for authentication
     * @param config  the HTTP client configuration
     */
    PawaPayClient(String baseUrl, String apiKey, HttpClientConfig config) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey must not be null");
        this.http = new PayOrchHttpClient(PROVIDER, config);
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Initiates a deposit (payment collection) with PawaPay.
     *
     * @param request the deposit request DTO
     * @return the PawaPay deposit response
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     * @throws ProviderAuthException        if the API returns 401
     */
    PawaPayDepositResponse initiateDeposit(PawaPayDepositRequest request) {
        String json = serialize(request);
        HttpResponse response = http.post(baseUrl + "/v1/deposits", json, authHeaders());
        checkResponse(response, "initiate deposit");
        return deserialize(response.body(), PawaPayDepositResponse.class);
    }

    /**
     * Retrieves the current status of a deposit.
     *
     * @param depositId the PawaPay deposit ID (same as the caller's transactionId)
     * @return the PawaPay deposit response
     * @throws TransactionNotFoundException if PawaPay returns 404
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     */
    PawaPayDepositResponse getDeposit(String depositId) {
        HttpResponse response = http.get(baseUrl + "/v1/deposits/" + depositId, authHeaders());
        if (response.statusCode() == 404) {
            throw new TransactionNotFoundException(PROVIDER, depositId);
        }
        checkResponse(response, "get deposit");
        return deserialize(response.body(), PawaPayDepositResponse.class);
    }

    /**
     * Initiates a refund for a completed deposit.
     *
     * @param request the refund request DTO
     * @return the PawaPay refund response
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     */
    PawaPayRefundResponse initiateRefund(PawaPayRefundRequest request) {
        String json = serialize(request);
        HttpResponse response = http.post(baseUrl + "/v1/refunds", json, authHeaders());
        checkResponse(response, "initiate refund");
        return deserialize(response.body(), PawaPayRefundResponse.class);
    }

    private Map<String, String> authHeaders() {
        return Map.of("Authorization", "Bearer " + apiKey);
    }

    private void checkResponse(HttpResponse response, String operation) {
        if (response.statusCode() == 401 || response.statusCode() == 403) {
            throw new ProviderAuthException(PROVIDER,
                    "HTTP %d on %s".formatted(response.statusCode(), operation));
        }
        if (response.isServerError()) {
            throw new ProviderUnavailableException(PROVIDER + ": server error "
                    + response.statusCode() + " on " + operation + " — " + response.body());
        }
    }

    private String serialize(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ProviderUnavailableException(
                    "Failed to serialize request for provider '%s'".formatted(PROVIDER), e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new ProviderUnavailableException(
                    "Failed to parse response from provider '%s'".formatted(PROVIDER), e);
        }
    }
}

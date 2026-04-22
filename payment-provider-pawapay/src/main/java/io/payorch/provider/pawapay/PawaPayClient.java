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
import io.payorch.provider.pawapay.dto.PawaPayPayoutRequest;
import io.payorch.provider.pawapay.dto.PawaPayPayoutResponse;
import io.payorch.provider.pawapay.dto.PawaPayRefundRequest;
import io.payorch.provider.pawapay.dto.PawaPayRefundResponse;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
    private final String token;

    /**
     * Constructs a PawaPay client.
     *
     * @param baseUrl the PawaPay API base URL
     * @param token   the Bearer token generated from the PawaPay dashboard
     * @param config  the HTTP client configuration
     */
    PawaPayClient(String baseUrl, String token, HttpClientConfig config) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.token   = Objects.requireNonNull(token, "token must not be null");
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
        HttpResponse response = http.post(baseUrl + "/v2/deposits", json, authHeaders());
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
        HttpResponse response = http.get(baseUrl + "/v2/deposits/" + depositId, authHeaders());
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
        HttpResponse response = http.post(baseUrl + "/v2/refunds", json, authHeaders());
        checkResponse(response, "initiate refund");
        return deserialize(response.body(), PawaPayRefundResponse.class);
    }

    /**
     * Initiates a payout (disbursement) to a recipient's mobile-money account.
     *
     * @param request the payout request DTO
     * @return the PawaPay payout response
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     * @throws ProviderAuthException        if the API returns 401 or 403
     */
    PawaPayPayoutResponse initiatePayout(PawaPayPayoutRequest request) {
        String json = serialize(request);
        HttpResponse response = http.post(baseUrl + "/v2/payouts", json, authHeaders());
        checkResponse(response, "initiate payout");
        return deserialize(response.body(), PawaPayPayoutResponse.class);
    }

    /**
     * Retrieves the current status of a payout.
     *
     * @param payoutId the PawaPay payout ID
     * @return the PawaPay payout response
     * @throws TransactionNotFoundException if PawaPay returns 404
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     */
    PawaPayPayoutResponse getPayout(String payoutId) {
        HttpResponse response = http.get(baseUrl + "/v2/payouts/" + payoutId, authHeaders());
        if (response.statusCode() == 404) {
            throw new TransactionNotFoundException(PROVIDER, payoutId);
        }
        checkResponse(response, "get payout");
        return deserialize(response.body(), PawaPayPayoutResponse.class);
    }

    /**
     * Retrieves the current status of a refund.
     *
     * @param refundId the PawaPay refund ID
     * @return the PawaPay refund response
     * @throws TransactionNotFoundException if PawaPay returns 404
     * @throws ProviderUnavailableException if the API is unreachable or returns a server error
     */
    PawaPayRefundResponse getRefund(String refundId) {
        HttpResponse response = http.get(baseUrl + "/v2/refunds/" + refundId, authHeaders());
        if (response.statusCode() == 404) {
            throw new TransactionNotFoundException(PROVIDER, refundId);
        }
        checkResponse(response, "get refund");
        return deserialize(response.body(), PawaPayRefundResponse.class);
    }

    /**
     * Fetches PawaPay's RSA/EC public key from {@code GET /public-key/http}.
     *
     * <p>The response is a JSON array: {@code [{"id": "...", "key": "<PEM>"}]}.
     * Returns the first key entry. No authentication required per PawaPay docs.
     *
     * @return the parsed public key, or empty if unavailable
     */
    Optional<PublicKey> fetchPublicKey() {
        try {
            HttpResponse response = http.get(baseUrl + "/public-key/http", Map.of());
            if (!response.isSuccessful()) {
                return Optional.empty();
            }
            String pem = extractPemFromResponse(response.body());
            if (pem == null || pem.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(parsePem(pem));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static String extractPemFromResponse(String json) throws Exception {
        // Minimal parse — response is: [{"id":"...","key":"-----BEGIN PUBLIC KEY-----..."}]
        int keyIndex = json.indexOf("\"key\"");
        if (keyIndex < 0) return null;
        int start = json.indexOf('"', keyIndex + 5);
        if (start < 0) return null;
        int end = json.indexOf('"', start + 1);
        if (end < 0) return null;
        return json.substring(start + 1, end).replace("\\n", "\n");
    }

    private static PublicKey parsePem(String pem) throws Exception {
        String b64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                        .replace("-----END PUBLIC KEY-----", "")
                        .replaceAll("\\s+", "");
        byte[] der = Base64.getDecoder().decode(b64);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        try {
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (java.security.spec.InvalidKeySpecException ignored) {
            return KeyFactory.getInstance("EC").generatePublic(spec);
        }
    }

    private Map<String, String> authHeaders() {
        return Map.of("Authorization", "Bearer " + token);
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

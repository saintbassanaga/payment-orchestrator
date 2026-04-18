package io.payorch.provider.cinetpay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.payorch.core.exception.ProviderAuthException;
import io.payorch.core.exception.ProviderUnavailableException;
import io.payorch.core.exception.TransactionNotFoundException;
import io.payorch.http.HttpClientConfig;
import io.payorch.http.HttpResponse;
import io.payorch.http.PayOrchHttpClient;
import io.payorch.provider.cinetpay.dto.CinetPayCheckRequest;
import io.payorch.provider.cinetpay.dto.CinetPayCheckResponse;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentRequest;
import io.payorch.provider.cinetpay.dto.CinetPayPaymentResponse;

import java.util.Map;
import java.util.Objects;

/**
 * Low-level HTTP client for the CinetPay v2 API.
 *
 * <p>Handles only raw HTTP communication: serialization, deserialization,
 * and HTTP error mapping. No business logic.
 *
 * @since 0.1.0
 */
final class CinetPayClient {

    private static final String PROVIDER = "cinetpay";
    private static final String CONTENT_TYPE_JSON = "application/json";

    private final PayOrchHttpClient http;
    private final ObjectMapper mapper;
    private final String baseUrl;

    /**
     * Constructs a CinetPay client.
     *
     * @param baseUrl the CinetPay API base URL
     * @param config  the HTTP client configuration
     */
    CinetPayClient(String baseUrl, HttpClientConfig config) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl must not be null");
        this.http = new PayOrchHttpClient(PROVIDER, config);
        this.mapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    /**
     * Initiates a payment with CinetPay.
     *
     * @param request the payment request DTO (contains credentials in body)
     * @return the CinetPay payment response
     * @throws ProviderUnavailableException if the API returns a server error
     * @throws ProviderAuthException        if the API returns 401 or 403
     */
    CinetPayPaymentResponse initiatePayment(CinetPayPaymentRequest request) {
        String json = serialize(request);
        HttpResponse response = http.post(baseUrl + "/payment", json, jsonHeaders());
        checkResponse(response, "initiate payment");
        return deserialize(response.body(), CinetPayPaymentResponse.class);
    }

    /**
     * Checks the status of an existing payment.
     *
     * @param request the check request DTO
     * @return the CinetPay check response
     * @throws TransactionNotFoundException if CinetPay returns 404
     * @throws ProviderUnavailableException if the API returns a server error
     */
    CinetPayCheckResponse checkPayment(CinetPayCheckRequest request) {
        String json = serialize(request);
        HttpResponse response = http.post(baseUrl + "/payment/check", json, jsonHeaders());
        if (response.statusCode() == 404) {
            throw new TransactionNotFoundException(PROVIDER, request.transactionId());
        }
        checkResponse(response, "check payment");
        return deserialize(response.body(), CinetPayCheckResponse.class);
    }

    private Map<String, String> jsonHeaders() {
        return Map.of("Content-Type", CONTENT_TYPE_JSON);
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

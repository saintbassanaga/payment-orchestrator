package io.payorch.provider.pawapay;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.auto.service.AutoService;
import io.payorch.core.exception.InvalidPaymentRequestException;
import io.payorch.core.exception.WebhookValidationException;
import io.payorch.core.model.Environment;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PayoutRequest;
import io.payorch.core.model.PayoutResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.AbstractOfficialProviderSpi;
import io.payorch.core.spi.PaymentProviderSpi;
import io.payorch.http.HttpClientConfig;
import io.payorch.provider.pawapay.dto.PawaPayDepositResponse;
import io.payorch.provider.pawapay.dto.PawaPayPayoutResponse;
import io.payorch.provider.pawapay.mapper.PawaPayRequestMapper;
import io.payorch.provider.pawapay.mapper.PawaPayResponseMapper;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * PayOrch adapter for the PawaPay mobile money aggregator.
 *
 * <p>Registered automatically via {@link AutoService} for discovery by
 * {@link io.payorch.core.spi.ProviderRegistry#loadFromServiceLoader()}.
 *
 * <p>Required credential keys:
 * <ul>
 *   <li>{@code token} — PawaPay Bearer token (generated from the PawaPay dashboard)</li>
 * </ul>
 *
 * <p>Required {@link PaymentRequest} metadata keys:
 * <ul>
 *   <li>{@code correspondent} — mobile operator code (e.g. {@code "MTN_MOMO_CMR"})</li>
 * </ul>
 *
 * @since 0.1.0
 */
@AutoService(PaymentProviderSpi.class)
public final class PawaPayProviderSpi extends AbstractOfficialProviderSpi {

    private static final String SANDBOX_URL    = "https://api.sandbox.pawapay.io";
    private static final String PRODUCTION_URL = "https://api.pawapay.io";
    private static final String CREDENTIAL_TOKEN = "token";

    private PawaPayClient client;
    private PawaPayWebhookVerifier webhookVerifier;
    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    /**
     * Constructs a new PawaPay provider adapter.
     */
    public PawaPayProviderSpi() {
    }

    @Override
    public String providerName() {
        return "pawapay";
    }

    @Override
    public void configure(ProviderCredentials credentials) {
        super.configure(credentials);
        String token  = credentials.require(CREDENTIAL_TOKEN);
        String baseUrl = credentials.environment() == Environment.PRODUCTION
                ? PRODUCTION_URL
                : SANDBOX_URL;
        this.client = new PawaPayClient(baseUrl, token, HttpClientConfig.defaults());
        this.webhookVerifier = new PawaPayWebhookVerifier(this.client);
    }

    @Override
    protected void requireConfigured() {
        if (client == null) {
            throw new IllegalStateException(
                    "Provider 'pawapay' has not been configured — call configure() first");
        }
    }

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured();
        PawaPayDepositResponse response = client.initiateDeposit(
                PawaPayRequestMapper.toDepositRequest(request));
        return PawaPayResponseMapper.toPaymentResult(response, request.transactionId(), request.amount());
    }

    @Override
    public PaymentResult getStatus(String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        requireConfigured();
        PawaPayDepositResponse response = client.getDeposit(transactionId);
        return PawaPayResponseMapper.toPaymentResult(response, transactionId, null);
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured();
        return PawaPayResponseMapper.toRefundResult(
                client.initiateRefund(PawaPayRequestMapper.toRefundRequest(request)),
                request.amount()
        );
    }

    @Override
    public PayoutResult payout(PayoutRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured();
        PawaPayPayoutResponse response = client.initiatePayout(
                PawaPayRequestMapper.toPayoutRequest(request));
        return PawaPayResponseMapper.toPayoutResult(response, request.payoutId(), request.amount());
    }

    /**
     * Retrieves the current status of a payout by its provider payout ID.
     *
     * @param payoutId the PawaPay payout identifier returned by {@link #payout}
     * @return the normalized payout result with the latest status
     * @throws NullPointerException                  if payoutId is null
     * @throws io.payorch.core.exception.TransactionNotFoundException if not found
     * @since 0.1.0
     */
    public PayoutResult getPayoutStatus(String payoutId) {
        Objects.requireNonNull(payoutId, "payoutId must not be null");
        requireConfigured();
        return PawaPayResponseMapper.toPayoutResult(
                client.getPayout(payoutId), payoutId, null);
    }

    /**
     * Retrieves the current status of a refund by its provider refund ID.
     *
     * @param refundId the PawaPay refund identifier returned by {@link #refund}
     * @return the normalized refund result with the latest status
     * @throws NullPointerException                  if refundId is null
     * @throws io.payorch.core.exception.TransactionNotFoundException if not found
     * @since 0.1.0
     */
    public RefundResult getRefundStatus(String refundId) {
        Objects.requireNonNull(refundId, "refundId must not be null");
        requireConfigured();
        return PawaPayResponseMapper.toRefundResult(client.getRefund(refundId), null);
    }

    /**
     * Verifies the RFC 9421 signature of an inbound PawaPay webhook and parses
     * its body into a normalized {@link PaymentEvent}.
     *
     * <p>Signature verification uses PawaPay's RSA/EC public key fetched from
     * {@code GET /public-key/http} (cached 24 h, invalidated on failure).
     *
     * @param request the raw inbound webhook — headers and body required
     * @return the normalized payment event
     * @throws WebhookValidationException    if signature verification fails
     * @throws InvalidPaymentRequestException if the webhook body cannot be parsed
     */
    @Override
    public PaymentEvent parseWebhook(WebhookRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured();

        if (!webhookVerifier.verify(request.rawBody(), request.headers())) {
            throw new WebhookValidationException(providerName(), "signature verification failed");
        }

        try {
            PawaPayDepositResponse response =
                    mapper.readValue(request.rawBody(), PawaPayDepositResponse.class);
            String transactionId = response.depositId() != null
                    ? response.depositId() : request.rawBody();
            return PawaPayResponseMapper.toPaymentEvent(response, transactionId, null);
        } catch (JsonProcessingException e) {
            throw new InvalidPaymentRequestException(
                    "PawaPay: failed to parse webhook body — " + e.getMessage());
        }
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                true, true, false, true,
                List.of("XAF", "XOF", "GHS", "TZS", "UGX", "RWF", "ZMW", "ETB", "SLL", "BWP"),
                List.of(Environment.SANDBOX, Environment.PRODUCTION)
        );
    }

    /**
     * Creates a PawaPay adapter using a custom HTTP configuration (for testing).
     *
     * @param baseUrl the base URL to use (allows WireMock override in tests)
     * @param config  the HTTP client configuration
     * @return a pre-configured adapter
     */
    static PawaPayProviderSpi withBaseUrl(String baseUrl, HttpClientConfig config) {
        PawaPayProviderSpi spi = new PawaPayProviderSpi();
        spi.client = new PawaPayClient(baseUrl, "test-token", config);
        spi.webhookVerifier = new PawaPayWebhookVerifier(spi.client);
        return spi;
    }

    /**
     * Returns a test-friendly adapter wired to a custom {@link PawaPayClient}.
     *
     * @param client the client to use
     * @return a pre-configured adapter
     */
    static PawaPayProviderSpi withClient(PawaPayClient client) {
        PawaPayProviderSpi spi = new PawaPayProviderSpi();
        spi.client = Objects.requireNonNull(client, "client must not be null");
        spi.webhookVerifier = new PawaPayWebhookVerifier(spi.client);
        return spi;
    }

    /**
     * Builds and returns PawaPay-specific headers for direct use in tests.
     *
     * @return a metadata map template for payment requests
     */
    public static Map<String, String> defaultMetadata(String correspondent) {
        return Map.of(PawaPayRequestMapper.CORRESPONDENT_KEY, correspondent);
    }
}

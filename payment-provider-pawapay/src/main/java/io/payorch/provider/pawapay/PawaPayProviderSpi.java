package io.payorch.provider.pawapay;

import com.google.auto.service.AutoService;
import io.payorch.core.exception.UnsupportedProviderOperationException;
import io.payorch.core.model.Environment;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.AbstractOfficialProviderSpi;
import io.payorch.core.spi.PaymentProviderSpi;
import io.payorch.http.HttpClientConfig;
import io.payorch.provider.pawapay.dto.PawaPayDepositResponse;
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
 *   <li>{@code apiKey} — PawaPay Bearer token</li>
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
    private static final String CREDENTIAL_API_KEY = "apiKey";

    private PawaPayClient client;

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
        String apiKey = credentials.require(CREDENTIAL_API_KEY);
        String baseUrl = credentials.environment() == Environment.PRODUCTION
                ? PRODUCTION_URL
                : SANDBOX_URL;
        this.client = new PawaPayClient(baseUrl, apiKey, HttpClientConfig.defaults());
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
    public PaymentEvent parseWebhook(WebhookRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        throw new UnsupportedProviderOperationException(providerName(), "parseWebhook");
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                true, false, true,
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
        spi.client = new PawaPayClient(baseUrl, "test-key", config);
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

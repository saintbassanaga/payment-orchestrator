package io.payorch.provider.cinetpay;

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
import io.payorch.provider.cinetpay.mapper.CinetPayRequestMapper;
import io.payorch.provider.cinetpay.mapper.CinetPayResponseMapper;

import java.util.List;
import java.util.Objects;

/**
 * PayOrch adapter for the CinetPay payment gateway.
 *
 * <p>Required credential keys:
 * <ul>
 *   <li>{@code apiKey} — the CinetPay API key</li>
 *   <li>{@code siteId} — the CinetPay site identifier</li>
 * </ul>
 *
 * <p>Optional {@link PaymentRequest} metadata keys:
 * <ul>
 *   <li>{@code notifyUrl} — webhook URL for asynchronous notifications</li>
 * </ul>
 *
 * @since 0.1.0
 */
@AutoService(PaymentProviderSpi.class)
public final class CinetPayProviderSpi extends AbstractOfficialProviderSpi {

    private static final String BASE_URL = "https://api-checkout.cinetpay.com/v2";
    private static final String CREDENTIAL_API_KEY = "apiKey";
    private static final String CREDENTIAL_SITE_ID = "siteId";

    private CinetPayClient client;
    private String apiKey;
    private String siteId;

    /**
     * Constructs a new CinetPay provider adapter.
     */
    public CinetPayProviderSpi() {
    }

    @Override
    public String providerName() {
        return "cinetpay";
    }

    @Override
    public void configure(ProviderCredentials credentials) {
        super.configure(credentials);
        this.apiKey = credentials.require(CREDENTIAL_API_KEY);
        this.siteId = credentials.require(CREDENTIAL_SITE_ID);
        this.client = new CinetPayClient(BASE_URL, HttpClientConfig.defaults());
    }

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        requireConfigured();
        return CinetPayResponseMapper.toPaymentResult(
                client.initiatePayment(CinetPayRequestMapper.toPaymentRequest(request, apiKey, siteId)),
                request.transactionId(),
                request.amount()
        );
    }

    @Override
    public PaymentResult getStatus(String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        requireConfigured();
        return CinetPayResponseMapper.toStatusResult(
                client.checkPayment(CinetPayRequestMapper.toCheckRequest(transactionId, apiKey, siteId)),
                transactionId,
                null
        );
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new UnsupportedProviderOperationException(providerName(), "refund");
    }

    @Override
    public PaymentEvent parseWebhook(WebhookRequest request) {
        throw new UnsupportedProviderOperationException(providerName(), "parseWebhook");
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(
                false, false, true,
                List.of("XAF", "XOF", "CDF", "GNF"),
                List.of(Environment.SANDBOX, Environment.PRODUCTION)
        );
    }

    @Override
    protected void requireConfigured() {
        if (client == null) {
            throw new IllegalStateException(
                    "Provider 'cinetpay' has not been configured — call configure() first");
        }
    }

    /**
     * Creates a CinetPay adapter wired to a custom base URL (for testing).
     *
     * @param baseUrl the URL to target (e.g. WireMock server)
     * @param config  the HTTP client configuration
     * @param apiKey  the API key to use
     * @param siteId  the site ID to use
     * @return a pre-configured adapter
     */
    static CinetPayProviderSpi withBaseUrl(
            String baseUrl, HttpClientConfig config, String apiKey, String siteId) {
        CinetPayProviderSpi spi = new CinetPayProviderSpi();
        spi.client = new CinetPayClient(baseUrl, config);
        spi.apiKey = apiKey;
        spi.siteId = siteId;
        return spi;
    }
}

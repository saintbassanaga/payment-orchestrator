package io.payorch.test;

import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.PayoutRequest;
import io.payorch.core.model.PayoutResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.CommunityPaymentProviderSpi;

import java.util.Objects;

/**
 * Configurable in-memory provider for use in unit tests.
 *
 * <p>Use the fluent {@code willReturn*} methods to set up canned responses,
 * and the {@code verify*} methods to assert that operations were invoked.
 *
 * <pre>{@code
 * MockProvider provider = new MockProvider("pawapay")
 *     .willReturnOnInitiate(PaymentTestFixtures.paymentResult("TX-1", PaymentStatus.INITIATED, "pawapay"));
 *
 * provider.configure(credentials);
 * PaymentResult result = provider.initiate(request);
 * provider.verifyInitiateCalled();
 * }</pre>
 *
 * @since 0.1.0
 */
public final class MockProvider implements CommunityPaymentProviderSpi {

    private final String name;
    private ProviderCredentials configuredWith;
    private PaymentResult initiateResult;
    private PaymentResult statusResult;
    private RefundResult refundResult;
    private PayoutResult payoutResult;
    private PaymentEvent webhookEvent;
    private ProviderCapabilities capabilities;

    private int configureCalls;
    private int initiateCalls;
    private int getStatusCalls;
    private int refundCalls;
    private int payoutCalls;
    private int parseWebhookCalls;

    /**
     * Constructs a mock provider with the given name and full default capabilities.
     *
     * @param name the canonical provider name, never null
     * @throws NullPointerException if {@code name} is null
     */
    public MockProvider(String name) {
        this.name = Objects.requireNonNull(name, "name must not be null");
        this.capabilities = PaymentTestFixtures.fullCapabilities();
        this.initiateResult = PaymentTestFixtures.paymentResult("TX-MOCK", PaymentStatus.INITIATED, name);
        this.statusResult = PaymentTestFixtures.paymentResult("TX-MOCK", PaymentStatus.SUCCESS, name);
        this.refundResult = PaymentTestFixtures.refundResult("RF-MOCK", "TX-MOCK", name);
        this.payoutResult = PaymentTestFixtures.payoutResult("PO-MOCK", name);
        this.webhookEvent = PaymentTestFixtures.paymentEvent("TX-MOCK", name);
    }

    @Override
    public String providerName() {
        return name;
    }

    @Override
    public void configure(ProviderCredentials credentials) {
        Objects.requireNonNull(credentials, "credentials must not be null");
        this.configuredWith = credentials;
        this.configureCalls++;
    }

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.initiateCalls++;
        return initiateResult;
    }

    @Override
    public PaymentResult getStatus(String transactionId) {
        Objects.requireNonNull(transactionId, "transactionId must not be null");
        this.getStatusCalls++;
        return statusResult;
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.refundCalls++;
        return refundResult;
    }

    @Override
    public PayoutResult payout(PayoutRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.payoutCalls++;
        return payoutResult;
    }

    @Override
    public PaymentEvent parseWebhook(WebhookRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        this.parseWebhookCalls++;
        return webhookEvent;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return capabilities;
    }

    // --- fluent configuration ---

    /**
     * Configures the response returned by {@link #initiate(PaymentRequest)}.
     *
     * @param result the result to return, never null
     * @return this instance for chaining
     */
    public MockProvider willReturnOnInitiate(PaymentResult result) {
        this.initiateResult = Objects.requireNonNull(result, "result must not be null");
        return this;
    }

    /**
     * Configures the response returned by {@link #getStatus(String)}.
     *
     * @param result the result to return, never null
     * @return this instance for chaining
     */
    public MockProvider willReturnOnGetStatus(PaymentResult result) {
        this.statusResult = Objects.requireNonNull(result, "result must not be null");
        return this;
    }

    /**
     * Configures the response returned by {@link #refund(RefundRequest)}.
     *
     * @param result the result to return, never null
     * @return this instance for chaining
     */
    public MockProvider willReturnOnRefund(RefundResult result) {
        this.refundResult = Objects.requireNonNull(result, "result must not be null");
        return this;
    }

    /**
     * Configures the result returned by {@link #payout(PayoutRequest)}.
     *
     * @param result the result to return, never null
     * @return this instance for chaining
     */
    public MockProvider willReturnOnPayout(PayoutResult result) {
        this.payoutResult = Objects.requireNonNull(result, "result must not be null");
        return this;
    }

    /**
     * Configures the event returned by {@link #parseWebhook(WebhookRequest)}.
     *
     * @param event the event to return, never null
     * @return this instance for chaining
     */
    public MockProvider willReturnOnParseWebhook(PaymentEvent event) {
        this.webhookEvent = Objects.requireNonNull(event, "event must not be null");
        return this;
    }

    /**
     * Configures the capabilities returned by {@link #capabilities()}.
     *
     * @param caps the capabilities to return, never null
     * @return this instance for chaining
     */
    public MockProvider withCapabilities(ProviderCapabilities caps) {
        this.capabilities = Objects.requireNonNull(caps, "caps must not be null");
        return this;
    }

    // --- verification ---

    /**
     * Returns the credentials supplied to the last {@link #configure} call, or {@code null}
     * if {@code configure} was never called.
     *
     * @return the configured credentials, may be null
     */
    public ProviderCredentials configuredWith() {
        return configuredWith;
    }

    /**
     * Returns the number of times {@link #configure} was called.
     *
     * @return configure call count
     */
    public int configureCalls() {
        return configureCalls;
    }

    /**
     * Returns the number of times {@link #initiate} was called.
     *
     * @return initiate call count
     */
    public int initiateCalls() {
        return initiateCalls;
    }

    /**
     * Returns the number of times {@link #getStatus} was called.
     *
     * @return getStatus call count
     */
    public int getStatusCalls() {
        return getStatusCalls;
    }

    /**
     * Returns the number of times {@link #refund} was called.
     *
     * @return refund call count
     */
    public int refundCalls() {
        return refundCalls;
    }

    /**
     * Returns the number of times {@link #payout} was called.
     *
     * @return payout call count
     */
    public int payoutCalls() {
        return payoutCalls;
    }

    /**
     * Returns the number of times {@link #parseWebhook} was called.
     *
     * @return parseWebhook call count
     */
    public int parseWebhookCalls() {
        return parseWebhookCalls;
    }
}

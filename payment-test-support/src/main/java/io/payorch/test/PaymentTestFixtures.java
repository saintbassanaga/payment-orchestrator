package io.payorch.test;

import io.payorch.core.model.Environment;
import io.payorch.core.model.Money;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.PaymentStatus;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Factory methods for building test objects with sensible defaults.
 *
 * <p>Use these in unit and integration tests to avoid boilerplate construction code.
 *
 * @since 0.1.0
 */
public final class PaymentTestFixtures {

    private PaymentTestFixtures() {
    }

    /**
     * Returns a {@link Money} instance of 5000 XAF.
     *
     * @return a default money value
     */
    public static Money defaultMoney() {
        return Money.of(new BigDecimal("5000"), "XAF");
    }

    /**
     * Returns a {@link Money} instance with the given amount in XAF.
     *
     * @param amount the amount as a string
     * @return a money value
     */
    public static Money money(String amount) {
        return Money.of(amount, "XAF");
    }

    /**
     * Returns a minimal {@link PaymentRequest} with default values.
     *
     * @return a default payment request
     */
    public static PaymentRequest defaultPaymentRequest() {
        return PaymentRequest.of("TX-TEST-001", defaultMoney(), "+237600000000", "Test payment");
    }

    /**
     * Returns a {@link PaymentRequest} with the given transaction ID.
     *
     * @param transactionId the unique transaction identifier
     * @return a payment request
     */
    public static PaymentRequest paymentRequest(String transactionId) {
        return PaymentRequest.of(transactionId, defaultMoney(), "+237600000000", "Test payment");
    }

    /**
     * Returns a {@link PaymentResult} with the given status.
     *
     * @param transactionId the transaction identifier
     * @param status        the payment status
     * @param providerName  the provider name
     * @return a payment result
     */
    public static PaymentResult paymentResult(
            String transactionId, PaymentStatus status, String providerName) {
        return new PaymentResult(
                transactionId, "EXT-" + transactionId, status,
                defaultMoney(), providerName, Optional.empty(), Instant.now(), Map.of()
        );
    }

    /**
     * Returns a {@link RefundRequest} for the given original transaction.
     *
     * @param originalTransactionId the ID of the payment to refund
     * @return a refund request
     */
    public static RefundRequest defaultRefundRequest(String originalTransactionId) {
        return new RefundRequest(
                "RF-" + originalTransactionId, originalTransactionId,
                defaultMoney(), "Test refund", Map.of()
        );
    }

    /**
     * Returns a {@link RefundResult} with a REFUNDED status.
     *
     * @param refundId              the refund identifier
     * @param originalTransactionId the original transaction identifier
     * @param providerName          the provider name
     * @return a refund result
     */
    public static RefundResult refundResult(
            String refundId, String originalTransactionId, String providerName) {
        return new RefundResult(
                refundId, originalTransactionId, PaymentStatus.REFUNDED,
                defaultMoney(), providerName, Optional.empty(), Instant.now()
        );
    }

    /**
     * Returns a {@link PaymentEvent} with a SUCCESS status.
     *
     * @param transactionId the transaction identifier
     * @param providerName  the provider name
     * @return a payment event
     */
    public static PaymentEvent paymentEvent(String transactionId, String providerName) {
        return new PaymentEvent(
                transactionId, "EXT-" + transactionId, PaymentStatus.SUCCESS,
                defaultMoney(), providerName, Instant.now(), Map.of()
        );
    }

    /**
     * Returns a {@link WebhookRequest} with the given raw body.
     *
     * @param providerName the provider that sent the webhook
     * @param rawBody      the raw JSON body
     * @return a webhook request
     */
    public static WebhookRequest webhookRequest(String providerName, String rawBody) {
        return new WebhookRequest(providerName, Map.of("Content-Type", "application/json"), rawBody);
    }

    /**
     * Returns sandbox {@link ProviderCredentials} for the given provider with the given properties.
     *
     * @param providerName the provider name
     * @param properties   the credential key-value pairs
     * @return sandbox credentials
     */
    public static ProviderCredentials sandboxCredentials(
            String providerName, Map<String, String> properties) {
        return new ProviderCredentials(providerName, Environment.SANDBOX, properties);
    }

    /**
     * Returns minimal sandbox {@link ProviderCredentials} with no properties.
     *
     * @param providerName the provider name
     * @return empty sandbox credentials
     */
    public static ProviderCredentials sandboxCredentials(String providerName) {
        return sandboxCredentials(providerName, Map.of());
    }

    /**
     * Returns a {@link ProviderCapabilities} where all capabilities are enabled.
     *
     * @return full capabilities descriptor
     */
    public static ProviderCapabilities fullCapabilities() {
        return new ProviderCapabilities(
                true, true, true,
                List.of("XAF", "XOF", "GHS", "NGN"),
                List.of(Environment.SANDBOX, Environment.PRODUCTION)
        );
    }
}

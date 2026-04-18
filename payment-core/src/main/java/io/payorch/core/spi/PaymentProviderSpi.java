package io.payorch.core.spi;

import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;

/**
 * Service Provider Interface that every payment provider adapter must implement.
 *
 * <p>This interface is {@code sealed}: only modules within this project may implement
 * it directly via {@link AbstractOfficialProviderSpi}. Community adapters must implement
 * {@link CommunityPaymentProviderSpi}, which is open.
 *
 * <p>No checked exception may cross this boundary — all errors are reported as
 * subtypes of {@link io.payorch.core.exception.PaymentException}.
 *
 * @since 0.1.0
 */
public sealed interface PaymentProviderSpi
        permits AbstractOfficialProviderSpi, CommunityPaymentProviderSpi {

    /**
     * Returns the canonical, lowercase name that identifies this provider
     * (e.g. {@code "pawapay"}, {@code "cinetpay"}).
     *
     * @return the provider name, never null or blank
     */
    String providerName();

    /**
     * Configures this adapter with the caller-supplied credentials.
     *
     * <p>This method is called once before any other operation. Implementations
     * must store the credentials and validate their presence.
     *
     * @param credentials the provider credentials, never null
     * @throws io.payorch.core.exception.ProviderAuthException if credentials are invalid
     */
    void configure(ProviderCredentials credentials);

    /**
     * Initiates a payment request with the provider.
     *
     * @param request the normalized payment request, never null
     * @return the normalized payment result, never null
     * @throws io.payorch.core.exception.InvalidPaymentRequestException  if the request is invalid
     * @throws io.payorch.core.exception.ProviderUnavailableException    if the provider is unreachable
     */
    PaymentResult initiate(PaymentRequest request);

    /**
     * Retrieves the current status of an existing transaction.
     *
     * @param transactionId the caller-supplied transaction identifier, never null
     * @return the normalized payment result, never null
     * @throws io.payorch.core.exception.TransactionNotFoundException    if the transaction does not exist
     * @throws io.payorch.core.exception.ProviderUnavailableException    if the provider is unreachable
     */
    PaymentResult getStatus(String transactionId);

    /**
     * Issues a full or partial refund for a completed transaction.
     *
     * @param request the normalized refund request, never null
     * @return the normalized refund result, never null
     * @throws io.payorch.core.exception.UnsupportedProviderOperationException if refunds are not supported
     * @throws io.payorch.core.exception.TransactionNotFoundException           if the original transaction is not found
     * @throws io.payorch.core.exception.ProviderUnavailableException           if the provider is unreachable
     */
    RefundResult refund(RefundRequest request);

    /**
     * Parses and validates an inbound webhook payload into a normalized event.
     *
     * @param request the raw webhook request containing headers and body, never null
     * @return the normalized payment event, never null
     * @throws io.payorch.core.exception.WebhookValidationException             if the signature or payload is invalid
     * @throws io.payorch.core.exception.UnsupportedProviderOperationException  if webhooks are not supported
     */
    PaymentEvent parseWebhook(WebhookRequest request);

    /**
     * Returns the static capability descriptor for this provider.
     *
     * @return the provider capabilities, never null
     */
    ProviderCapabilities capabilities();
}

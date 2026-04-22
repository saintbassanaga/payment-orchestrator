package io.payorch.autoconfigure;

import io.payorch.core.exception.UnsupportedProviderOperationException;
import io.payorch.core.model.PaymentEvent;
import io.payorch.core.model.PaymentRequest;
import io.payorch.core.model.PaymentResult;
import io.payorch.core.model.ProviderCapabilities;
import io.payorch.core.model.ProviderCredentials;
import io.payorch.core.model.PayoutRequest;
import io.payorch.core.model.PayoutResult;
import io.payorch.core.model.RefundRequest;
import io.payorch.core.model.RefundResult;
import io.payorch.core.model.WebhookRequest;
import io.payorch.core.spi.CommunityPaymentProviderSpi;

import java.util.List;

final class StubProvider implements CommunityPaymentProviderSpi {

    private final String name;

    StubProvider(String name) {
        this.name = name;
    }

    @Override
    public String providerName() {
        return name;
    }

    @Override
    public void configure(ProviderCredentials credentials) {
    }

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        throw new UnsupportedProviderOperationException(name, "initiate");
    }

    @Override
    public PaymentResult getStatus(String transactionId) {
        throw new UnsupportedProviderOperationException(name, "getStatus");
    }

    @Override
    public RefundResult refund(RefundRequest request) {
        throw new UnsupportedProviderOperationException(name, "refund");
    }

    @Override
    public PayoutResult payout(PayoutRequest request) {
        throw new UnsupportedProviderOperationException(name, "payout");
    }

    @Override
    public PaymentEvent parseWebhook(WebhookRequest request) {
        throw new UnsupportedProviderOperationException(name, "parseWebhook");
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(false, false, false, false, List.of(), List.of());
    }
}

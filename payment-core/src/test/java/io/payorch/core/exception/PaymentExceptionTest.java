package io.payorch.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentExceptionTest {

    @Test
    void should_format_provider_auth_message_with_cause() {
        RuntimeException cause = new RuntimeException("401 Unauthorized");
        ProviderAuthException ex = new ProviderAuthException("pawapay", cause);

        assertThat(ex.getMessage()).contains("pawapay");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void should_format_provider_auth_message_with_string() {
        ProviderAuthException ex = new ProviderAuthException("cinetpay", "invalid token");

        assertThat(ex.getMessage()).contains("cinetpay").contains("invalid token");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void should_format_provider_unavailable_message_with_cause() {
        RuntimeException cause = new RuntimeException("timeout");
        ProviderUnavailableException ex = new ProviderUnavailableException("pawapay", cause);

        assertThat(ex.getMessage()).contains("pawapay");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void should_format_provider_unavailable_message_without_cause() {
        ProviderUnavailableException ex = new ProviderUnavailableException("unknown status 'FOO'");

        assertThat(ex.getMessage()).contains("FOO");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void should_format_provider_not_found_message() {
        ProviderNotFoundException ex = new ProviderNotFoundException("stripe");

        assertThat(ex.getMessage()).contains("stripe");
    }

    @Test
    void should_format_invalid_request_message() {
        InvalidPaymentRequestException ex = new InvalidPaymentRequestException("amount negative");

        assertThat(ex.getMessage()).contains("amount negative");
    }

    @Test
    void should_format_invalid_request_with_cause() {
        RuntimeException cause = new RuntimeException("parse error");
        InvalidPaymentRequestException ex = new InvalidPaymentRequestException("bad input", cause);

        assertThat(ex.getMessage()).contains("bad input");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void should_format_webhook_validation_message() {
        WebhookValidationException ex = new WebhookValidationException("pawapay", "bad signature");

        assertThat(ex.getMessage()).contains("pawapay").contains("bad signature");
        assertThat(ex.getCause()).isNull();
    }

    @Test
    void should_format_webhook_validation_message_with_cause() {
        RuntimeException cause = new RuntimeException("hmac failed");
        WebhookValidationException ex = new WebhookValidationException("pawapay", "bad sig", cause);

        assertThat(ex.getMessage()).contains("pawapay");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    void should_format_transaction_not_found_message() {
        TransactionNotFoundException ex = new TransactionNotFoundException("pawapay", "TX-999");

        assertThat(ex.getMessage()).contains("TX-999").contains("pawapay");
    }

    @Test
    void should_format_unsupported_operation_message() {
        UnsupportedProviderOperationException ex =
                new UnsupportedProviderOperationException("monetbill", "refund");

        assertThat(ex.getMessage()).contains("monetbill").contains("refund");
    }

    @Test
    void should_be_unchecked_exceptions() {
        assertThat(new ProviderAuthException("p", "m")).isInstanceOf(RuntimeException.class);
        assertThat(new ProviderNotFoundException("p")).isInstanceOf(RuntimeException.class);
        assertThat(new InvalidPaymentRequestException("m")).isInstanceOf(RuntimeException.class);
    }
}

package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebhookRequestTest {

    @Test
    void should_find_header_case_insensitively() {
        WebhookRequest request = new WebhookRequest(
                "pawapay", Map.of("X-Signature", "abc123"), "{}"
        );

        assertThat(request.header("x-signature")).contains("abc123");
        assertThat(request.header("X-SIGNATURE")).contains("abc123");
        assertThat(request.header("X-Signature")).contains("abc123");
    }

    @Test
    void should_return_empty_when_header_is_absent() {
        WebhookRequest request = new WebhookRequest("pawapay", Map.of(), "{}");

        assertThat(request.header("X-Signature")).isEmpty();
    }

    @Test
    void should_make_headers_immutable() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("key", "value");
        WebhookRequest request = new WebhookRequest("pawapay", mutable, "{}");

        mutable.put("injected", "evil");

        assertThat(request.header("injected")).isEmpty();
    }

    @Test
    void should_throw_when_provider_name_is_null() {
        assertThatThrownBy(() -> new WebhookRequest(null, Map.of(), "{}"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("providerName");
    }

    @Test
    void should_throw_when_headers_are_null() {
        assertThatThrownBy(() -> new WebhookRequest("pawapay", null, "{}"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("headers");
    }

    @Test
    void should_throw_when_raw_body_is_null() {
        assertThatThrownBy(() -> new WebhookRequest("pawapay", Map.of(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("rawBody");
    }

    @Test
    void should_throw_when_header_name_is_null() {
        WebhookRequest request = new WebhookRequest("pawapay", Map.of(), "{}");

        assertThatThrownBy(() -> request.header(null))
                .isInstanceOf(NullPointerException.class);
    }
}
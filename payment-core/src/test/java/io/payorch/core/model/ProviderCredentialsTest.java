package io.payorch.core.model;

import io.payorch.core.exception.InvalidPaymentRequestException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCredentialsTest {

    @Test
    void should_return_value_when_key_exists() {
        ProviderCredentials creds = credentials("apiKey", "secret-123");

        assertThat(creds.get("apiKey")).contains("secret-123");
    }

    @Test
    void should_return_empty_when_key_is_absent() {
        ProviderCredentials creds = credentials("apiKey", "secret-123");

        assertThat(creds.get("missing")).isEmpty();
    }

    @Test
    void should_require_value_when_key_exists() {
        ProviderCredentials creds = credentials("apiKey", "secret-123");

        assertThat(creds.require("apiKey")).isEqualTo("secret-123");
    }

    @Test
    void should_throw_when_required_key_is_absent() {
        ProviderCredentials creds = credentials("apiKey", "secret-123");

        assertThatThrownBy(() -> creds.require("missing"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("missing")
                .hasMessageContaining("pawapay");
    }

    @Test
    void should_throw_when_provider_name_is_null() {
        assertThatThrownBy(() -> new ProviderCredentials(null, Environment.SANDBOX, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("providerName");
    }

    @Test
    void should_throw_when_environment_is_null() {
        assertThatThrownBy(() -> new ProviderCredentials("pawapay", null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("environment");
    }

    @Test
    void should_throw_when_properties_is_null() {
        assertThatThrownBy(() -> new ProviderCredentials("pawapay", Environment.SANDBOX, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("properties");
    }

    @Test
    void should_make_properties_immutable() {
        Map<String, String> mutable = new HashMap<>();
        mutable.put("key", "value");
        ProviderCredentials creds = new ProviderCredentials("pawapay", Environment.SANDBOX, mutable);

        mutable.put("injected", "evil");

        assertThat(creds.get("injected")).isEmpty();
    }

    @Test
    void should_throw_when_get_key_is_null() {
        ProviderCredentials creds = credentials("apiKey", "value");

        assertThatThrownBy(() -> creds.get(null))
                .isInstanceOf(NullPointerException.class);
    }

    private static ProviderCredentials credentials(String key, String value) {
        return new ProviderCredentials("pawapay", Environment.SANDBOX, Map.of(key, value));
    }
}
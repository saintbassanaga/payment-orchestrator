package io.payorch.core.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProviderCapabilitiesTest {

    @Test
    void should_construct_and_expose_all_flags() {
        ProviderCapabilities caps = new ProviderCapabilities(
                true, true, false, true,
                List.of("XAF", "XOF"),
                List.of(Environment.SANDBOX, Environment.PRODUCTION));

        assertThat(caps.supportsRefund()).isTrue();
        assertThat(caps.supportsPayout()).isTrue();
        assertThat(caps.supportsWebhook()).isFalse();
        assertThat(caps.supportsStatusCheck()).isTrue();
        assertThat(caps.supportedCurrencies()).containsExactly("XAF", "XOF");
        assertThat(caps.supportedEnvironments())
                .containsExactly(Environment.SANDBOX, Environment.PRODUCTION);
    }

    @Test
    void should_make_currency_list_immutable() {
        ArrayList<String> mutable = new ArrayList<>(List.of("XAF"));
        ProviderCapabilities caps = new ProviderCapabilities(
                false, false, false, false, mutable, List.of(Environment.SANDBOX));

        assertThatThrownBy(() -> caps.supportedCurrencies().add("USD"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_make_environment_list_immutable() {
        ArrayList<Environment> mutable = new ArrayList<>(List.of(Environment.SANDBOX));
        ProviderCapabilities caps = new ProviderCapabilities(
                false, false, false, false, List.of("XAF"), mutable);

        assertThatThrownBy(() -> caps.supportedEnvironments().add(Environment.PRODUCTION))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void should_throw_on_null_lists() {
        assertThatThrownBy(() -> new ProviderCapabilities(
                false, false, false, false, null, List.of(Environment.SANDBOX)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ProviderCapabilities(
                false, false, false, false, List.of("XAF"), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_cover_all_environments() {
        assertThat(Environment.values())
                .containsExactlyInAnyOrder(Environment.SANDBOX, Environment.PRODUCTION);
    }
}

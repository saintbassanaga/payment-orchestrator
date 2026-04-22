package io.payorch.http;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpClientConfigTest {

    @Test
    void should_return_sensible_defaults() {
        HttpClientConfig config = HttpClientConfig.defaults();

        assertThat(config.connectTimeout()).isPositive();
        assertThat(config.readTimeout()).isPositive();
        assertThat(config.writeTimeout()).isPositive();
        assertThat(config.maxRetries()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void should_construct_with_custom_values() {
        HttpClientConfig config = new HttpClientConfig(
                Duration.ofSeconds(5), Duration.ofSeconds(15),
                Duration.ofSeconds(15), 3);

        assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(15));
        assertThat(config.maxRetries()).isEqualTo(3);
    }

    @Test
    void should_throw_on_negative_retries() {
        assertThatThrownBy(() -> new HttpClientConfig(
                Duration.ofSeconds(5), Duration.ofSeconds(15),
                Duration.ofSeconds(15), -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_on_null_timeouts() {
        assertThatThrownBy(() -> new HttpClientConfig(
                null, Duration.ofSeconds(15), Duration.ofSeconds(15), 2))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new HttpClientConfig(
                Duration.ofSeconds(5), null, Duration.ofSeconds(15), 2))
                .isInstanceOf(NullPointerException.class);
    }
}

package io.payorch.http;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for the PayOrch HTTP client.
 *
 * <p>Use {@link #defaults()} for sensible out-of-the-box settings, or construct
 * a custom instance via the canonical constructor.
 *
 * @param connectTimeout timeout for establishing a TCP connection
 * @param readTimeout    timeout for reading a response from the server
 * @param writeTimeout   timeout for writing a request to the server
 * @param maxRetries     number of retry attempts on transient network failures (0 = no retry)
 * @since 0.1.0
 */
public record HttpClientConfig(
        Duration connectTimeout,
        Duration readTimeout,
        Duration writeTimeout,
        int maxRetries) {

    /**
     * Validates the configuration at construction time.
     *
     * @throws NullPointerException     if any timeout is null
     * @throws IllegalArgumentException if {@code maxRetries} is negative
     */
    public HttpClientConfig {
        Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
        Objects.requireNonNull(readTimeout, "readTimeout must not be null");
        Objects.requireNonNull(writeTimeout, "writeTimeout must not be null");
        if (maxRetries < 0) {
            throw new IllegalArgumentException(
                    "maxRetries must be >= 0, got: " + maxRetries);
        }
    }

    /**
     * Returns a configuration with sensible defaults suitable for most providers.
     *
     * <ul>
     *   <li>Connect timeout: 10 s</li>
     *   <li>Read timeout: 30 s</li>
     *   <li>Write timeout: 30 s</li>
     *   <li>Max retries: 2</li>
     * </ul>
     *
     * @return the default configuration
     */
    public static HttpClientConfig defaults() {
        return new HttpClientConfig(
                Duration.ofSeconds(10),
                Duration.ofSeconds(30),
                Duration.ofSeconds(30),
                2
        );
    }
}

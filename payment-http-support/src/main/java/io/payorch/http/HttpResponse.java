package io.payorch.http;

import java.util.Objects;

/**
 * Represents a normalized HTTP response returned by {@link PayOrchHttpClient}.
 *
 * @param statusCode the HTTP status code
 * @param body       the response body as a string (may be empty but never null)
 * @since 0.1.0
 */
public record HttpResponse(int statusCode, String body) {

    /**
     * Validates the response at construction time.
     *
     * @throws NullPointerException if {@code body} is null
     */
    public HttpResponse {
        Objects.requireNonNull(body, "body must not be null");
    }

    /**
     * Returns {@code true} if the status code is in the 2xx range.
     *
     * @return whether this response indicates success
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns {@code true} if the status code is in the 4xx range.
     *
     * @return whether this response indicates a client error
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Returns {@code true} if the status code is in the 5xx range.
     *
     * @return whether this response indicates a server error
     */
    public boolean isServerError() {
        return statusCode >= 500;
    }
}
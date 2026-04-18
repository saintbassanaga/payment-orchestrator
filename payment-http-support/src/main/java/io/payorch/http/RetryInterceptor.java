package io.payorch.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OkHttp interceptor that retries requests on transient network failures.
 *
 * <p>Retries up to {@code maxRetries} times when an {@link IOException} is thrown
 * (e.g. connection reset, timeout). Client errors (4xx) are never retried.
 *
 * @since 0.1.0
 */
public final class RetryInterceptor implements Interceptor {

    private static final Logger LOG = Logger.getLogger(RetryInterceptor.class.getName());

    private final int maxRetries;

    /**
     * Constructs a retry interceptor with the given retry limit.
     *
     * @param maxRetries the maximum number of retry attempts (0 = no retry)
     * @throws IllegalArgumentException if {@code maxRetries} is negative
     */
    public RetryInterceptor(int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0, got: " + maxRetries);
        }
        this.maxRetries = maxRetries;
    }

    /**
     * Proceeds with the request, retrying on {@link IOException} up to {@code maxRetries} times.
     *
     * @param chain the interceptor chain
     * @return the response from the server
     * @throws IOException if all attempts fail
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        IOException lastException = null;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            if (attempt > 0) {
                LOG.log(Level.WARNING, "Retrying request to {0} (attempt {1}/{2})",
                        new Object[]{request.url(), attempt, maxRetries});
            }
            try {
                return chain.proceed(request);
            } catch (IOException e) {
                lastException = e;
            }
        }

        throw lastException;
    }
}
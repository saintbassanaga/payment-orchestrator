package io.payorch.http;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * OkHttp interceptor that logs outgoing requests and incoming responses.
 *
 * <p>Uses {@link java.util.logging.Logger} to avoid introducing an external
 * logging dependency in {@code payment-http-support}.
 *
 * @since 0.1.0
 */
public final class LoggingInterceptor implements Interceptor {

    private static final Logger LOG = Logger.getLogger(LoggingInterceptor.class.getName());

    /**
     * Constructs a new logging interceptor.
     */
    public LoggingInterceptor() {
    }

    /**
     * Logs the outgoing request and the incoming response status.
     *
     * @param chain the interceptor chain
     * @return the response from the next interceptor or server
     * @throws IOException if the request fails
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        LOG.log(Level.FINE, "-> {0} {1}",
                new Object[]{request.method(), request.url()});

        long start = System.currentTimeMillis();
        Response response = chain.proceed(request);
        long elapsed = System.currentTimeMillis() - start;

        LOG.log(Level.FINE, "<- {0} {1} ({2}ms)",
                new Object[]{response.code(), request.url(), elapsed});

        return response;
    }
}
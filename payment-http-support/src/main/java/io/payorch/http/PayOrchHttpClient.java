package io.payorch.http;

import io.payorch.core.exception.ProviderUnavailableException;
import okhttp3.Dispatcher;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

/**
 * HTTP client wrapping OkHttp for use by payment provider adapters.
 *
 * <p>All requests are dispatched on Java 21 virtual threads, making the
 * synchronous API highly scalable under concurrent load without blocking
 * platform threads.
 *
 * <p>Network-level failures are wrapped in {@link ProviderUnavailableException}
 * so that adapters never need to handle raw {@link IOException}.
 *
 * @since 0.1.0
 */
public final class PayOrchHttpClient {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final String providerName;

    /**
     * Constructs a client for the given provider using default configuration.
     *
     * @param providerName the canonical name of the provider (used in error messages)
     * @throws NullPointerException if {@code providerName} is null
     */
    public PayOrchHttpClient(String providerName) {
        this(providerName, HttpClientConfig.defaults());
    }

    /**
     * Constructs a client for the given provider using custom configuration.
     *
     * @param providerName the canonical name of the provider (used in error messages)
     * @param config       the HTTP client configuration
     * @throws NullPointerException if any argument is null
     */
    public PayOrchHttpClient(String providerName, HttpClientConfig config) {
        Objects.requireNonNull(providerName, "providerName must not be null");
        Objects.requireNonNull(config, "config must not be null");
        this.providerName = providerName;
        this.client = buildClient(config);
    }

    /**
     * Executes an HTTP GET request and returns the normalized response.
     *
     * @param url     the target URL
     * @param headers additional HTTP headers to include
     * @return the HTTP response
     * @throws NullPointerException          if {@code url} or {@code headers} is null
     * @throws ProviderUnavailableException  if a network failure occurs
     */
    public HttpResponse get(String url, Map<String, String> headers) {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(headers, "headers must not be null");

        Request.Builder builder = new Request.Builder().url(url).get();
        headers.forEach(builder::addHeader);

        return execute(builder.build());
    }

    /**
     * Executes an HTTP POST request with a JSON body and returns the normalized response.
     *
     * @param url     the target URL
     * @param json    the JSON request body
     * @param headers additional HTTP headers to include
     * @return the HTTP response
     * @throws NullPointerException          if any argument is null
     * @throws ProviderUnavailableException  if a network failure occurs
     */
    public HttpResponse post(String url, String json, Map<String, String> headers) {
        Objects.requireNonNull(url, "url must not be null");
        Objects.requireNonNull(json, "json must not be null");
        Objects.requireNonNull(headers, "headers must not be null");

        RequestBody body = RequestBody.create(json, JSON);
        Request.Builder builder = new Request.Builder().url(url).post(body);
        headers.forEach(builder::addHeader);

        return execute(builder.build());
    }

    private HttpResponse execute(Request request) {
        try (Response response = client.newCall(request).execute()) {
            ResponseBody responseBody = response.body();
            String bodyString = responseBody != null ? responseBody.string() : "";
            return new HttpResponse(response.code(), bodyString);
        } catch (IOException e) {
            throw new ProviderUnavailableException(providerName, e);
        }
    }

    private static OkHttpClient buildClient(HttpClientConfig config) {
        return new OkHttpClient.Builder()
                .dispatcher(new Dispatcher(Executors.newVirtualThreadPerTaskExecutor()))
                .connectTimeout(config.connectTimeout())
                .readTimeout(config.readTimeout())
                .writeTimeout(config.writeTimeout())
                .addInterceptor(new LoggingInterceptor())
                .addInterceptor(new RetryInterceptor(config.maxRetries()))
                .build();
    }
}

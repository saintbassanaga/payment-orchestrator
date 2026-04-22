package io.payorch.http;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RetryInterceptorTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void should_throw_on_negative_max_retries() {
        assertThatThrownBy(() -> new RetryInterceptor(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_succeed_on_first_try() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(2))
                .build();

        try (okhttp3.Response response = client.newCall(
                new Request.Builder().url(server.url("/")).build()).execute()) {
            assertThat(response.code()).isEqualTo(200);
        }
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void should_not_retry_on_4xx() throws IOException {
        server.enqueue(new MockResponse().setResponseCode(400));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(2))
                .build();

        try (okhttp3.Response response = client.newCall(
                new Request.Builder().url(server.url("/")).build()).execute()) {
            assertThat(response.code()).isEqualTo(400);
        }
        assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    void should_propagate_exception_when_max_retries_is_zero() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new RetryInterceptor(0))
                .build();

        assertThatThrownBy(() -> client.newCall(
                new Request.Builder().url(server.url("/")).build()).execute())
                .isInstanceOf(IOException.class);
    }
}

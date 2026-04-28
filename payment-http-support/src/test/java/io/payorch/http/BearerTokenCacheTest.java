package io.payorch.http;

import io.payorch.core.exception.ProviderAuthException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BearerTokenCacheTest {

    @Test
    void should_call_supplier_once_on_first_get() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> fetcher = () -> {
            calls.incrementAndGet();
            return "token-1";
        };

        BearerTokenCache cache = new BearerTokenCache("test", fetcher, Duration.ofMinutes(1));
        String token = cache.get();

        assertThat(token).isEqualTo("token-1");
        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void should_return_cached_token_on_second_get() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> fetcher = () -> {
            calls.incrementAndGet();
            return "token-1";
        };

        BearerTokenCache cache = new BearerTokenCache("test", fetcher, Duration.ofMinutes(1));
        cache.get();
        cache.get();

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void should_refresh_after_invalidate() {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> fetcher = () -> "token-" + calls.incrementAndGet();

        BearerTokenCache cache = new BearerTokenCache("test", fetcher, Duration.ofMinutes(1));
        String first = cache.get();
        cache.invalidate();
        String second = cache.get();

        assertThat(first).isEqualTo("token-1");
        assertThat(second).isEqualTo("token-2");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    void should_refresh_when_ttl_expired() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> fetcher = () -> "token-" + calls.incrementAndGet();

        BearerTokenCache cache = new BearerTokenCache("test", fetcher, Duration.ofMillis(50));
        cache.get();
        Thread.sleep(100);
        String refreshed = cache.get();

        assertThat(calls.get()).isEqualTo(2);
        assertThat(refreshed).isEqualTo("token-2");
    }

    @Test
    void should_throw_when_supplier_returns_null() {
        BearerTokenCache cache = new BearerTokenCache("test", () -> null, Duration.ofMinutes(1));

        assertThatThrownBy(cache::get)
                .isInstanceOf(ProviderAuthException.class);
    }

    @Test
    void should_throw_when_supplier_returns_blank() {
        BearerTokenCache cache = new BearerTokenCache("test", () -> "  ", Duration.ofMinutes(1));

        assertThatThrownBy(cache::get)
                .isInstanceOf(ProviderAuthException.class);
    }

    @Test
    void should_call_supplier_only_once_under_concurrent_access() throws InterruptedException {
        AtomicInteger calls = new AtomicInteger();
        Supplier<String> fetcher = () -> {
            try {
                Thread.sleep(10);
            } catch (InterruptedException ignored) {
            }
            return "token-" + calls.incrementAndGet();
        };

        BearerTokenCache cache = new BearerTokenCache("test", fetcher, Duration.ofMinutes(1));
        int threads = 10;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done  = new CountDownLatch(threads);

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                exec.submit(() -> {
                    try {
                        start.await();
                        cache.get();
                    } catch (InterruptedException ignored) {
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            done.await();
        }

        assertThat(calls.get()).isEqualTo(1);
    }

    @Test
    void should_throw_on_null_constructor_args() {
        assertThatThrownBy(() -> new BearerTokenCache(null, () -> "t", Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BearerTokenCache("p", null, Duration.ofMinutes(1)))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BearerTokenCache("p", () -> "t", null))
                .isInstanceOf(NullPointerException.class);
    }
}

package io.payorch.http;

import io.payorch.core.exception.ProviderAuthException;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe cache for provider-issued Bearer tokens with automatic TTL-based refresh.
 *
 * <p>Usage pattern — in any {@code XxxClient} that requires a dynamic token:
 * <pre>{@code
 * private final BearerTokenCache tokenCache = new BearerTokenCache(
 *     "campay", this::fetchToken, Duration.ofSeconds(3500)
 * );
 *
 * Map<String, String> authHeaders() {
 *     return Map.of("Authorization", "Token " + tokenCache.get());
 * }
 * }</pre>
 *
 * <p>The {@code fetcher} is called lazily on first use and again whenever the
 * cached token has expired. Concurrent refreshes are serialized — only one
 * thread fetches at a time; others wait and reuse the result (double-checked
 * locking on a {@code synchronized} block).
 *
 * @since 0.1.0
 */
public final class BearerTokenCache {

    private final String providerName;
    private final Supplier<String> fetcher;
    private final Duration ttl;

    private volatile String  cachedToken;
    private volatile Instant expiry = Instant.EPOCH;

    /**
     * Constructs a token cache.
     *
     * @param providerName the provider name — used in error messages
     * @param fetcher      called to fetch a fresh token when the cache is empty or expired
     * @param ttl          how long a token remains valid before a refresh is triggered
     * @throws NullPointerException if any argument is null
     */
    public BearerTokenCache(String providerName, Supplier<String> fetcher, Duration ttl) {
        this.providerName = Objects.requireNonNull(providerName, "providerName must not be null");
        this.fetcher      = Objects.requireNonNull(fetcher, "fetcher must not be null");
        this.ttl          = Objects.requireNonNull(ttl, "ttl must not be null");
    }

    /**
     * Returns a valid Bearer token, refreshing it if expired.
     *
     * @return the current Bearer token, never null or blank
     * @throws ProviderAuthException if the fetcher returns null or blank
     * @since 0.1.0
     */
    public String get() {
        if (Instant.now().isBefore(expiry)) {
            return cachedToken;
        }
        return refresh();
    }

    /**
     * Invalidates the cached token immediately, forcing a fetch on the next {@link #get()} call.
     * Call this after receiving a 401 from the provider.
     *
     * @since 0.1.0
     */
    public void invalidate() {
        this.cachedToken = null;
        this.expiry      = Instant.EPOCH;
    }

    // ── Private ───────────────────────────────────────────────────────────────

    private synchronized String refresh() {
        if (Instant.now().isBefore(expiry)) {
            return cachedToken; // another thread already refreshed
        }
        String token = fetcher.get();
        if (token == null || token.isBlank()) {
            throw new ProviderAuthException(providerName,
                    new IllegalStateException("token fetcher returned null or blank"));
        }
        this.cachedToken = token;
        this.expiry      = Instant.now().plus(ttl);
        return cachedToken;
    }
}
package io.payorch.provider.pawapay;

import io.payorch.core.exception.WebhookValidationException;
import io.payorch.webhook.Rfc9421Verifier;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Verifies PawaPay webhook signatures using RFC 9421 HTTP Message Signatures.
 *
 * <p>Fetches and caches PawaPay's RSA/EC public key from {@code GET /public-key/http}
 * with a 24-hour TTL. The cache is invalidated on signature failure to handle
 * key rotation without a deployment.
 *
 * @since 0.1.0
 */
final class PawaPayWebhookVerifier {

    private static final Duration KEY_TTL = Duration.ofHours(24);

    private final PawaPayClient client;

    private volatile PublicKey cachedKey;
    private volatile Instant   cachedAt;

    /**
     * Constructs a verifier backed by the given PawaPay HTTP client.
     *
     * @param client the PawaPay client used to fetch the public key
     * @throws NullPointerException if {@code client} is null
     */
    PawaPayWebhookVerifier(PawaPayClient client) {
        this.client = Objects.requireNonNull(client, "client must not be null");
    }

    /**
     * Verifies the RFC 9421 signature of an inbound PawaPay webhook.
     *
     * <p>If the signature check fails after a fresh key fetch, the cache is
     * invalidated so the next call re-fetches — handles PawaPay key rotation.
     *
     * @param rawBody the unmodified raw request body
     * @param headers the HTTP request headers
     * @return {@code true} if the signature is valid
     * @throws WebhookValidationException if the public key is unavailable or
     *                                    if signature headers are missing/malformed
     */
    boolean verify(String rawBody, Map<String, String> headers) {
        PublicKey key = resolveKey().orElseThrow(() ->
                new WebhookValidationException(
                        "PawaPay: RSA public key unavailable — cannot verify webhook signature"));

        boolean valid = Rfc9421Verifier.verify(rawBody, headers, key);
        if (!valid) {
            invalidateCache();
        }
        return valid;
    }

    // ── Cache management ──────────────────────────────────────────────────────

    private Optional<PublicKey> resolveKey() {
        if (isCacheValid()) {
            return Optional.of(cachedKey);
        }
        return fetchAndCache();
    }

    private boolean isCacheValid() {
        return cachedKey != null && cachedAt != null
                && Instant.now().isBefore(cachedAt.plus(KEY_TTL));
    }

    private synchronized Optional<PublicKey> fetchAndCache() {
        if (isCacheValid()) return Optional.of(cachedKey); // double-checked locking
        Optional<PublicKey> fetched = client.fetchPublicKey();
        fetched.ifPresent(key -> {
            this.cachedKey = key;
            this.cachedAt  = Instant.now();
        });
        return fetched;
    }

    private void invalidateCache() {
        this.cachedKey = null;
        this.cachedAt  = null;
    }
}
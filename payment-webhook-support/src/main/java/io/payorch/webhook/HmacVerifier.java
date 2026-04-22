package io.payorch.webhook;

import io.payorch.core.exception.WebhookValidationException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Verifies HMAC-SHA256 webhook signatures.
 *
 * <p>The computed signature is compared using a constant-time equality check
 * to prevent timing attacks. This class is stateless — all methods are static.
 *
 * @since 0.1.0
 */
public final class HmacVerifier {

    private static final String ALGORITHM = "HmacSHA256";

    private HmacVerifier() {
    }

    /**
     * Verifies that {@code signature} matches the HMAC-SHA256 of {@code payload}
     * signed with {@code secret}.
     *
     * <p>Both the incoming signature and the computed value are compared as
     * lowercase hex strings using a constant-time check.
     *
     * @param payload   the raw request body to verify
     * @param secret    the shared secret key
     * @param signature the signature from the webhook header (hex-encoded)
     * @return {@code true} if the signature is valid
     * @throws NullPointerException       if any argument is null
     * @throws WebhookValidationException if the HMAC algorithm is unavailable
     * @since 0.1.0
     */
    public static boolean verify(String payload, String secret, String signature) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        Objects.requireNonNull(signature, "signature must not be null");

        String computed = compute(payload, secret);
        return MessageDigest.isEqual(
                computed.getBytes(StandardCharsets.UTF_8),
                signature.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Computes the HMAC-SHA256 of {@code payload} with {@code secret},
     * returned as a lowercase hex string.
     *
     * @param payload the data to sign
     * @param secret  the shared secret key
     * @return lowercase hex-encoded HMAC-SHA256
     * @throws NullPointerException       if any argument is null
     * @throws WebhookValidationException if the HMAC algorithm is unavailable
     * @since 0.1.0
     */
    public static String compute(String payload, String secret) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(secret, "secret must not be null");
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new WebhookValidationException(
                    "HMAC-SHA256 computation failed: " + e.getMessage());
        }
    }
}
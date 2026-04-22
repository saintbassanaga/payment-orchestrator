package io.payorch.webhook;

import io.payorch.core.exception.WebhookValidationException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Verifies HTTP Message Signatures according to RFC 9421.
 *
 * <p>Verification steps:
 * <ol>
 *   <li>Content-Digest — SHA-256 or SHA-512 of the raw request body.</li>
 *   <li>Anti-replay — rejects signatures whose {@code created} timestamp is
 *       outside the configured replay window.</li>
 *   <li>Signature base — reconstructed from covered components per RFC 9421 §2.5.</li>
 *   <li>RSA/ECDSA signature — verified against the provider's public key.</li>
 * </ol>
 *
 * <p>This class is stateless. All methods are static.
 *
 * @since 0.1.0
 */
public final class Rfc9421Verifier {

    /** Default anti-replay window: 5 minutes. */
    public static final long DEFAULT_REPLAY_WINDOW_SECONDS = 300;

    private static final Pattern CREATED_PATTERN   = Pattern.compile("created=(\\d+)");
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("\"([^\"]+)\"");
    private static final Pattern ALG_PATTERN        = Pattern.compile("alg=\"([^\"]+)\"");

    private Rfc9421Verifier() {
    }

    /**
     * Verifies the RFC 9421 signature of an inbound webhook request.
     *
     * @param rawBody           the unmodified raw request body
     * @param headers           the HTTP request headers (case-insensitive lookup expected from caller)
     * @param publicKey         the provider's public key (RSA or EC)
     * @param replayWindowSecs  maximum age in seconds of a valid signature
     * @return {@code true} if the signature is valid
     * @throws NullPointerException       if any argument is null
     * @throws WebhookValidationException if a structural violation is detected
     *                                    (missing headers, malformed input, replay detected)
     * @since 0.1.0
     */
    public static boolean verify(String rawBody, Map<String, String> headers,
                                  PublicKey publicKey, long replayWindowSecs) {
        Objects.requireNonNull(rawBody, "rawBody must not be null");
        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(publicKey, "publicKey must not be null");

        String sigInput = header(headers, "signature-input");
        String sigHeader = header(headers, "signature");

        if (sigInput == null || sigHeader == null) {
            throw new WebhookValidationException(
                    "RFC 9421: missing Signature or Signature-Input header");
        }

        verifyContentDigest(rawBody, headers);

        int eqIdx = sigInput.indexOf('=');
        if (eqIdx < 0) {
            throw new WebhookValidationException(
                    "RFC 9421: malformed Signature-Input — no '=' separator");
        }
        String label      = sigInput.substring(0, eqIdx).trim();
        String inputValue = sigInput.substring(eqIdx + 1).trim();

        checkReplay(inputValue, replayWindowSecs);

        String sigBase = buildSignatureBase(headers, inputValue);

        String prefix = label + "=:";
        if (!sigHeader.startsWith(prefix) || !sigHeader.endsWith(":")) {
            throw new WebhookValidationException(
                    "RFC 9421: malformed Signature header for label '%s'".formatted(label));
        }
        byte[] sigBytes = Base64.getDecoder().decode(
                sigHeader.substring(prefix.length(), sigHeader.length() - 1));

        try {
            java.security.Signature sig = resolveAlgorithm(inputValue);
            sig.initVerify(publicKey);
            sig.update(sigBase.getBytes(StandardCharsets.UTF_8));
            return sig.verify(sigBytes);
        } catch (WebhookValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookValidationException(
                    "RFC 9421: signature verification error — " + e.getMessage());
        }
    }

    /**
     * Verifies the RFC 9421 signature using the default replay window of 5 minutes.
     *
     * @param rawBody   the unmodified raw request body
     * @param headers   the HTTP request headers
     * @param publicKey the provider's public key
     * @return {@code true} if the signature is valid
     * @throws NullPointerException       if any argument is null
     * @throws WebhookValidationException if verification fails
     * @since 0.1.0
     */
    public static boolean verify(String rawBody, Map<String, String> headers, PublicKey publicKey) {
        return verify(rawBody, headers, publicKey, DEFAULT_REPLAY_WINDOW_SECONDS);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static void verifyContentDigest(String rawBody, Map<String, String> headers) {
        String digestHeader = header(headers, "content-digest");
        if (digestHeader == null || digestHeader.isBlank()) {
            return; // not a covered component — skip
        }

        String algorithm;
        String expected;

        if (digestHeader.startsWith("sha-512=:") && digestHeader.endsWith(":")) {
            algorithm = "SHA-512";
            expected  = digestHeader.substring("sha-512=:".length(), digestHeader.length() - 1);
        } else if (digestHeader.startsWith("sha-512=")) {
            algorithm = "SHA-512";
            expected  = digestHeader.substring("sha-512=".length());
        } else if (digestHeader.startsWith("sha-256=:") && digestHeader.endsWith(":")) {
            algorithm = "SHA-256";
            expected  = digestHeader.substring("sha-256=:".length(), digestHeader.length() - 1);
        } else if (digestHeader.startsWith("sha-256=")) {
            algorithm = "SHA-256";
            expected  = digestHeader.substring("sha-256=".length());
        } else {
            throw new WebhookValidationException(
                    "RFC 9421: unsupported Content-Digest format: " + digestHeader);
        }

        try {
            byte[] bodyBytes = rawBody.getBytes(StandardCharsets.UTF_8);
            byte[] hash      = MessageDigest.getInstance(algorithm).digest(bodyBytes);
            String computed  = Base64.getEncoder().encodeToString(hash);
            if (!expected.equals(computed)) {
                throw new WebhookValidationException(
                        "RFC 9421: Content-Digest mismatch — expected=%s computed=%s"
                                .formatted(expected, computed));
            }
        } catch (WebhookValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new WebhookValidationException(
                    "RFC 9421: Content-Digest verification error — " + e.getMessage());
        }
    }

    private static void checkReplay(String inputValue, long windowSecs) {
        Matcher m = CREATED_PATTERN.matcher(inputValue);
        if (!m.find()) return; // no created= — skip anti-replay
        try {
            long created = Long.parseLong(m.group(1));
            long skew    = Math.abs(Instant.now().getEpochSecond() - created);
            if (skew > windowSecs) {
                throw new WebhookValidationException(
                        "RFC 9421: signature replay rejected — created=%d skew=%ds"
                                .formatted(created, skew));
            }
        } catch (NumberFormatException e) {
            // malformed created= — skip
        }
    }

    private static String buildSignatureBase(Map<String, String> headers, String inputValue) {
        List<String> components = parseCoveredComponents(inputValue);
        StringBuilder base = new StringBuilder();
        for (String comp : components) {
            String value = header(headers, comp);
            if (value == null) {
                throw new WebhookValidationException(
                        "RFC 9421: Signature-Input covers '%s' but header is absent"
                                .formatted(comp));
            }
            base.append('"').append(comp).append("\": ").append(value).append('\n');
        }
        base.append("\"@signature-params\": ").append(inputValue);
        return base.toString();
    }

    private static List<String> parseCoveredComponents(String inputValue) {
        List<String> result = new ArrayList<>();
        int start = inputValue.indexOf('(');
        int end   = inputValue.indexOf(')');
        if (start < 0 || end <= start) return result;
        Matcher m = COMPONENT_PATTERN.matcher(inputValue.substring(start + 1, end));
        while (m.find()) result.add(m.group(1));
        return result;
    }

    /**
     * Maps the RFC 9421 {@code alg=} parameter to a JCA {@link java.security.Signature}.
     * Supported: {@code ecdsa-p256-sha256}, {@code ecdsa-p384-sha384},
     * {@code rsa-v1_5-sha256}, {@code rsa-pss-sha512}.
     */
    private static java.security.Signature resolveAlgorithm(String inputValue) throws Exception {
        Matcher m   = ALG_PATTERN.matcher(inputValue);
        String alg  = m.find() ? m.group(1).toLowerCase() : "rsa-v1_5-sha256";
        return switch (alg) {
            case "ecdsa-p256-sha256" -> java.security.Signature.getInstance("SHA256withECDSA");
            case "ecdsa-p384-sha384" -> java.security.Signature.getInstance("SHA384withECDSA");
            case "rsa-pss-sha512" -> {
                java.security.Signature s = java.security.Signature.getInstance("RSASSA-PSS");
                s.setParameter(new PSSParameterSpec(
                        "SHA-512", "MGF1", MGF1ParameterSpec.SHA512, 64, 1));
                yield s;
            }
            default -> java.security.Signature.getInstance("SHA256withRSA");
        };
    }

    /** Case-insensitive header lookup. */
    private static String header(Map<String, String> headers, String name) {
        return headers.entrySet().stream()
                .filter(e -> e.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }
}
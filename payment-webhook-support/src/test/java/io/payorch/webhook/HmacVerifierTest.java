package io.payorch.webhook;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacVerifierTest {

    // Known test vector — SHA256("hello" with key "secret")
    private static final String PAYLOAD = "hello";
    private static final String SECRET  = "secret";
    // Expected: openssl dgst -sha256 -hmac "secret" <<< "hello" (without newline)
    private static final String KNOWN_HEX = "88aab3ede8d3adf94d26ab90d3bafd4a2083070c3bcce9c014ee04a443847c0b";

    @Test
    void should_compute_known_hmac_sha256() {
        assertThat(HmacVerifier.compute(PAYLOAD, SECRET)).isEqualTo(KNOWN_HEX);
    }

    @Test
    void should_return_true_for_matching_signature() {
        assertThat(HmacVerifier.verify(PAYLOAD, SECRET, KNOWN_HEX)).isTrue();
    }

    @Test
    void should_return_true_for_uppercase_signature() {
        assertThat(HmacVerifier.verify(PAYLOAD, SECRET, KNOWN_HEX.toUpperCase())).isTrue();
    }

    @Test
    void should_return_false_for_wrong_signature() {
        assertThat(HmacVerifier.verify(PAYLOAD, SECRET, "deadbeef")).isFalse();
    }

    @Test
    void should_return_false_for_wrong_secret() {
        assertThat(HmacVerifier.verify(PAYLOAD, "wrong-secret", KNOWN_HEX)).isFalse();
    }

    @Test
    void should_return_false_for_tampered_payload() {
        assertThat(HmacVerifier.verify("tampered", SECRET, KNOWN_HEX)).isFalse();
    }

    @Test
    void should_not_throw_on_different_length_signatures() {
        // Constant-time comparison must not throw on length mismatch
        assertThat(HmacVerifier.verify(PAYLOAD, SECRET, "short")).isFalse();
        assertThat(HmacVerifier.verify(PAYLOAD, SECRET, KNOWN_HEX + "extra")).isFalse();
    }

    @Test
    void should_throw_on_null_arguments() {
        assertThatThrownBy(() -> HmacVerifier.verify(null, SECRET, KNOWN_HEX))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HmacVerifier.verify(PAYLOAD, null, KNOWN_HEX))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HmacVerifier.verify(PAYLOAD, SECRET, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> HmacVerifier.compute(null, SECRET))
                .isInstanceOf(NullPointerException.class);
    }
}

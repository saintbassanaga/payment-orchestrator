package io.payorch.core.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static io.payorch.core.util.AmountToWords.Language.EN;
import static io.payorch.core.util.AmountToWords.Language.FR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AmountToWordsTest {

    // ── French — whole numbers ────────────────────────────────────────────────

    @ParameterizedTest(name = "FR {0} → {1}")
    @CsvSource({
        "0,        Zéro",
        "1,        Un",
        "11,       Onze",
        "16,       Seize",
        "21,       Vingt et un",
        "70,       Soixante-dix",
        "71,       Soixante et onze",
        "80,       Quatre-vingts",
        "81,       Quatre-vingt-un",
        "90,       Quatre-vingt-dix",
        "91,       Quatre-vingt-onze",
        "99,       Quatre-vingt-dix-neuf",
        "100,      Cent",
        "101,      Cent un",
        "200,      Deux cents",
        "201,      Deux cent un",
        "500,      Cinq cents",
        "1000,     Mille",
        "1001,     Mille un",
        "2000,     Deux mille",
        "5000,     Cinq mille",
        "100000,   Cent mille",
        "500000,   Cinq cent mille",
        "1000000,  Un million",
        "2000000,  Deux millions",
        "1500000,  Un million cinq cent mille",
        "1000000000, Un milliard",
        "2500000000, Deux milliards cinq cents millions"
    })
    void should_convert_french_whole_numbers(long amount, String expected) {
        assertThat(AmountToWords.inLetter(amount, FR)).isEqualTo(expected);
    }

    @Test
    void should_not_pluralize_mille_in_french() {
        assertThat(AmountToWords.inLetter(3_000L, FR)).isEqualTo("Trois mille");
        assertThat(AmountToWords.inLetter(1_000L, FR)).isEqualTo("Mille");
    }

    @Test
    void should_lose_s_on_cents_before_mille_in_french() {
        assertThat(AmountToWords.inLetter(200L, FR)).isEqualTo("Deux cents");
        assertThat(AmountToWords.inLetter(200_000L, FR)).isEqualTo("Deux cent mille");
    }

    // ── English — whole numbers ───────────────────────────────────────────────

    @ParameterizedTest(name = "EN {0} → {1}")
    @CsvSource({
        "0,        Zero",
        "1,        One",
        "11,       Eleven",
        "21,       Twenty-one",
        "70,       Seventy",
        "80,       Eighty",
        "99,       Ninety-nine",
        "100,      One hundred",
        "200,      Two hundred",
        "201,      Two hundred one",
        "1000,     One thousand",
        "5000,     Five thousand",
        "100000,   One hundred thousand",
        "500000,   Five hundred thousand",
        "1000000,  One million",
        "2000000,  Two millions",
        "1000000000, One billion"
    })
    void should_convert_english_whole_numbers(long amount, String expected) {
        assertThat(AmountToWords.inLetter(amount, EN)).isEqualTo(expected);
    }

    // ── French — BigDecimal (centimes) ────────────────────────────────────────

    @Test
    void should_omit_centimes_when_zero_in_french() {
        assertThat(AmountToWords.inLetter(new BigDecimal("5000.00"), FR))
                .isEqualTo("Cinq mille");
    }

    @Test
    void should_include_centimes_in_french() {
        assertThat(AmountToWords.inLetter(new BigDecimal("1500.50"), FR))
                .isEqualTo("Mille cinq cents et cinquante centimes");
    }

    @Test
    void should_use_singular_centime_in_french() {
        assertThat(AmountToWords.inLetter(new BigDecimal("10.01"), FR))
                .isEqualTo("Dix et un centime");
    }

    // ── English — BigDecimal (cents) ──────────────────────────────────────────

    @Test
    void should_omit_cents_when_zero_in_english() {
        assertThat(AmountToWords.inLetter(new BigDecimal("5000.00"), EN))
                .isEqualTo("Five thousand");
    }

    @Test
    void should_include_cents_in_english() {
        assertThat(AmountToWords.inLetter(new BigDecimal("1500.50"), EN))
                .isEqualTo("One thousand five hundred and fifty cents");
    }

    @Test
    void should_use_singular_cent_in_english() {
        assertThat(AmountToWords.inLetter(new BigDecimal("10.01"), EN))
                .isEqualTo("Ten and one cent");
    }

    // ── Currency labels ───────────────────────────────────────────────────────

    @Test
    void should_append_xaf_label_in_french() {
        assertThat(AmountToWords.inLetter(new BigDecimal("5000"), "XAF", FR))
                .isEqualTo("Cinq mille Francs CFA");
    }

    @Test
    void should_append_xof_label_in_french() {
        assertThat(AmountToWords.inLetter(new BigDecimal("500000"), "XOF", FR))
                .isEqualTo("Cinq cent mille Francs CFA");
    }

    @Test
    void should_append_usd_label_via_jdk_currency() {
        String result = AmountToWords.inLetter(new BigDecimal("5000"), "USD", EN);
        assertThat(result).startsWith("Five thousand").containsIgnoringCase("dollar");
    }

    @Test
    void should_append_eur_label_via_jdk_currency_in_french() {
        String result = AmountToWords.inLetter(new BigDecimal("100"), "EUR", FR);
        assertThat(result).startsWith("Cent").containsIgnoringCase("euro");
    }

    @Test
    void should_append_unknown_currency_verbatim() {
        assertThat(AmountToWords.inLetter(new BigDecimal("100"), "ZZZ", FR))
                .isEqualTo("Cent ZZZ");
        assertThat(AmountToWords.inLetter(new BigDecimal("100"), "ZZZ", EN))
                .isEqualTo("One hundred ZZZ");
    }

    // ── Guards ────────────────────────────────────────────────────────────────

    @Test
    void should_throw_on_negative_amount() {
        assertThatThrownBy(() -> AmountToWords.inLetter(-1L, FR))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AmountToWords.inLetter(new BigDecimal("-1"), EN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void should_throw_on_null_amount() {
        assertThatThrownBy(() -> AmountToWords.inLetter((BigDecimal) null, FR))
                .isInstanceOf(NullPointerException.class);
    }
}

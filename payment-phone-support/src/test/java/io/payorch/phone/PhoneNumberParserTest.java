package io.payorch.phone;

import io.payorch.core.exception.InvalidPaymentRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhoneNumberParserTest {

    @ParameterizedTest
    @CsvSource({
            "+237670000000, +237670000000, 237670000000, CM",
            "+233244000000, +233244000000, 233244000000, GH",
            "+221770000000, +221770000000, 221770000000, SN"
    })
    void should_parse_e164_without_region_hint(
            String raw, String expectedE164, String expectedDigits, String expectedCountry) {
        ParsedPhoneNumber result = PhoneNumberParser.parse(raw);

        assertThat(result.e164()).isEqualTo(expectedE164);
        assertThat(result.e164Digits()).isEqualTo(expectedDigits);
        assertThat(result.countryCode()).isEqualTo(expectedCountry);
    }

    @ParameterizedTest
    @CsvSource({
            "+237670000000, CM, +237670000000, 237670000000",
            "670000000,     CM, +237670000000, 237670000000"
    })
    void should_parse_local_format_with_region_hint(
            String raw, String region, String expectedE164, String expectedDigits) {
        ParsedPhoneNumber result = PhoneNumberParser.parse(raw, region);

        assertThat(result.e164()).isEqualTo(expectedE164);
        assertThat(result.e164Digits()).isEqualTo(expectedDigits);
        assertThat(result.countryCode()).isEqualTo(region);
    }

    @Test
    void should_extract_national_number() {
        ParsedPhoneNumber result = PhoneNumberParser.parse("+237670000000");
        assertThat(result.nationalNumber()).isNotBlank();
    }

    @Test
    void should_throw_for_invalid_number() {
        assertThatThrownBy(() -> PhoneNumberParser.parse("000", "CM"))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("000");
    }

    @Test
    void should_throw_for_null_number() {
        assertThatThrownBy(() -> PhoneNumberParser.parse(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_for_null_region() {
        assertThatThrownBy(() -> PhoneNumberParser.parse("+237670000000", null))
                .isInstanceOf(NullPointerException.class);
    }
}

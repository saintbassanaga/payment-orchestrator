package io.payorch.phone;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedPhoneNumberTest {

    @Test
    void should_construct_with_all_fields() {
        ParsedPhoneNumber phone = new ParsedPhoneNumber(
                "+237671000001", "237671000001", "CM", "671000001");

        assertThat(phone.e164()).isEqualTo("+237671000001");
        assertThat(phone.e164Digits()).isEqualTo("237671000001");
        assertThat(phone.countryCode()).isEqualTo("CM");
        assertThat(phone.nationalNumber()).isEqualTo("671000001");
    }

    @Test
    void should_throw_on_null_e164() {
        assertThatThrownBy(() -> new ParsedPhoneNumber(null, "237671000001", "CM", "671000001"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_throw_on_null_country_code() {
        assertThatThrownBy(() -> new ParsedPhoneNumber("+237671000001", "237671000001", null, "671000001"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void should_parse_real_cameroonian_number_via_parser() {
        ParsedPhoneNumber phone = PhoneNumberParser.parse("+237671000001");

        assertThat(phone.e164()).isEqualTo("+237671000001");
        assertThat(phone.e164Digits()).isEqualTo("237671000001");
        assertThat(phone.countryCode()).isEqualTo("CM");
        assertThat(phone.nationalNumber()).isNotBlank();
    }
}

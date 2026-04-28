package io.payorch.phone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OperatorDetectorTest {

    @ParameterizedTest
    @CsvSource({
            "+237670000000, MTN",
            "+237690000000, ORANGE",
            "+237655000000, ORANGE",
            "+233244000000, MTN",
            "+221770000000, ORANGE"
    })
    void should_detect_operator_via_carrier_database(String e164, String expectedOperator) {
        ParsedPhoneNumber phone = PhoneNumberParser.parse(e164);
        assertThat(OperatorDetector.detect(phone))
                .isPresent()
                .hasValue(MobileOperator.valueOf(expectedOperator));
    }

    @Test
    void should_return_empty_for_unknown_carrier() {
        ParsedPhoneNumber phone = PhoneNumberParser.parse("+12125551234");
        assertThat(OperatorDetector.detect(phone)).isEmpty();
    }

    @Test
    void should_throw_for_null_phone() {
        assertThatThrownBy(() -> OperatorDetector.detect(null))
                .isInstanceOf(NullPointerException.class);
    }
}

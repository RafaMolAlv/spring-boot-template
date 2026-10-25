package es.nextdigital.demo.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class IbanValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "ES0690000001000000000001",
            "es06 9000 0001 0000 0000 0001",
            "DE89370400440532013000",
            "GB29NWBK60161331926819"
    })
    void validIbans(String iban) {
        assertThat(IbanValidator.isValid(iban)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "ES0790000001000000000001", // dígito de control incorrecto
            "ES06900000010000000000",   // longitud incorrecta para el checksum
            "1234567890123456",         // sin código de país
            "ES06-9000-0001-0000"       // caracteres no permitidos
    })
    void invalidIbans(String iban) {
        assertThat(IbanValidator.isValid(iban)).isFalse();
    }

    @Test
    void extractsSpanishBankCode() {
        assertThat(IbanValidator.spanishBankCode("ES4491000001000000000004")).isEqualTo("9100");
        assertThat(IbanValidator.spanishBankCode("DE89370400440532013000")).isNull();
    }
}

package es.nextdigital.demo.controller.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/** Petición de retirada o ingreso de efectivo. */
public record AmountRequest(
        @NotNull @Pattern(regexp = "\\d{4}", message = "debe tener 4 dígitos") String pin,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount) {
}

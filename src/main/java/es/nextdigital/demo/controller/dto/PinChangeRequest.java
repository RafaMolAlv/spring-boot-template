package es.nextdigital.demo.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record PinChangeRequest(
        @NotNull @Pattern(regexp = "\\d{4}", message = "debe tener 4 dígitos") String currentPin,
        @NotNull @Pattern(regexp = "\\d{4}", message = "debe tener 4 dígitos") String newPin) {
}

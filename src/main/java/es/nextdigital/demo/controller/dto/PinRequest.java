package es.nextdigital.demo.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Solo PIN: para consultas. Va en el cuerpo para no exponerlo en la URL ni en logs de acceso. */
public record PinRequest(
        @NotNull @Pattern(regexp = "\\d{4}", message = "debe tener 4 dígitos") String pin) {
}

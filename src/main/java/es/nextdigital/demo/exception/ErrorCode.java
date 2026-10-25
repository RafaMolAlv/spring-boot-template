package es.nextdigital.demo.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "La tarjeta no existe"),
    ATM_NOT_FOUND(HttpStatus.NOT_FOUND, "El cajero no existe"),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "La cuenta no existe"),
    INVALID_PIN(HttpStatus.UNAUTHORIZED, "PIN incorrecto"),
    CARD_NOT_ACTIVATED(HttpStatus.FORBIDDEN, "La tarjeta no está activada"),
    CARD_ALREADY_ACTIVATED(HttpStatus.CONFLICT, "La tarjeta ya está activada"),
    ACCOUNT_NOT_OWNED(HttpStatus.FORBIDDEN, "La cuenta no pertenece al titular de la tarjeta"),
    SAME_PIN(HttpStatus.BAD_REQUEST, "El nuevo PIN debe ser distinto del actual"),
    INSUFFICIENT_FUNDS(HttpStatus.UNPROCESSABLE_ENTITY, "Fondos insuficientes"),
    WITHDRAWAL_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "El importe supera el límite de retirada de la tarjeta"),
    DEPOSIT_NOT_ALLOWED(HttpStatus.UNPROCESSABLE_ENTITY, "Solo se puede ingresar dinero en cajeros de nuestra entidad"),
    INVALID_IBAN(HttpStatus.BAD_REQUEST, "El IBAN de destino no es válido"),
    SAME_ACCOUNT_TRANSFER(HttpStatus.BAD_REQUEST, "La cuenta de destino debe ser distinta de la de origen"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "La cuenta se ha modificado durante la operación, inténtelo de nuevo"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Datos de entrada no válidos");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

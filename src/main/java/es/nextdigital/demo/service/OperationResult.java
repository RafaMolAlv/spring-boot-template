package es.nextdigital.demo.service;

import java.math.BigDecimal;

/**
 * Resultado de una operación de efectivo o transferencia.
 *
 * @param iban    cuenta afectada
 * @param amount  importe de la operación
 * @param fee     comisión aplicada (0 si no hay)
 * @param balance saldo resultante de la cuenta
 */
public record OperationResult(String iban, BigDecimal amount, BigDecimal fee, BigDecimal balance) {
}

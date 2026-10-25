package es.nextdigital.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * @param code                código de entidad de nuestro banco
 * @param externalTransferFee comisión por transferencia a otra entidad
 */
@ConfigurationProperties(prefix = "bank")
public record BankProperties(String code, BigDecimal externalTransferFee) {
}

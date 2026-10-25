package es.nextdigital.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.math.BigDecimal;

@Entity
public class Atm {

    @Id
    private Long id;

    /** Código de la entidad propietaria del cajero (4 dígitos, como en el IBAN español). */
    @Column(name = "bank_code", nullable = false, length = 4)
    private String bankCode;

    /** Comisión que aplica la entidad del cajero por cada retirada. */
    @Column(name = "withdrawal_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal withdrawalFee;

    protected Atm() {
    }

    public Atm(Long id, String bankCode, BigDecimal withdrawalFee) {
        this.id = id;
        this.bankCode = bankCode;
        this.withdrawalFee = withdrawalFee;
    }

    public Long getId() {
        return id;
    }

    public String getBankCode() {
        return bankCode;
    }

    public BigDecimal getWithdrawalFee() {
        return withdrawalFee;
    }
}

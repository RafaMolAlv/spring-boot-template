package es.nextdigital.demo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

import java.math.BigDecimal;

@Entity
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number", nullable = false, unique = true, length = 19)
    private String number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CardType type;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    /** Hash BCrypt del PIN. Nunca se guarda el PIN en texto plano. */
    @Column(name = "pin_hash", nullable = false)
    private String pinHash;

    @Column(nullable = false)
    private boolean active;

    /** Importe máximo que se puede retirar en una operación. */
    @Column(name = "withdrawal_limit", nullable = false, precision = 15, scale = 2)
    private BigDecimal withdrawalLimit;

    /** Crédito máximo disponible (solo tarjetas de crédito). */
    @Column(name = "credit_limit", precision = 15, scale = 2)
    private BigDecimal creditLimit;

    protected Card() {
    }

    public Card(String number, CardType type, Account account, String pinHash, boolean active,
                BigDecimal withdrawalLimit, BigDecimal creditLimit) {
        this.number = number;
        this.type = type;
        this.account = account;
        this.pinHash = pinHash;
        this.active = active;
        this.withdrawalLimit = withdrawalLimit;
        this.creditLimit = creditLimit;
    }

    /**
     * Fondos de los que puede disponer el titular con esta tarjeta:
     * débito = saldo; crédito = saldo + crédito máximo (la cuenta puede quedar en negativo).
     */
    public BigDecimal availableFunds() {
        if (type == CardType.CREDIT && creditLimit != null) {
            return account.getBalance().add(creditLimit);
        }
        return account.getBalance();
    }

    public void activate(String newPinHash) {
        this.active = true;
        this.pinHash = newPinHash;
    }

    public void changePin(String newPinHash) {
        this.pinHash = newPinHash;
    }

    public Long getId() {
        return id;
    }

    public String getNumber() {
        return number;
    }

    public CardType getType() {
        return type;
    }

    public Account getAccount() {
        return account;
    }

    public String getPinHash() {
        return pinHash;
    }

    public boolean isActive() {
        return active;
    }

    public BigDecimal getWithdrawalLimit() {
        return withdrawalLimit;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }
}

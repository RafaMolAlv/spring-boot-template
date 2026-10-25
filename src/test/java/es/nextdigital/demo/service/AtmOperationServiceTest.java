package es.nextdigital.demo.service;

import es.nextdigital.demo.config.BankProperties;
import es.nextdigital.demo.domain.Account;
import es.nextdigital.demo.domain.Atm;
import es.nextdigital.demo.domain.Card;
import es.nextdigital.demo.domain.CardType;
import es.nextdigital.demo.domain.Customer;
import es.nextdigital.demo.domain.Movement;
import es.nextdigital.demo.domain.MovementType;
import es.nextdigital.demo.exception.BusinessException;
import es.nextdigital.demo.exception.ErrorCode;
import es.nextdigital.demo.repository.AccountRepository;
import es.nextdigital.demo.repository.AtmRepository;
import es.nextdigital.demo.repository.MovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AtmOperationServiceTest {

    private static final String OWN_BANK = "9000";
    private static final String CARD = "4000000000000001";
    private static final String PIN = "1234";
    private static final String ORIGIN_IBAN = "ES0690000001000000000001";
    private static final String INTERNAL_IBAN = "ES4990000001000000000003";
    private static final String EXTERNAL_IBAN = "ES4491000001000000000004";

    private final CardService cardService = mock(CardService.class);
    private final AtmRepository atmRepository = mock(AtmRepository.class);
    private final AccountRepository accountRepository = mock(AccountRepository.class);
    private final MovementRepository movementRepository = mock(MovementRepository.class);
    private final AtmOperationService service = new AtmOperationService(cardService, atmRepository,
            accountRepository, movementRepository, new BankProperties(OWN_BANK, new BigDecimal("1.50")));

    private Customer owner;
    private Account account;

    @BeforeEach
    void setUp() {
        owner = customer(1L);
        account = new Account(ORIGIN_IBAN, new BigDecimal("100.00"), owner);
        when(atmRepository.findById(1L)).thenReturn(Optional.of(new Atm(1L, OWN_BANK, BigDecimal.ZERO)));
        when(atmRepository.findById(2L)).thenReturn(Optional.of(new Atm(2L, "9100", new BigDecimal("2.00"))));
    }

    // ---------- Retiradas ----------

    @Test
    void debitWithdrawalWithEnoughBalance() {
        givenCard(CardType.DEBIT, "500", null);

        OperationResult result = service.withdraw(1L, CARD, PIN, new BigDecimal("40"));

        assertThat(result.balance()).isEqualByComparingTo("60.00");
        assertThat(result.fee()).isEqualByComparingTo("0");
        assertThat(savedTypes()).containsExactly(MovementType.WITHDRAWAL);
    }

    @Test
    void debitWithdrawalWithoutBalanceIsRejected() {
        givenCard(CardType.DEBIT, "500", null);

        assertError(() -> service.withdraw(1L, CARD, PIN, new BigDecimal("150")), ErrorCode.INSUFFICIENT_FUNDS);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
        verify(movementRepository, never()).save(any());
    }

    @Test
    void creditWithdrawalCanUseCreditLimit() {
        givenCard(CardType.CREDIT, "500", "300");

        OperationResult result = service.withdraw(1L, CARD, PIN, new BigDecimal("350"));

        assertThat(result.balance()).isEqualByComparingTo("-250.00");
    }

    @Test
    void creditWithdrawalAboveCreditLimitIsRejected() {
        givenCard(CardType.CREDIT, "1000", "300");

        assertError(() -> service.withdraw(1L, CARD, PIN, new BigDecimal("450")), ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    void withdrawalAboveCardLimitIsRejectedEvenWithBalance() {
        givenCard(CardType.DEBIT, "50", null);

        assertError(() -> service.withdraw(1L, CARD, PIN, new BigDecimal("60")), ErrorCode.WITHDRAWAL_LIMIT_EXCEEDED);
    }

    @Test
    void foreignAtmChargesFeeAsSeparateMovement() {
        givenCard(CardType.DEBIT, "500", null);

        OperationResult result = service.withdraw(2L, CARD, PIN, new BigDecimal("40"));

        assertThat(result.fee()).isEqualByComparingTo("2.00");
        assertThat(result.balance()).isEqualByComparingTo("58.00");
        assertThat(savedTypes()).containsExactly(MovementType.WITHDRAWAL, MovementType.FEE);
    }

    @Test
    void foreignAtmFeeCountsTowardsAvailableBalance() {
        givenCard(CardType.DEBIT, "500", null);

        assertError(() -> service.withdraw(2L, CARD, PIN, new BigDecimal("99")), ErrorCode.INSUFFICIENT_FUNDS);
    }

    @Test
    void unknownAtm() {
        when(atmRepository.findById(99L)).thenReturn(Optional.empty());

        assertError(() -> service.withdraw(99L, CARD, PIN, BigDecimal.TEN), ErrorCode.ATM_NOT_FOUND);
    }

    // ---------- Ingresos ----------

    @Test
    void depositInOwnAtm() {
        givenCard(CardType.DEBIT, "500", null);

        OperationResult result = service.deposit(1L, CARD, PIN, new BigDecimal("25.50"));

        assertThat(result.balance()).isEqualByComparingTo("125.50");
        assertThat(savedTypes()).containsExactly(MovementType.DEPOSIT);
    }

    @Test
    void depositInForeignAtmIsRejected() {
        givenCard(CardType.DEBIT, "500", null);

        assertError(() -> service.deposit(2L, CARD, PIN, BigDecimal.TEN), ErrorCode.DEPOSIT_NOT_ALLOWED);
        assertThat(account.getBalance()).isEqualByComparingTo("100.00");
    }

    // ---------- Helpers ----------

    private void givenCard(CardType type, String withdrawalLimit, String creditLimit) {
        Card card = new Card(CARD, type, account, "hash", true, new BigDecimal(withdrawalLimit),
                creditLimit == null ? null : new BigDecimal(creditLimit));
        when(cardService.authenticateActive(CARD, PIN)).thenReturn(card);
    }

    private static Customer customer(Long id) {
        Customer customer = new Customer("Cliente " + id);
        ReflectionTestUtils.setField(customer, "id", id);
        return customer;
    }

    private List<MovementType> savedTypes() {
        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);
        verify(movementRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues().stream().map(Movement::getType).toList();
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}

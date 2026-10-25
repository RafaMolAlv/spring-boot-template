package es.nextdigital.demo.service;

import es.nextdigital.demo.domain.Account;
import es.nextdigital.demo.domain.Card;
import es.nextdigital.demo.domain.CardType;
import es.nextdigital.demo.domain.Customer;
import es.nextdigital.demo.exception.BusinessException;
import es.nextdigital.demo.exception.ErrorCode;
import es.nextdigital.demo.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CardServiceTest {

    private static final String NUMBER = "4000000000000003";

    // Encoder real con coste bajo para que el test sea rápido
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final CardRepository repository = mock(CardRepository.class);
    private final CardService service = new CardService(repository, encoder);

    private Card card;

    @BeforeEach
    void setUp() {
        Account account = new Account("ES4990000001000000000003", new BigDecimal("500.00"), new Customer("Test"));
        card = new Card(NUMBER, CardType.DEBIT, account, encoder.encode("0000"), false,
                new BigDecimal("500.00"), null);
        when(repository.findByNumber(NUMBER)).thenReturn(Optional.of(card));
    }

    @Test
    void activateStoresNewPinHashedAndActivatesCard() {
        service.activate(NUMBER, "0000", "4321");

        assertThat(card.isActive()).isTrue();
        assertThat(card.getPinHash()).isNotEqualTo("4321");
        assertThat(encoder.matches("4321", card.getPinHash())).isTrue();
    }

    @Test
    void activateRejectsWrongPin() {
        assertError(() -> service.activate(NUMBER, "9999", "4321"), ErrorCode.INVALID_PIN);
        assertThat(card.isActive()).isFalse();
    }

    @Test
    void activateRequiresPinChange() {
        assertError(() -> service.activate(NUMBER, "0000", "0000"), ErrorCode.SAME_PIN);
    }

    @Test
    void activateTwiceFails() {
        service.activate(NUMBER, "0000", "4321");
        assertError(() -> service.activate(NUMBER, "4321", "1111"), ErrorCode.CARD_ALREADY_ACTIVATED);
    }

    @Test
    void operationsRequireActiveCard() {
        assertError(() -> service.authenticateActive(NUMBER, "0000"), ErrorCode.CARD_NOT_ACTIVATED);
        assertError(() -> service.changePin(NUMBER, "0000", "4321"), ErrorCode.CARD_NOT_ACTIVATED);
    }

    @Test
    void changePinAfterActivation() {
        service.activate(NUMBER, "0000", "4321");
        service.changePin(NUMBER, "4321", "8765");

        assertThat(encoder.matches("8765", card.getPinHash())).isTrue();
    }

    @Test
    void unknownCard() {
        when(repository.findByNumber("1")).thenReturn(Optional.empty());
        assertError(() -> service.authenticate("1", "0000"), ErrorCode.CARD_NOT_FOUND);
    }

    private void assertError(Runnable action, ErrorCode expected) {
        assertThatThrownBy(action::run)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }
}

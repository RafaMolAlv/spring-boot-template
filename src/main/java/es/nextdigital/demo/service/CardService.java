package es.nextdigital.demo.service;

import es.nextdigital.demo.domain.Card;
import es.nextdigital.demo.exception.BusinessException;
import es.nextdigital.demo.exception.ErrorCode;
import es.nextdigital.demo.repository.CardRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CardService {

    private final CardRepository cardRepository;
    private final PasswordEncoder pinEncoder;

    public CardService(CardRepository cardRepository, PasswordEncoder pinEncoder) {
        this.cardRepository = cardRepository;
        this.pinEncoder = pinEncoder;
    }

    /** Comprueba que la tarjeta existe y que el PIN es correcto. */
    public Card authenticate(String cardNumber, String pin) {
        Card card = cardRepository.findByNumber(cardNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));
        if (!pinEncoder.matches(pin, card.getPinHash())) {
            throw new BusinessException(ErrorCode.INVALID_PIN);
        }
        return card;
    }

    /** Igual que {@link #authenticate} pero exige además que la tarjeta esté activada. */
    public Card authenticateActive(String cardNumber, String pin) {
        Card card = authenticate(cardNumber, pin);
        if (!card.isActive()) {
            throw new BusinessException(ErrorCode.CARD_NOT_ACTIVATED);
        }
        return card;
    }

    /** Primera activación: obliga a cambiar el PIN inicial en la misma operación. */
    public void activate(String cardNumber, String currentPin, String newPin) {
        Card card = authenticate(cardNumber, currentPin);
        if (card.isActive()) {
            throw new BusinessException(ErrorCode.CARD_ALREADY_ACTIVATED);
        }
        requireDifferentPin(currentPin, newPin);
        card.activate(pinEncoder.encode(newPin));
    }

    public void changePin(String cardNumber, String currentPin, String newPin) {
        Card card = authenticateActive(cardNumber, currentPin);
        requireDifferentPin(currentPin, newPin);
        card.changePin(pinEncoder.encode(newPin));
    }

    private void requireDifferentPin(String currentPin, String newPin) {
        if (currentPin.equals(newPin)) {
            throw new BusinessException(ErrorCode.SAME_PIN);
        }
    }
}

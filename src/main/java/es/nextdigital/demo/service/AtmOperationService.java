package es.nextdigital.demo.service;

import es.nextdigital.demo.config.BankProperties;
import es.nextdigital.demo.domain.Account;
import es.nextdigital.demo.domain.Atm;
import es.nextdigital.demo.domain.Card;
import es.nextdigital.demo.domain.Movement;
import es.nextdigital.demo.domain.MovementType;
import es.nextdigital.demo.exception.BusinessException;
import es.nextdigital.demo.exception.ErrorCode;
import es.nextdigital.demo.repository.AccountRepository;
import es.nextdigital.demo.repository.AtmRepository;
import es.nextdigital.demo.repository.MovementRepository;
import es.nextdigital.demo.util.IbanValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class AtmOperationService {

    private final CardService cardService;
    private final AtmRepository atmRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final BankProperties bankProperties;

    public AtmOperationService(CardService cardService, AtmRepository atmRepository,
                               AccountRepository accountRepository, MovementRepository movementRepository,
                               BankProperties bankProperties) {
        this.cardService = cardService;
        this.atmRepository = atmRepository;
        this.accountRepository = accountRepository;
        this.movementRepository = movementRepository;
        this.bankProperties = bankProperties;
    }

    public OperationResult withdraw(Long atmId, String cardNumber, String pin, BigDecimal amount) {
        Atm atm = findAtm(atmId);
        Card card = cardService.authenticateActive(cardNumber, pin);

        if (amount.compareTo(card.getWithdrawalLimit()) > 0) {
            throw new BusinessException(ErrorCode.WITHDRAWAL_LIMIT_EXCEEDED);
        }
        BigDecimal fee = isOwnBank(atm) ? BigDecimal.ZERO : atm.getWithdrawalFee();
        if (amount.add(fee).compareTo(card.availableFunds()) > 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }

        Account account = card.getAccount();
        account.debit(amount);
        record(account, MovementType.WITHDRAWAL, amount, "Retirada en cajero " + atm.getId());
        chargeFee(account, fee, "Comisión retirada en cajero de la entidad " + atm.getBankCode());
        return new OperationResult(account.getIban(), amount, fee, account.getBalance());
    }

    public OperationResult deposit(Long atmId, String cardNumber, String pin, BigDecimal amount) {
        Atm atm = findAtm(atmId);
        Card card = cardService.authenticateActive(cardNumber, pin);

        if (!isOwnBank(atm)) {
            throw new BusinessException(ErrorCode.DEPOSIT_NOT_ALLOWED);
        }

        Account account = card.getAccount();
        account.credit(amount);
        record(account, MovementType.DEPOSIT, amount, "Ingreso en cajero " + atm.getId());
        return new OperationResult(account.getIban(), amount, BigDecimal.ZERO, account.getBalance());
    }

    public OperationResult transfer(Long atmId, String cardNumber, String pin,
                                    String destinationIban, BigDecimal amount, String concept) {
        findAtm(atmId);
        Card card = cardService.authenticateActive(cardNumber, pin);

        String destination = IbanValidator.normalize(destinationIban);
        if (!IbanValidator.isValid(destination)) {
            throw new BusinessException(ErrorCode.INVALID_IBAN);
        }
        Account origin = card.getAccount();
        if (destination.equals(origin.getIban())) {
            throw new BusinessException(ErrorCode.SAME_ACCOUNT_TRANSFER);
        }

        boolean internal = bankProperties.code().equals(IbanValidator.spanishBankCode(destination));
        BigDecimal fee = internal ? BigDecimal.ZERO : bankProperties.externalTransferFee();
        if (amount.add(fee).compareTo(card.availableFunds()) > 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_FUNDS);
        }
        // Si la cuenta destino es nuestra, debe existir antes de mover dinero
        Account target = internal
                ? accountRepository.findByIban(destination)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND))
                : null;

        String suffix = concept == null || concept.isBlank() ? "" : " - " + concept;
        origin.debit(amount);
        record(origin, MovementType.TRANSFER_OUT, amount, "Transferencia a " + destination + suffix);
        chargeFee(origin, fee, "Comisión transferencia a otra entidad");

        if (target != null) {
            target.credit(amount);
            record(target, MovementType.TRANSFER_IN, amount, "Transferencia de " + origin.getIban() + suffix);
        }
        return new OperationResult(origin.getIban(), amount, fee, origin.getBalance());
    }

    private Atm findAtm(Long atmId) {
        return atmRepository.findById(atmId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ATM_NOT_FOUND));
    }

    private boolean isOwnBank(Atm atm) {
        return bankProperties.code().equals(atm.getBankCode());
    }

    private void chargeFee(Account account, BigDecimal fee, String description) {
        if (fee.signum() > 0) {
            account.debit(fee);
            record(account, MovementType.FEE, fee, description);
        }
    }

    private void record(Account account, MovementType type, BigDecimal amount, String description) {
        movementRepository.save(new Movement(account, type, amount, description));
    }
}

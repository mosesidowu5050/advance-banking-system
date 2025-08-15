package com.apostle.services;

import com.apostle.data.model.AccountType;
import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import com.apostle.data.repositories.BankAccountRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.AddAccountRequest;
import com.apostle.dtos.responses.AddAccountResponse;
import com.apostle.dtos.responses.BalanceResponse;
import com.apostle.exceptions.InsufficientBalanceException;
import com.apostle.exceptions.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BankAccountServiceImpl implements BankAccountService {

    private final BankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;
    private final Validator validator;
    public static final String  SYSTEM_ACCOUNT_ID = "SYSTEM";

    public BankAccountServiceImpl(BankAccountRepository bankAccountRepository, UserRepository userRepository, Validator validator) {
        this.bankAccountRepository = bankAccountRepository;
        this.userRepository = userRepository;
        this.validator = validator;
    }

    @Override
    public BankAccount getSystemAccount() {
        return bankAccountRepository.findByAccountNumber("SYSTEM")
                .orElseGet(() -> {
                    BankAccount systemAccount = new BankAccount();
                    systemAccount.setAccountNumber("SYSTEM");
                    systemAccount.setName("Platform System Account");
                    systemAccount.setBalance(BigDecimal.ZERO);
                    systemAccount.setAccountType(AccountType.SYSTEM);
                    return bankAccountRepository.save(systemAccount);
                });
    }

    @Override
    public BankAccount createAccountForUser(User user, AccountType accountType) {
        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = BankAccount.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.ZERO)
                .accountType(accountType)
                .name(user.getUsername())
                .user(user)
                .build();

        return bankAccountRepository.save(account);
    }

    @Override
    public AddAccountResponse addSubAccountForCurrentUser(AddAccountRequest addAccountRequest) {
        Set<ConstraintViolation<AddAccountRequest>> violations = validator.validate(addAccountRequest);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findUserByEmail(currentUserEmail)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        String accountNumber = generateUniqueAccountNumber();

        BankAccount account = new BankAccount();
        account.setName(addAccountRequest.getName());
        account.setUser(user);
        account.setName(user.getUsername());
        account.setBalance(BigDecimal.ZERO);
        account.setAccountNumber(accountNumber);

        bankAccountRepository.save(account);

        return new AddAccountResponse(
                account.getAccountNumber(),
                account.getName(),
                account.getBalance()
        );
    }


    @Override
    public BalanceResponse getBalance(String  accountNumber) {
        BankAccount account = getAccountByAccountNumber(accountNumber);
        return new BalanceResponse( account.getBalance());
    }

    @Override
    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void credit(String  accountNumber, BigDecimal amount) {
        BankAccount account = getAccountByAccountNumber(accountNumber);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InsufficientBalanceException("amount must be greater than 0");
        }
        account.setBalance(account.getBalance().add(amount));
        bankAccountRepository.save(account);
    }

    @Override
    @Transactional
    @Retryable(
            value = {ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void debit(String  accountNumber, BigDecimal amount) {
        BankAccount account = getAccountByAccountNumber(accountNumber);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient Balance");
        }
        account.setBalance(account.getBalance().subtract(amount));
        bankAccountRepository.save(account);
    }

    @Override
    public BankAccount getAccountByAccountNumber(String accountNumber) {
        return bankAccountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new UserNotFoundException("Account not found"));
    }

    private String generateUniqueAccountNumber() {
        String accountNumber;
        do {
            accountNumber = String.valueOf(1000000000L + ThreadLocalRandom.current().nextLong(9000000000L));
        } while (bankAccountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

}

package com.apostle.services.transactionService;

import com.apostle.data.model.BankAccount;
import com.apostle.data.model.Transaction;
import com.apostle.data.model.TransactionStatus;
import com.apostle.data.model.TransactionType;
import com.apostle.data.repositories.TransactionRepository;
import com.apostle.dtos.requests.DepositRequest;
import com.apostle.dtos.requests.SendMoneyRequest;
import com.apostle.dtos.responses.TransactionResponse;
import com.apostle.exceptions.TransactionNotFoundException;
import com.apostle.services.bankService.BankAccountService;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import  org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final TransactionRepository transactionRepo;
    private final BankAccountService bankService;
    private static final String SYSTEM_ACCOUNT_ID = "SYSTEM";


    @Override
    @Retryable(
            value = {TransientDataAccessException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TransactionResponse deposit(DepositRequest request) {
        bankService.credit(request.receiverAccountNumber(), request.amount());

        BankAccount systemAccount = bankService.getSystemAccount();
        BankAccount receiverAccount = bankService.getAccountByAccountNumber(request.receiverAccountNumber());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountId(systemAccount.getId());
        transaction.setSenderAccountNumber(systemAccount.getAccountNumber());
        transaction.setReceiverAccountId(receiverAccount.getId());
        transaction.setReceiverAccountNumber(receiverAccount.getAccountNumber());
        transaction.setAmount(request.amount());
        transaction.setType(TransactionType.CREDIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setNote(request.note());
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setTransactionReference(generateTransactionReference());

        transactionRepo.save(transaction);

        return mapToTransactionResponse(transaction);
    }


    @Override
    @Transactional
    @Retryable(
            value = {TransientDataAccessException.class,
            ObjectOptimisticLockingFailureException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public TransactionResponse transfer(SendMoneyRequest request) {
        validateTransferRequest(request);

        BankAccount senderAccount = bankService.getAccountByAccountNumber(request.senderAccountNumber());
        BankAccount receiverAccount = bankService.getAccountByAccountNumber(request.receiverAccountNumber());

        bankService.debit(senderAccount.getAccountNumber(), request.amount());
        bankService.credit(receiverAccount.getAccountNumber(), request.amount());

        LocalDateTime now = LocalDateTime.now();
        String note = Optional.ofNullable(request.note()).orElse("");

        Transaction senderTx = createTransaction(
                senderAccount,
                receiverAccount,
                request.amount(),
                TransactionType.DEBIT,
                "Transfer to " + receiverAccount.getName() + ": " + note,
                now,
                generateTransactionReference()
        );

        Transaction receiverTx = createTransaction(
                receiverAccount,
                senderAccount,
                request.amount(),
                TransactionType.CREDIT,
                "Received from " + senderAccount.getName() + ": " + note,
                now,
                generateTransactionReference()
        );


        transactionRepo.save(senderTx);
        transactionRepo.save(receiverTx);

        return mapToTransactionResponse(senderTx);
    }


    @Override
    public List<TransactionResponse> getTransactionsForAccount(String accountId, LocalDateTime start, LocalDateTime end, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        List<Transaction> transactions = transactionRepo.findTransactionsWithinDateBySenderOrReceiverWithType(
                accountId, TransactionType.DEBIT, accountId, TransactionType.CREDIT, start, end, pageable);
        return transactions.stream()
                .map(t -> new TransactionResponse(t.getTransactionReference(), t.getAmount(), t.getType(),
                        t.getStatus(), t.getNote(), t.getTimestamp(),
                        bankService.getAccountByAccountNumber(t.getReceiverAccountNumber()).getName() ) )
                .sorted(Comparator.comparing(TransactionResponse::timeStamp).reversed())
                .toList();
    }


    @Override
    public TransactionResponse getTransactionById(String reference) {
        Transaction transaction = transactionRepo.findByTransactionReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found"));

        return mapToTransactionResponse(transaction);
    }


    private TransactionResponse mapToTransactionResponse(Transaction transaction) {
        String receiverName = bankService.getAccountByAccountNumber(transaction.getReceiverAccountNumber()).getName();
        return new TransactionResponse(
                transaction.getTransactionReference(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getNote(),
                transaction.getTimestamp(),
                receiverName
        );
    }



    private void validateTransferRequest(SendMoneyRequest request) {
        if (request.senderAccountNumber().equals(request.receiverAccountNumber())) {
            throw new IllegalArgumentException("Cannot transfer to self");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private Transaction createTransaction(
            BankAccount source,
            BankAccount target,
            BigDecimal amount,
            TransactionType type,
            String note,
            LocalDateTime timestamp,
            String transactionReference
    ) {
        Transaction transaction = new Transaction();

        if (type == TransactionType.DEBIT) {
            transaction.setSenderAccountId(source.getId());
            transaction.setSenderAccountNumber(source.getAccountNumber());
            transaction.setReceiverAccountId(target.getId());
            transaction.setReceiverAccountNumber(target.getAccountNumber());
        } else {
            transaction.setSenderAccountId(target.getId());
            transaction.setSenderAccountNumber(target.getAccountNumber());
            transaction.setReceiverAccountId(source.getId());
            transaction.setReceiverAccountNumber(source.getAccountNumber());
        }

        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setNote(note);
        transaction.setTimestamp(timestamp);
        transaction.setTransactionReference(transactionReference);

        return transaction;
    }

    private String generateTransactionReference() {
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuidPart = UUID.randomUUID().toString().replaceAll("-", "").substring(0, 12);
        return datePart + uuidPart;
    }
}



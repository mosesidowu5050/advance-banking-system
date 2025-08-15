package com.apostle.services;

import com.apostle.dtos.requests.DepositRequest;
import com.apostle.dtos.requests.SendMoneyRequest;
import com.apostle.dtos.responses.TransactionResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public interface TransactionService {
    TransactionResponse deposit(DepositRequest request);
    TransactionResponse transfer(SendMoneyRequest request);
    List<TransactionResponse> getTransactionsForAccount(String accountId, LocalDateTime start, LocalDateTime end, int page, int size);
    TransactionResponse getTransactionById(String  transactionId);
}

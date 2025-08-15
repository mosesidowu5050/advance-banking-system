package com.apostle.services;

import com.apostle.data.model.AccountType;
import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import com.apostle.dtos.requests.AddAccountRequest;
import com.apostle.dtos.responses.AddAccountResponse;
import com.apostle.dtos.responses.BalanceResponse;

import java.math.BigDecimal;

public interface BankAccountService {
    BankAccount getSystemAccount();

    BankAccount createAccountForUser(User user, AccountType accountType);

    AddAccountResponse addSubAccountForCurrentUser(AddAccountRequest addAccountRequest);

    BalanceResponse getBalance(String accountNumber);

    void credit(String accountNumber, BigDecimal amount);

    void debit(String accountNumber, BigDecimal amount);

//    BankAccount getAccountById(String accountId);

    BankAccount getAccountByAccountNumber(String accountNumber);
}


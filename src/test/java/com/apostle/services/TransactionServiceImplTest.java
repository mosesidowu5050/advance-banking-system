package com.apostle.services;

import com.apostle.data.model.BankAccount;
import com.apostle.data.model.TransactionStatus;
import com.apostle.data.model.TransactionType;
import com.apostle.data.model.User;
import com.apostle.data.repositories.BankAccountRepository;
import com.apostle.data.repositories.TransactionRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.DepositRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.requests.SendMoneyRequest;
import com.apostle.dtos.responses.RegisterResponses;
import com.apostle.dtos.responses.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class TransactionServiceImplTest {

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    @Autowired
    private BankAccountService bankAccountService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepo;

    @BeforeEach
    public void beforeEach() {
        transactionRepo.deleteAll();
        userRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    private RegisterRequest createRegisterRequest(String email, String username, String password) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    @Test
    public void testDeposit_worksCorrectly() {
        RegisterRequest registerRequest = createRegisterRequest(
                "deposit.user@example.com", "depositUser", "Password@2024"
        );
        RegisterResponses response = authenticationService.register(registerRequest);
        assertTrue(response.isSuccess());

        User user = userRepository.findUserByEmail(registerRequest.getEmail()).orElseThrow();
        BankAccount account = bankAccountRepository.findByUserId(user.getId()).orElseThrow();
        String accountNumber = account.getAccountNumber();

        BigDecimal depositAmount = new BigDecimal("1000.00");
        DepositRequest depositRequest = new DepositRequest(accountNumber, depositAmount, "Test deposit");
        TransactionResponse transactionResponse = transactionService.deposit(depositRequest);

        BankAccount updatedAccount = bankAccountService.getAccountByAccountNumber(account.getAccountNumber());

        assertEquals("deposituser", transactionResponse.receiverName());
        assertEquals(depositAmount, updatedAccount.getBalance());
        assertEquals(TransactionStatus.SUCCESS, transactionResponse.status());
        assertEquals(TransactionType.CREDIT, transactionResponse.type());
        assertEquals("Test deposit", transactionResponse.note());
        assertNotNull(transactionResponse.transactionReference());
    }

    @Test
    public void testDeposit_withInvalidAccountNumber_shouldThrowException() {
        DepositRequest invalidRequest = new DepositRequest("nonexistent-account", BigDecimal.valueOf(500), "Invalid account test");

        Exception exception = assertThrows(RuntimeException.class, () -> transactionService.deposit(invalidRequest));

        assertTrue(exception.getMessage().contains("Account not found"));
    }

    @Test
    public void testDeposit_withZeroAmount_shouldThrowException() {
        RegisterRequest registerRequest = createRegisterRequest("zero.amount@example.com", "zeroUser", "User@2024");
        authenticationService.register(registerRequest);
        BankAccount account = bankAccountRepository.findByUserId(userRepository.findUserByEmail(registerRequest.getEmail()).get().getId()).get();

        DepositRequest request = new DepositRequest(account.getAccountNumber(), BigDecimal.ZERO, "Zero amount");

        Exception exception = assertThrows(Exception.class, () -> transactionService.deposit(request));

        assertTrue(exception.getMessage().toLowerCase().contains("greater than 0"));
    }

    @Test
    public void testDeposit_withNegativeAmount_shouldThrowException() {
        RegisterRequest registerRequest = createRegisterRequest("negative.amount@example.com", "negativeUser", "Password@2024");
        authenticationService.register(registerRequest);
        BankAccount account = bankAccountRepository.findByUserId(userRepository.findUserByEmail(registerRequest.getEmail()).get().getId()).get();

        DepositRequest request = new DepositRequest(account.getAccountNumber(), new BigDecimal("-500.00"), "Negative deposit");

        Exception exception = assertThrows(Exception.class, () -> transactionService.deposit(request));

        assertTrue(exception.getMessage().toLowerCase().contains("greater than 0"));
    }

    @Test
    public void testTransfer_successful() {
        RegisterRequest senderRequest = createRegisterRequest("sender@example.com", "senderUser", "Password@123");
        authenticationService.register(senderRequest);
        BankAccount senderAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(senderRequest.getEmail()).get().getId()).get();

        bankAccountService.credit(senderAccount.getAccountNumber(), new BigDecimal("2000"));

        RegisterRequest receiverRequest = createRegisterRequest("receiver@example.com", "receiverUser", "Password@123");
        authenticationService.register(receiverRequest);
        BankAccount receiverAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(receiverRequest.getEmail()).get().getId()).get();

        SendMoneyRequest transferRequest = new SendMoneyRequest(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                new BigDecimal("500.00"),
                "Test transfer"
        );

        TransactionResponse response = transactionService.transfer(transferRequest);

        BankAccount updatedSender = bankAccountService.getAccountByAccountNumber(senderAccount.getAccountNumber());
        BankAccount updatedReceiver = bankAccountService.getAccountByAccountNumber(receiverAccount.getAccountNumber());

        assertEquals(new BigDecimal("1500.00"), updatedSender.getBalance());
        assertEquals(new BigDecimal("500.00"), updatedReceiver.getBalance());

        assertEquals(TransactionType.DEBIT, response.type());
        assertEquals(TransactionStatus.SUCCESS, response.status());
        assertEquals("Transfer to " + "receiveruser" + ": Test transfer", response.note());
        assertEquals("receiveruser", response.receiverName());
    }

    @Test
    public void testTransfer_sameSenderAndReceiver_shouldThrowException() {
        RegisterRequest request = createRegisterRequest("same@example.com", "sameUser", "Password@123");
        authenticationService.register(request);
        BankAccount account = bankAccountRepository.findByUserId(
                userRepository.findUserByEmail(request.getEmail()).get().getId()
        ).get();

        SendMoneyRequest transferRequest = new SendMoneyRequest(
                account.getAccountNumber(),
                account.getAccountNumber(),
                new BigDecimal("100.00"),
                "Invalid"
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transfer(transferRequest);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("cannot transfer to self"));
    }

    @Test
    public void testTransfer_zeroAmount_shouldThrowException() {
        RegisterRequest senderRequest = createRegisterRequest("zeroamount@example.com", "zeroUser", "Password@123");
        authenticationService.register(senderRequest);
        BankAccount senderAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(senderRequest.getEmail()).get().getId()).get();

        RegisterRequest receiverRequest = createRegisterRequest("receiverzero@example.com", "recvUser", "Password@123");
        authenticationService.register(receiverRequest);
        BankAccount receiverAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(receiverRequest.getEmail()).get().getId()).get();

        SendMoneyRequest request = new SendMoneyRequest(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                BigDecimal.ZERO,
                "Zero test"
        );

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            transactionService.transfer(request);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("greater than zero"));
    }

    @Test
    public void testTransfer_insufficientBalance_shouldThrowException() {
        RegisterRequest senderRequest = createRegisterRequest("nobalance@example.com", "noBalUser", "Password@123");
        authenticationService.register(senderRequest);
        BankAccount senderAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(senderRequest.getEmail()).get().getId()).get();

        RegisterRequest receiverRequest = createRegisterRequest("recv.nobalance@example.com", "recvNoBal", "Password@123");
        authenticationService.register(receiverRequest);
        BankAccount receiverAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(receiverRequest.getEmail()).get().getId()).get();

        SendMoneyRequest transferRequest = new SendMoneyRequest(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                new BigDecimal("5000.00"),
                "Too much"
        );

        Exception exception = assertThrows(RuntimeException.class, () -> {
            transactionService.transfer(transferRequest);
        });

        assertTrue(exception.getMessage().toLowerCase().contains("insufficient balance"));
    }
    @Test
    public void testGetTransactionsForAccount_returnsAllRelatedTransactions() {
        RegisterRequest senderRegisterRequest = createRegisterRequest("senderi@gmail.com", "Nicholas_Agbo", "Password@123");
        authenticationService.register(senderRegisterRequest);

        RegisterRequest receiverRegisterRequest = createRegisterRequest("receiver@gmail.com", "John_Adah", "Password@123");
        authenticationService.register(receiverRegisterRequest);
        BankAccount senderAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(senderRegisterRequest.getEmail()).get().getId()).get();
        BankAccount receiverAccount = bankAccountRepository.findByUserId(userRepository.findUserByEmail(receiverRegisterRequest.getEmail()).get().getId()).get();

        transactionService.deposit(new DepositRequest(senderAccount.getAccountNumber(), new BigDecimal("1000"), "Initial deposit"));

        transactionService.transfer(new SendMoneyRequest(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                new BigDecimal("600"),
                "Sending money"
        ));

        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now().plusDays(1);
        int page = 0;
        int size = 10;

        List<TransactionResponse> senderTransactions = transactionService.getTransactionsForAccount(senderAccount.getId(), start, end, page, size);
        List<TransactionResponse> receiverTransactions = transactionService.getTransactionsForAccount(receiverAccount.getId(), start, end, page, size);

        System.out.println("Sender Transactions: " + senderTransactions);
        System.out.println("Receiver Transactions: " + receiverTransactions);

        assertEquals(2, senderTransactions.size());
        assertEquals(1, receiverTransactions.size());

    }

}
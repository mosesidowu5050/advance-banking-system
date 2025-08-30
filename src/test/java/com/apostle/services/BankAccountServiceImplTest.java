package com.apostle.services;

import com.apostle.data.model.AccountType;
import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import com.apostle.data.repositories.BankAccountRepository;
import com.apostle.data.repositories.UserRepository;
import com.apostle.dtos.requests.AddAccountRequest;
import com.apostle.dtos.requests.LoginRequest;
import com.apostle.dtos.requests.RegisterRequest;
import com.apostle.dtos.responses.AddAccountResponse;
import com.apostle.dtos.responses.LoginResponse;
import com.apostle.dtos.responses.RegisterResponses;
import com.apostle.exceptions.InsufficientBalanceException;
import com.apostle.exceptions.UserNotFoundException;
import com.apostle.services.authService.AuthenticationServiceImpl;
import com.apostle.services.bankService.BankAccountServiceImpl;
import com.apostle.services.jwtService.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class BankAccountServiceImplTest {

    @Autowired
    private AuthenticationServiceImpl authenticationService;

    @Autowired
    private BankAccountServiceImpl bankAccountService;

    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @BeforeEach
    public void beforeEach() {
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


    private void authenticateUser(String email, String token) {
        User user = userRepository.findUserByEmail(email).orElseThrow();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
                );

        SecurityContextHolder.getContext().setAuthentication(authentication);
    }


    @Test
    public void testAccountCreation_works() {
        RegisterRequest request = createRegisterRequest("john.doe12@example.com", "johndoe", "Password@2024");
        RegisterResponses registerResponses = authenticationService.register(request);

        assertNotNull(registerResponses);
        assertTrue(registerResponses.isSuccess());
        assertNotNull(registerResponses.getAccountNumber());

        BankAccount createdAccount = bankAccountRepository.findByAccountNumber(registerResponses.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        assertEquals(10, createdAccount.getAccountNumber().length());
        assertEquals(BigDecimal.ZERO, createdAccount.getBalance());
        assertEquals(AccountType.SAVINGS, createdAccount.getAccountType());
        assertNotNull(createdAccount.getUser());
    }

    @Test
    public void testCreditAccount_works() {
        RegisterRequest request = createRegisterRequest("alice.doe33@example.com", "alicedoe", "Password@2024");
        RegisterResponses registerResponses = authenticationService.register(request);

        BankAccount account = bankAccountRepository.findByAccountNumber(registerResponses.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal creditAmount = new BigDecimal("1000.00");
        bankAccountService.credit(account.getAccountNumber(), creditAmount);

        BankAccount updatedAccount = bankAccountService.getAccountByAccountNumber(account.getAccountNumber());

        assertEquals(creditAmount, updatedAccount.getBalance());
    }


    @Test
    public void testCreditAccount_failsForNegativeAmount() {
        RegisterRequest request = createRegisterRequest("negative@example.com", "negative", "Password@2024");
        RegisterResponses response = authenticationService.register(request);

        BankAccount account = bankAccountRepository.findByAccountNumber(response.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal negativeAmount = new BigDecimal("-500.00");

        assertThrows(InsufficientBalanceException.class, () -> bankAccountService.credit(account.getAccountNumber(), negativeAmount));
    }

    @Test
    public void testDebitAccount_failsForInsufficientBalance() {
        RegisterRequest request = createRegisterRequest("lowfund@example.com", "lowfund", "Password@2024");
        RegisterResponses response = authenticationService.register(request);

        BankAccount account = bankAccountRepository.findByAccountNumber(response.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal debitAmount = new BigDecimal("500.00");

        assertThrows(InsufficientBalanceException.class, () -> bankAccountService.debit(account.getAccountNumber(), debitAmount));
    }

    @Test
    public void testDebitAccount_worksAfterCredit() {
        RegisterRequest request = createRegisterRequest("creditdebit@example.com", "cduser", "Cuser@2024");
        RegisterResponses response = authenticationService.register(request);

        BankAccount account = bankAccountRepository.findByAccountNumber(response.getAccountNumber())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        BigDecimal credit = new BigDecimal("1000.00");
        BigDecimal debit = new BigDecimal("400.00");

        bankAccountService.credit(account.getAccountNumber(), credit);
        bankAccountService.debit(account.getAccountNumber(), debit);

        BankAccount updated = bankAccountService.getAccountByAccountNumber(account.getAccountNumber());
        assertEquals(new BigDecimal("600.00"), updated.getBalance());
    }

    @Test
    public void testGetBalance_failsForInvalidAccountId() {
        String invalidAccountId = "non_existing_id";

        assertThrows(UserNotFoundException.class, () -> bankAccountService.getBalance(invalidAccountId));
    }

    @Test
    public void testAddSubAccountForCurrentUser_works() {
        RegisterRequest request = createRegisterRequest("jane.subacc@example.com", "janeSub", "Password@2025");
        authenticationService.register(request);

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());

        LoginResponse loginResponse = authenticationService.login(loginRequest);
        String token = loginResponse.getToken();

        authenticateUser(request.getEmail(), token);

        AddAccountRequest subAccRequest = new AddAccountRequest();
        subAccRequest.setName("janesub");

        AddAccountResponse response = bankAccountService.addSubAccountForCurrentUser(subAccRequest);

        assertNotNull(response);
        assertNotNull(response.getAccountNumber());
        assertEquals("My Vout Wallet", response.getName());
        assertEquals(BigDecimal.ZERO, response.getBalance());
    }

    @Test
    public void testAddSubAccount_shouldFailWhenUserDoesNotExistInDatabase() {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("ghost@example.com", null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        AddAccountRequest request = new AddAccountRequest();
        request.setName("Ghost Wallet");

        Exception exception = assertThrows(UserNotFoundException.class, () ->
                bankAccountService.addSubAccountForCurrentUser(request)
        );

        assertEquals("User not found", exception.getMessage());
    }



}

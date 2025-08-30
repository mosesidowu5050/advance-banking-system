package com.apostle.controllers;

import com.apostle.dtos.requests.AddAccountRequest;
import com.apostle.dtos.responses.ApiResponse;
import com.apostle.dtos.responses.BalanceResponse;
import com.apostle.exceptions.UserNotFoundException;
import com.apostle.services.bankService.BankAccountService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@PreAuthorize("hasRole('CUSTOMER')")
public class BankAccountController {

    private final BankAccountService bankAccountService;
    private static final Logger logger = LoggerFactory.getLogger(BankAccountController.class);

    public BankAccountController(BankAccountService bankAccountService) {
        this.bankAccountService = bankAccountService;
    }

    @PostMapping("/bank-account")
    public ResponseEntity<ApiResponse> createBankAccount(@Valid @RequestBody AddAccountRequest addAccountRequest) {
        try {
            logger.info("Creating bank account for request: {}", addAccountRequest);
            var response = bankAccountService.addSubAccountForCurrentUser(addAccountRequest);
            return ResponseEntity.ok(new ApiResponse(true, "Bank account created successfully", response));
        } catch (ConstraintViolationException e) {
            logger.error("Validation error creating bank account: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (UserNotFoundException e) {
            logger.error("User not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error creating bank account: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }

    @GetMapping("/balance/{accountNumber}")
    public ResponseEntity<ApiResponse> getBalance(@PathVariable String accountNumber) {
        try {
            logger.info("Fetching balance for account: {}", accountNumber);
            BalanceResponse response = bankAccountService.getBalance(accountNumber);
            return ResponseEntity.ok(new ApiResponse(true, "Balance retrieved successfully", response));
        } catch (UserNotFoundException e) {
            logger.error("Account not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching balance: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }
}
package com.apostle.controllers;

import com.apostle.dtos.requests.DepositRequest;
import com.apostle.dtos.requests.SendMoneyRequest;
import com.apostle.dtos.responses.ApiResponse;
import com.apostle.dtos.responses.TransactionResponse;
import com.apostle.exceptions.InsufficientBalanceException;
import com.apostle.exceptions.TransactionNotFoundException;
import com.apostle.exceptions.UserNotFoundException;
import com.apostle.services.transactionService.TransactionService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@PreAuthorize("hasRole('CUSTOMER')")
public class TransactionController {

    private final TransactionService transactionService;
    private static final Logger logger = LoggerFactory.getLogger(TransactionController.class);

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<ApiResponse> deposit(@Valid @RequestBody DepositRequest depositRequest) {
        try {
            logger.info("Processing deposit for account: {}", depositRequest.receiverAccountNumber());
            TransactionResponse response = transactionService.deposit(depositRequest);
            return ResponseEntity.ok(new ApiResponse(true, "Deposit successful", response));
        } catch (ConstraintViolationException e) {
            logger.error("Validation error during deposit: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (UserNotFoundException e) {
            logger.error("No account found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error processing deposit: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse> transfer(@Valid @RequestBody SendMoneyRequest sendMoneyRequest) {
        try {
            logger.info("Processing transfer from {} to {}", sendMoneyRequest.senderAccountNumber(), sendMoneyRequest.receiverAccountNumber());
            TransactionResponse response = transactionService.transfer(sendMoneyRequest);
            return ResponseEntity.ok(new ApiResponse(true, "Transfer successful", response));
        } catch (ConstraintViolationException e) {
            logger.error("Validation error during transfer: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (UserNotFoundException e) {
            logger.error("Account not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage(), null));
        } catch (InsufficientBalanceException e) {
            logger.error("Insufficient balance: {}", e.getMessage());
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error processing transfer: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }

    @GetMapping("/account/{accountId}")
    public ResponseEntity<ApiResponse> getTransactionsForAccount(
            @PathVariable String accountId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            logger.info("Fetching transactions for account: {}, from {} to {}", accountId, start, end);
            List<TransactionResponse> response = transactionService.getTransactionsForAccount(accountId, start, end, page, size);
            return ResponseEntity.ok(new ApiResponse(true, "Transactions retrieved successfully", response));
        } catch (Exception e) {
            logger.error("Error fetching transactions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse> getTransactionById(@PathVariable String transactionId) {
        try {
            logger.info("Fetching transaction by ID: {}", transactionId);
            TransactionResponse response = transactionService.getTransactionById(transactionId);
            return ResponseEntity.ok(new ApiResponse(true, "Transaction retrieved successfully", response));
        } catch (TransactionNotFoundException e) {
            logger.error("Transaction not found: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponse(false, e.getMessage(), null));
        } catch (Exception e) {
            logger.error("Error fetching transaction: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponse(false, "An unexpected error occurred", null));
        }
    }
}
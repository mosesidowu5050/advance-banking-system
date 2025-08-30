package com.apostle.dtos.responses;

import com.apostle.data.model.TransactionStatus;
import com.apostle.data.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse (

        String transactionReference,
        BigDecimal amount,
        TransactionType type,
        TransactionStatus status,
        String note,
        LocalDateTime timeStamp,
        String receiverName
) { }

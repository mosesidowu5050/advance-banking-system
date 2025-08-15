package com.apostle.dtos.requests;

import java.math.BigDecimal;

public record DepositRequest(
        String receiverAccountNumber,
        BigDecimal amount,
        String note
) { }

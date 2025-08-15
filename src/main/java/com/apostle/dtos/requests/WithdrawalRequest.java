package com.apostle.dtos.requests;

import java.math.BigDecimal;

public record WithdrawalRequest(
        Long senderId,
        BigDecimal amount,
        String note
) { }

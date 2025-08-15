package com.apostle.dtos.responses;

import java.math.BigDecimal;

public record BalanceResponse(
//        String accountId,
        BigDecimal balance
) { }

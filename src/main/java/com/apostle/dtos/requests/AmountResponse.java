package com.apostle.dtos.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record AmountResponse(
        @NotNull String accountId,
        @Positive @Digits(integer = 8, fraction = 2) BigDecimal amount
) { }

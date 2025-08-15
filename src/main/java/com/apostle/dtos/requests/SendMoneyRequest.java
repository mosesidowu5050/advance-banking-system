package com.apostle.dtos.requests;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SendMoneyRequest (
        @NotNull String senderAccountNumber,
        @NotNull String receiverAccountNumber,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @Size(max = 255) String note
) {}


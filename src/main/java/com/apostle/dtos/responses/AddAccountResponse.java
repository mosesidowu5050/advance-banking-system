package com.apostle.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AddAccountResponse {
    private String accountNumber;
    private String name;
    private BigDecimal balance;
}

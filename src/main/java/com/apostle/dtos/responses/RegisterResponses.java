package com.apostle.dtos.responses;

import lombok.Data;

@Data
public class RegisterResponses {
    private String message;
    private boolean success;
    private String accountNumber;
}

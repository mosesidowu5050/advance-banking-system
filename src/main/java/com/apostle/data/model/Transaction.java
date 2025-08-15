package com.apostle.data.model;


import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    private String transactionId;
    private String transactionReference;
    private String senderAccountId;
    private String receiverAccountId;

    private String senderAccountNumber;
    private String receiverAccountNumber;

    private BigDecimal amount;

    private TransactionStatus status;
    private TransactionType type;

    private String note;

    @CreatedDate
    private LocalDateTime timestamp;
}

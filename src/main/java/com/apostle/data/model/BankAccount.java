package com.apostle.data.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "bank_accounts")
public class BankAccount {

    @Id
    private String id;

    @Indexed(unique = true)
    private String accountNumber;

    private String name;
    private BigDecimal balance;


    @Version
    private Long version;

    private AccountType accountType;


    private User user;
}

package com.apostle.data.repositories;

import com.apostle.data.model.BankAccount;
import com.apostle.data.model.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BankAccountRepository extends MongoRepository<BankAccount, String> {

    boolean existsByAccountNumber(String accountNumber);

    Optional<BankAccount> findByAccountNumber(String accountNumber);

    Optional<BankAccount> findByUserId(String userId);

//    Optional<BankAccount> findByUser(User user);
}

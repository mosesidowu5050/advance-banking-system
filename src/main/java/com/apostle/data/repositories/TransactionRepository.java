package com.apostle.data.repositories;

import com.apostle.data.model.Transaction;
import com.apostle.data.model.TransactionType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {


    List<Transaction> findAllBySenderAccountIdAndTypeOrReceiverAccountIdAndType(
            String senderId, TransactionType senderType, String receiverId, TransactionType receiverType, Pageable pageable);


    @Query("{$and: [{ timestamp: { $gte: ?4, $lte: ?5 } }, { $or: [ { senderAccountId: ?0, type: ?1 }, { receiverAccountId: ?2, type: ?3 } ] } ] }")
    List<Transaction> findTransactionsWithinDateBySenderOrReceiverWithType(
            String senderId,
            TransactionType senderType,
            String receiverId,
            TransactionType receiverType,
            LocalDateTime start,
            LocalDateTime end,
            Pageable pageable
    );


    Optional<Transaction> findByTransactionReference(String transactionReference);
}

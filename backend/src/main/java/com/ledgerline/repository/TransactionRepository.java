package com.ledgerline.repository;

import com.ledgerline.model.Account;
import com.ledgerline.model.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTxUuid(String txUuid);
    Optional<TransactionRecord> findByIdempotencyKey(String idempotencyKey);
    List<TransactionRecord> findBySourceAccountOrDestinationAccountOrderByCreatedAtDesc(Account source, Account dest);
}


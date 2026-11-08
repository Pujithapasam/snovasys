package com.ledgerline.repository;

import com.ledgerline.model.Account;
import com.ledgerline.model.EntryType;
import com.ledgerline.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {
    List<LedgerEntry> findAllByOrderByEntryOrderAsc();
    List<LedgerEntry> findByAccountOrderByEntryOrderDesc(Account account);
    Optional<LedgerEntry> findTopByOrderByEntryOrderDesc();

    @Query("SELECT COALESCE(SUM(l.amount), 0) FROM LedgerEntry l WHERE l.entryType = :entryType")
    BigDecimal sumAmountByEntryType(@Param("entryType") EntryType entryType);

    @Query("SELECT COUNT(l) FROM LedgerEntry l")
    long countEntries();
}


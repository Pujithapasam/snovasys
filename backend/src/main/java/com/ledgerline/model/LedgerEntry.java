package com.ledgerline.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_order", nullable = false)
    private Long entryOrder;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", nullable = false)
    private TransactionRecord transactionRecord;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "prev_hash", nullable = false, length = 64)
    private String prevHash;

    @Column(name = "entry_hash", nullable = false, length = 64)
    private String entryHash;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public LedgerEntry() {}

    public LedgerEntry(Long entryOrder, TransactionRecord transactionRecord, Account account,
                       EntryType entryType, BigDecimal amount, String prevHash, String entryHash) {
        this.entryOrder = entryOrder;
        this.transactionRecord = transactionRecord;
        this.account = account;
        this.entryType = entryType;
        this.amount = amount;
        this.prevHash = prevHash;
        this.entryHash = entryHash;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEntryOrder() { return entryOrder; }
    public void setEntryOrder(Long entryOrder) { this.entryOrder = entryOrder; }

    public TransactionRecord getTransactionRecord() { return transactionRecord; }
    public void setTransactionRecord(TransactionRecord transactionRecord) { this.transactionRecord = transactionRecord; }

    public Account getAccount() { return account; }
    public void setAccount(Account account) { this.account = account; }

    public EntryType getEntryType() { return entryType; }
    public void setEntryType(EntryType entryType) { this.entryType = entryType; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPrevHash() { return prevHash; }
    public void setPrevHash(String prevHash) { this.prevHash = prevHash; }

    public String getEntryHash() { return entryHash; }
    public void setEntryHash(String entryHash) { this.entryHash = entryHash; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


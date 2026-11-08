package com.ledgerline.dto;

import com.ledgerline.model.EntryType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerEntryDto {
    private Long id;
    private Long entryOrder;
    private String txUuid;
    private String accountNumber;
    private String accountName;
    private EntryType entryType;
    private BigDecimal amount;
    private String prevHash;
    private String entryHash;
    private LocalDateTime createdAt;

    public LedgerEntryDto() {}
    public LedgerEntryDto(Long id, Long entryOrder, String txUuid, String accountNumber,
                          String accountName, EntryType entryType, BigDecimal amount,
                          String prevHash, String entryHash, LocalDateTime createdAt) {
        this.id = id;
        this.entryOrder = entryOrder;
        this.txUuid = txUuid;
        this.accountNumber = accountNumber;
        this.accountName = accountName;
        this.entryType = entryType;
        this.amount = amount;
        this.prevHash = prevHash;
        this.entryHash = entryHash;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEntryOrder() { return entryOrder; }
    public void setEntryOrder(Long entryOrder) { this.entryOrder = entryOrder; }

    public String getTxUuid() { return txUuid; }
    public void setTxUuid(String txUuid) { this.txUuid = txUuid; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getAccountName() { return accountName; }
    public void setAccountName(String accountName) { this.accountName = accountName; }

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


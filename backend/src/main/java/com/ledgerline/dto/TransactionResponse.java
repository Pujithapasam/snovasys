package com.ledgerline.dto;

import com.ledgerline.model.TransactionStatus;
import com.ledgerline.model.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionResponse {
    private String txUuid;
    private String idempotencyKey;
    private TransactionType type;
    private BigDecimal amount;
    private String currency;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private TransactionStatus status;
    private String message;
    private LocalDateTime createdAt;

    public TransactionResponse() {}
    public TransactionResponse(String txUuid, String idempotencyKey, TransactionType type,
                               BigDecimal amount, String currency, String sourceAccountNumber,
                               String destinationAccountNumber, TransactionStatus status,
                               String message, LocalDateTime createdAt) {
        this.txUuid = txUuid;
        this.idempotencyKey = idempotencyKey;
        this.type = type;
        this.amount = amount;
        this.currency = currency;
        this.sourceAccountNumber = sourceAccountNumber;
        this.destinationAccountNumber = destinationAccountNumber;
        this.status = status;
        this.message = message;
        this.createdAt = createdAt;
    }

    public String getTxUuid() { return txUuid; }
    public void setTxUuid(String txUuid) { this.txUuid = txUuid; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getSourceAccountNumber() { return sourceAccountNumber; }
    public void setSourceAccountNumber(String sourceAccountNumber) { this.sourceAccountNumber = sourceAccountNumber; }

    public String getDestinationAccountNumber() { return destinationAccountNumber; }
    public void setDestinationAccountNumber(String destinationAccountNumber) { this.destinationAccountNumber = destinationAccountNumber; }

    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


package com.ledgerline.dto;

import com.ledgerline.model.AccountType;
import java.math.BigDecimal;

public class AccountDto {
    private Long id;
    private String accountNumber;
    private String name;
    private String ownerUsername;
    private BigDecimal balance;
    private String currency;
    private AccountType accountType;
    private String status;

    public AccountDto() {}
    public AccountDto(Long id, String accountNumber, String name, String ownerUsername,
                      BigDecimal balance, String currency, AccountType accountType, String status) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.name = name;
        this.ownerUsername = ownerUsername;
        this.balance = balance;
        this.currency = currency;
        this.accountType = accountType;
        this.status = status;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOwnerUsername() { return ownerUsername; }
    public void setOwnerUsername(String ownerUsername) { this.ownerUsername = ownerUsername; }

    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public AccountType getAccountType() { return accountType; }
    public void setAccountType(AccountType accountType) { this.accountType = accountType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}


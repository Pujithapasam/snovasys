package com.ledgerline.service;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.exception.AccountNotFoundException;
import com.ledgerline.model.Account;
import com.ledgerline.model.AccountType;
import com.ledgerline.model.User;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          @Lazy TransactionService transactionService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAllAccounts() {
        return accountRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AccountDto> getAccountsByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return accountRepository.findByUser(user).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AccountDto getAccountByNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        return toDto(account);
    }

    @Transactional
    public AccountDto createAccount(String username, String name, BigDecimal initialDeposit, AccountType type) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String accountNumber = "ACC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Account account = new Account(accountNumber, user, name, BigDecimal.ZERO, "USD", type != null ? type : AccountType.CUSTOMER_CHECKING);
        account = accountRepository.save(account);

        if (initialDeposit != null && initialDeposit.compareTo(BigDecimal.ZERO) > 0) {
            DepositRequest depositReq = new DepositRequest(account.getAccountNumber(), initialDeposit, "Initial account funding");
            transactionService.deposit(depositReq, "INIT-" + accountNumber);
            // Refresh account
            account = accountRepository.findById(account.getId()).orElse(account);
        }

        return toDto(account);
    }

    @Transactional
    public AccountDto updateAccountStatus(String accountNumber, String status) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Account not found: " + accountNumber));
        account.setStatus(status.toUpperCase());
        account = accountRepository.save(account);
        return toDto(account);
    }

    private AccountDto toDto(Account account) {
        return new AccountDto(
                account.getId(),
                account.getAccountNumber(),
                account.getName(),
                account.getUser() != null ? account.getUser().getUsername() : "SYSTEM",
                account.getBalance(),
                account.getCurrency(),
                account.getAccountType(),
                account.getStatus()
        );
    }
}


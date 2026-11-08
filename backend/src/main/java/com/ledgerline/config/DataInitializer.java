package com.ledgerline.config;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.model.Account;
import com.ledgerline.model.AccountType;
import com.ledgerline.model.Role;
import com.ledgerline.model.User;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.TransactionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionService transactionService;

    public DataInitializer(UserRepository userRepository,
                           AccountRepository accountRepository,
                           PasswordEncoder passwordEncoder,
                           TransactionService transactionService) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionService = transactionService;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already seeded. Skipping initial data load.");
            return;
        }

        log.info("Initializing LedgerLine default users, system clearing vault, and accounts...");

        // 1. Create System & Administrative Users
        User adminUser = userRepository.save(new User("admin", passwordEncoder.encode("password123"), Role.ADMIN));
        User auditorUser = userRepository.save(new User("auditor", passwordEncoder.encode("password123"), Role.AUDITOR));
        User alice = userRepository.save(new User("alice", passwordEncoder.encode("password123"), Role.ACCOUNT_HOLDER));
        User bob = userRepository.save(new User("bob", passwordEncoder.encode("password123"), Role.ACCOUNT_HOLDER));
        User charlie = userRepository.save(new User("charlie", passwordEncoder.encode("password123"), Role.ACCOUNT_HOLDER));

        // 2. Create System Clearing Vault Account (Zero-sum anchor for deposits and withdrawals)
        Account vault = new Account("ACC-SYSTEM-VAULT", adminUser, "Federal Reserve Clearing Vault", BigDecimal.ZERO, "USD", AccountType.SYSTEM_CLEARING);
        accountRepository.save(vault);

        // 3. Create Customer Accounts
        Account aliceChecking = accountRepository.save(new Account("ACC-ALICE-001", alice, "Alice Main Checking", BigDecimal.ZERO, "USD", AccountType.CUSTOMER_CHECKING));
        Account aliceSavings = accountRepository.save(new Account("ACC-ALICE-002", alice, "Alice High-Yield Savings", BigDecimal.ZERO, "USD", AccountType.CUSTOMER_SAVINGS));
        Account bobChecking = accountRepository.save(new Account("ACC-BOB-001", bob, "Bob Everyday Checking", BigDecimal.ZERO, "USD", AccountType.CUSTOMER_CHECKING));
        Account charlieChecking = accountRepository.save(new Account("ACC-CHARLIE-001", charlie, "Charlie Checking", BigDecimal.ZERO, "USD", AccountType.CUSTOMER_CHECKING));

        // 4. Fund Accounts via Double-Entry Ledger (Maintains zero-sum integrity from Genesis)
        transactionService.deposit(new DepositRequest(aliceChecking.getAccountNumber(), new BigDecimal("1000.00"), "Initial Deposit"), "GENESIS-ALICE-001");
        transactionService.deposit(new DepositRequest(aliceSavings.getAccountNumber(), new BigDecimal("2500.00"), "Initial Deposit"), "GENESIS-ALICE-002");
        transactionService.deposit(new DepositRequest(bobChecking.getAccountNumber(), new BigDecimal("500.00"), "Initial Deposit"), "GENESIS-BOB-001");
        transactionService.deposit(new DepositRequest(charlieChecking.getAccountNumber(), new BigDecimal("250.00"), "Initial Deposit"), "GENESIS-CHARLIE-001");

        log.info("LedgerLine data initialization complete! Initial seed books balanced and cryptographically chained.");
    }
}


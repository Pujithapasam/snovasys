package com.ledgerline;

import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.model.Account;
import com.ledgerline.model.AccountType;
import com.ledgerline.model.Role;
import com.ledgerline.model.User;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ConcurrencyAndIdempotencyTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    private Account testAccount;

    @BeforeEach
    void setup() {
        User user = userRepository.findByUsername("concurrency_tester").orElseGet(() ->
                userRepository.save(new User("concurrency_tester", "pass", Role.ACCOUNT_HOLDER))
        );

        String accNumber = "TEST-" + UUID.randomUUID().toString().substring(0, 8);
        testAccount = new Account(accNumber, user, "Test Concurrency Account", BigDecimal.ZERO, "USD", AccountType.CUSTOMER_CHECKING);
        testAccount = accountRepository.save(testAccount);

        // Fund with exactly $100.00
        transactionService.deposit(new DepositRequest(testAccount.getAccountNumber(), new BigDecimal("100.00"), "Initial Fund"), null);
        testAccount = accountRepository.findById(testAccount.getId()).orElseThrow();
    }

    @Test
    @DisplayName("Idempotency Test: Sending identical idempotency key twice deducts funds only once")
    void testIdempotencyPreventsDoubleSpend() {
        String idempotencyKey = "IDEMP-KEY-" + UUID.randomUUID();
        WithdrawRequest request = new WithdrawRequest(testAccount.getAccountNumber(), new BigDecimal("40.00"), "Idempotency test");

        // First attempt
        TransactionResponse firstResponse = transactionService.withdraw(request, idempotencyKey);
        assertNotNull(firstResponse);
        assertEquals(new BigDecimal("40.00"), firstResponse.getAmount());

        Account afterFirst = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(new BigDecimal("60.0000"), afterFirst.getBalance().setScale(4));

        // Replay attempt with same key (simulating network retry)
        TransactionResponse secondResponse = transactionService.withdraw(request, idempotencyKey);
        assertNotNull(secondResponse);
        assertEquals(firstResponse.getTxUuid(), secondResponse.getTxUuid(), "Must return identical transaction UUID");

        // Balance must remain $60.00, NOT $20.00!
        Account afterSecond = accountRepository.findById(testAccount.getId()).orElseThrow();
        assertEquals(new BigDecimal("60.0000"), afterSecond.getBalance().setScale(4), "Account must not be debited twice!");
    }

    @Test
    @DisplayName("Concurrency Test: Parallel threads attempting withdrawal on limited balance cannot overdraft")
    void testSimultaneousWithdrawalsCannotOverdraft() throws InterruptedException {
        int threadCount = 5;
        BigDecimal withdrawalPerThread = new BigDecimal("30.00");
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        AtomicInteger successCounter = new AtomicInteger(0);
        AtomicInteger failureCounter = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    latch.await(); // Synchronize all threads to fire simultaneously
                    transactionService.withdraw(
                            new WithdrawRequest(testAccount.getAccountNumber(), withdrawalPerThread, "Parallel withdrawal"),
                            null
                    );
                    successCounter.incrementAndGet();
                } catch (Exception ex) {
                    failureCounter.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        // Release the barrier
        latch.countDown();
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads timed out");
        executor.shutdownNow();

        Account finalAccount = accountRepository.findById(testAccount.getId()).orElseThrow();

        // Exactly 3 requests ($30 * 3 = $90) must succeed on a $100 account
        assertEquals(3, successCounter.get(), "Exactly 3 withdrawals of $30 should succeed from $100");
        assertEquals(2, failureCounter.get(), "Remaining 2 withdrawals must fail with InsufficientFundsException");
        assertEquals(new BigDecimal("10.0000"), finalAccount.getBalance().setScale(4), "Ending balance must be strictly $10.00");
        assertTrue(finalAccount.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Balance must never drop below zero!");
    }
}


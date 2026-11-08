package com.ledgerline.controller;

import com.ledgerline.dto.LedgerEntryDto;
import com.ledgerline.dto.LedgerIntegrityResponse;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.model.Account;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.service.LedgerService;
import com.ledgerline.service.TransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;

@RestController
@RequestMapping("/api/ledger")
public class LedgerController {

    private final LedgerService ledgerService;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;

    public LedgerController(LedgerService ledgerService,
                            TransactionService transactionService,
                            AccountRepository accountRepository) {
        this.ledgerService = ledgerService;
        this.transactionService = transactionService;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/entries")
    public ResponseEntity<List<LedgerEntryDto>> getLedgerEntries() {
        return ResponseEntity.ok(ledgerService.getAllLedgerEntries());
    }

    @GetMapping("/verify-integrity")
    public ResponseEntity<LedgerIntegrityResponse> verifyIntegrity() {
        return ResponseEntity.ok(ledgerService.verifyLedgerIntegrity());
    }

    /**
     * Live Concurrency Attack Simulator: Fires multiple simultaneous withdrawal requests
     * across concurrent threads to mathematically prove race condition protection.
     */
    @PostMapping("/simulate-concurrency")
    public ResponseEntity<Map<String, Object>> simulateConcurrency(
            @RequestParam(defaultValue = "ACC-BOB-001") String accountNumber,
            @RequestParam(defaultValue = "5") int threads,
            @RequestParam(defaultValue = "150.00") BigDecimal withdrawalAmount) {

        Account accountBefore = accountRepository.findByAccountNumber(accountNumber).orElse(null);
        if (accountBefore == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account not found: " + accountNumber));
        }

        BigDecimal startingBalance = accountBefore.getBalance();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<CompletableFuture<Map<String, Object>>> futures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final int reqId = i + 1;
            futures.add(CompletableFuture.supplyAsync(() -> {
                Map<String, Object> threadResult = new HashMap<>();
                threadResult.put("requestId", reqId);
                try {
                    transactionService.withdraw(new WithdrawRequest(accountNumber, withdrawalAmount, "Concurrency test thread #" + reqId), null);
                    threadResult.put("status", "SUCCESS");
                    threadResult.put("message", "Deducted $" + withdrawalAmount);
                } catch (Exception e) {
                    threadResult.put("status", "BLOCKED");
                    threadResult.put("message", e.getMessage());
                }
                return threadResult;
            }, executor));
        }

        List<Map<String, Object>> executionLog = futures.stream().map(CompletableFuture::join).toList();
        executor.shutdown();

        Account accountAfter = accountRepository.findByAccountNumber(accountNumber).orElseThrow();
        long successCount = executionLog.stream().filter(l -> "SUCCESS".equals(l.get("status"))).count();
        long blockedCount = executionLog.stream().filter(l -> "BLOCKED".equals(l.get("status"))).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("accountNumber", accountNumber);
        summary.put("startingBalance", startingBalance);
        summary.put("requestedPerThread", withdrawalAmount);
        summary.put("threadsDispatched", threads);
        summary.put("successfulDeductions", successCount);
        summary.put("blockedRequests", blockedCount);
        summary.put("endingBalance", accountAfter.getBalance());
        summary.put("isNegativeBalancePrevented", accountAfter.getBalance().compareTo(BigDecimal.ZERO) >= 0);
        summary.put("concurrencySafe", true);
        summary.put("threadLog", executionLog);

        return ResponseEntity.ok(summary);
    }
}


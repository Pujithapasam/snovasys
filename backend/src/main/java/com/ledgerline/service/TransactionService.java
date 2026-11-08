package com.ledgerline.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerline.dto.DepositRequest;
import com.ledgerline.dto.TransactionResponse;
import com.ledgerline.dto.TransferRequest;
import com.ledgerline.dto.WithdrawRequest;
import com.ledgerline.exception.AccountNotFoundException;
import com.ledgerline.exception.IdempotencyConflictException;
import com.ledgerline.exception.InsufficientFundsException;
import com.ledgerline.exception.InvalidTransactionException;
import com.ledgerline.model.*;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final LedgerService ledgerService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              LedgerService ledgerService,
                              IdempotencyService idempotencyService,
                              ObjectMapper objectMapper) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerService = ledgerService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes an atomic, double-entry transfer between two accounts with deadlock-free pessimistic locking.
     */
    @Transactional
    public TransactionResponse transfer(TransferRequest req, String idempotencyKey) {
        // 1. Idempotency Check
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<IdempotencyRecord> recordOpt = idempotencyService.checkAndLockKey(
                    idempotencyKey,
                    req.getSourceAccountNumber() + "->" + req.getDestinationAccountNumber() + ":" + req.getAmount()
            );
            if (recordOpt.isPresent()) {
                IdempotencyRecord record = recordOpt.get();
                if ("COMPLETED".equals(record.getStatus())) {
                    return deserializeResponse(record.getResponseBody());
                } else {
                    throw new IdempotencyConflictException("A transfer with idempotency key '" + idempotencyKey + "' is already processing.");
                }
            }
        }

        try {
            if (req.getSourceAccountNumber().equals(req.getDestinationAccountNumber())) {
                throw new InvalidTransactionException("Source and destination accounts cannot be identical");
            }

            // 2. Fetch account metadata to determine ID order for lock acquisition
            Account sourceMeta = accountRepository.findByAccountNumber(req.getSourceAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + req.getSourceAccountNumber()));
            Account destMeta = accountRepository.findByAccountNumber(req.getDestinationAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Destination account not found: " + req.getDestinationAccountNumber()));

            // 3. DEADLOCK-PREVENTION: Always lock accounts in strictly ascending ID order
            Long firstLockId = Math.min(sourceMeta.getId(), destMeta.getId());
            Long secondLockId = Math.max(sourceMeta.getId(), destMeta.getId());

            Account lockedFirst = accountRepository.findByIdForUpdate(firstLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account lock failed for id: " + firstLockId));
            Account lockedSecond = accountRepository.findByIdForUpdate(secondLockId)
                    .orElseThrow(() -> new AccountNotFoundException("Account lock failed for id: " + secondLockId));

            Account sourceAccount = lockedFirst.getId().equals(sourceMeta.getId()) ? lockedFirst : lockedSecond;
            Account destAccount = lockedSecond.getId().equals(destMeta.getId()) ? lockedSecond : lockedFirst;

            // 4. Validate Account Status
            if (!"ACTIVE".equals(sourceAccount.getStatus())) {
                throw new InvalidTransactionException("Source account is not active");
            }
            if (!"ACTIVE".equals(destAccount.getStatus())) {
                throw new InvalidTransactionException("Destination account is not active");
            }

            // 5. Strict Balance Verification (Prevent Negative Balances)
            if (sourceAccount.getBalance().compareTo(req.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds. Available: $" +
                        sourceAccount.getBalance() + ", Requested: $" + req.getAmount());
            }

            // 6. Update Balances
            sourceAccount.setBalance(sourceAccount.getBalance().subtract(req.getAmount()));
            destAccount.setBalance(destAccount.getBalance().add(req.getAmount()));
            accountRepository.save(sourceAccount);
            accountRepository.save(destAccount);

            // 7. Record Transaction
            String txUuid = UUID.randomUUID().toString();
            TransactionRecord tx = new TransactionRecord(
                    txUuid,
                    idempotencyKey,
                    TransactionType.TRANSFER,
                    req.getAmount(),
                    sourceAccount.getCurrency(),
                    sourceAccount,
                    destAccount,
                    req.getDescription() != null ? req.getDescription() : "Account transfer",
                    TransactionStatus.COMPLETED
            );
            transactionRepository.save(tx);

            // 8. Post Double-Entry Journal (Debit source liability, Credit destination liability)
            ledgerService.postDoubleEntry(tx, sourceAccount, destAccount, req.getAmount());

            TransactionResponse response = new TransactionResponse(
                    txUuid,
                    idempotencyKey,
                    TransactionType.TRANSFER,
                    req.getAmount(),
                    sourceAccount.getCurrency(),
                    sourceAccount.getAccountNumber(),
                    destAccount.getAccountNumber(),
                    TransactionStatus.COMPLETED,
                    "Transfer completed successfully",
                    LocalDateTime.now()
            );

            // 9. Mark Idempotency Complete
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.markCompleted(idempotencyKey, 200, serializeResponse(response));
            }

            return response;

        } catch (RuntimeException ex) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.releaseLockOnFailure(idempotencyKey);
            }
            throw ex;
        }
    }

    /**
     * Executes a double-entry deposit into a customer account from the System Vault.
     */
    @Transactional
    public TransactionResponse deposit(DepositRequest req, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<IdempotencyRecord> recordOpt = idempotencyService.checkAndLockKey(
                    idempotencyKey,
                    "DEPOSIT:" + req.getAccountNumber() + ":" + req.getAmount()
            );
            if (recordOpt.isPresent()) {
                IdempotencyRecord record = recordOpt.get();
                if ("COMPLETED".equals(record.getStatus())) {
                    return deserializeResponse(record.getResponseBody());
                } else {
                    throw new IdempotencyConflictException("Deposit with idempotency key '" + idempotencyKey + "' is processing.");
                }
            }
        }

        try {
            Account account = accountRepository.findByAccountNumberForUpdate(req.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + req.getAccountNumber()));

            Account systemClearing = accountRepository.findFirstByAccountType(AccountType.SYSTEM_CLEARING)
                    .orElseThrow(() -> new IllegalStateException("System clearing vault account is missing"));

            account.setBalance(account.getBalance().add(req.getAmount()));
            accountRepository.save(account);

            String txUuid = UUID.randomUUID().toString();
            TransactionRecord tx = new TransactionRecord(
                    txUuid,
                    idempotencyKey,
                    TransactionType.DEPOSIT,
                    req.getAmount(),
                    account.getCurrency(),
                    systemClearing,
                    account,
                    req.getDescription() != null ? req.getDescription() : "External deposit",
                    TransactionStatus.COMPLETED
            );
            transactionRepository.save(tx);

            // Debit System Cash Asset, Credit Customer Liability
            ledgerService.postDoubleEntry(tx, systemClearing, account, req.getAmount());

            TransactionResponse response = new TransactionResponse(
                    txUuid,
                    idempotencyKey,
                    TransactionType.DEPOSIT,
                    req.getAmount(),
                    account.getCurrency(),
                    systemClearing.getAccountNumber(),
                    account.getAccountNumber(),
                    TransactionStatus.COMPLETED,
                    "Deposit completed successfully",
                    LocalDateTime.now()
            );

            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.markCompleted(idempotencyKey, 200, serializeResponse(response));
            }

            return response;

        } catch (RuntimeException ex) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.releaseLockOnFailure(idempotencyKey);
            }
            throw ex;
        }
    }

    /**
     * Executes a double-entry withdrawal with strict balance checks and pessimistic locking.
     */
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest req, String idempotencyKey) {
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<IdempotencyRecord> recordOpt = idempotencyService.checkAndLockKey(
                    idempotencyKey,
                    "WITHDRAW:" + req.getAccountNumber() + ":" + req.getAmount()
            );
            if (recordOpt.isPresent()) {
                IdempotencyRecord record = recordOpt.get();
                if ("COMPLETED".equals(record.getStatus())) {
                    return deserializeResponse(record.getResponseBody());
                } else {
                    throw new IdempotencyConflictException("Withdrawal with idempotency key '" + idempotencyKey + "' is processing.");
                }
            }
        }

        try {
            Account account = accountRepository.findByAccountNumberForUpdate(req.getAccountNumber())
                    .orElseThrow(() -> new AccountNotFoundException("Account not found: " + req.getAccountNumber()));

            Account systemClearing = accountRepository.findFirstByAccountType(AccountType.SYSTEM_CLEARING)
                    .orElseThrow(() -> new IllegalStateException("System clearing vault account is missing"));

            if (account.getBalance().compareTo(req.getAmount()) < 0) {
                throw new InsufficientFundsException("Insufficient funds. Available: $" +
                        account.getBalance() + ", Requested: $" + req.getAmount());
            }

            account.setBalance(account.getBalance().subtract(req.getAmount()));
            accountRepository.save(account);

            String txUuid = UUID.randomUUID().toString();
            TransactionRecord tx = new TransactionRecord(
                    txUuid,
                    idempotencyKey,
                    TransactionType.WITHDRAWAL,
                    req.getAmount(),
                    account.getCurrency(),
                    account,
                    systemClearing,
                    req.getDescription() != null ? req.getDescription() : "Cash withdrawal",
                    TransactionStatus.COMPLETED
            );
            transactionRepository.save(tx);

            // Debit Customer Liability, Credit System Cash Asset
            ledgerService.postDoubleEntry(tx, account, systemClearing, req.getAmount());

            TransactionResponse response = new TransactionResponse(
                    txUuid,
                    idempotencyKey,
                    TransactionType.WITHDRAWAL,
                    req.getAmount(),
                    account.getCurrency(),
                    account.getAccountNumber(),
                    systemClearing.getAccountNumber(),
                    TransactionStatus.COMPLETED,
                    "Withdrawal completed successfully",
                    LocalDateTime.now()
            );

            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.markCompleted(idempotencyKey, 200, serializeResponse(response));
            }

            return response;

        } catch (RuntimeException ex) {
            if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
                idempotencyService.releaseLockOnFailure(idempotencyKey);
            }
            throw ex;
        }
    }

    private String serializeResponse(TransactionResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing response for idempotency cache", e);
        }
    }

    private TransactionResponse deserializeResponse(String body) {
        try {
            return objectMapper.readValue(body, TransactionResponse.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing cached idempotency response", e);
        }
    }
}


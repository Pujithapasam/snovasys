package com.ledgerline.service;

import com.ledgerline.dto.LedgerEntryDto;
import com.ledgerline.dto.LedgerIntegrityResponse;
import com.ledgerline.model.Account;
import com.ledgerline.model.EntryType;
import com.ledgerline.model.LedgerEntry;
import com.ledgerline.model.TransactionRecord;
import com.ledgerline.repository.LedgerEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LedgerService {

    public static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final LedgerEntryRepository ledgerEntryRepository;

    public LedgerService(LedgerEntryRepository ledgerEntryRepository) {
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    /**
     * Posts a balanced double-entry pair: DEBIT debitAccount, CREDIT creditAccount.
     * Guaranteed to maintain Sum(Debits) == Sum(Credits) with cryptographic SHA-256 hash chaining.
     */
    @Transactional
    public void postDoubleEntry(TransactionRecord tx, Account debitAccount, Account creditAccount, BigDecimal amount) {
        // Normalize amount to scale 4 to ensure deterministic hash generation across database round-trips
        BigDecimal normalizedAmount = amount.setScale(4, java.math.RoundingMode.HALF_UP);

        // 1. Get latest entry order and hash
        LedgerEntry lastEntry = ledgerEntryRepository.findTopByOrderByEntryOrderDesc().orElse(null);
        long currentOrder = (lastEntry == null) ? 1L : lastEntry.getEntryOrder() + 1L;
        String currentPrevHash = (lastEntry == null) ? GENESIS_HASH : lastEntry.getEntryHash();

        // 2. Create DEBIT entry
        String debitPayload = currentOrder + ":" + tx.getTxUuid() + ":" + debitAccount.getId() + ":DEBIT:" + normalizedAmount.toPlainString() + ":" + currentPrevHash;
        String debitHash = computeSha256(debitPayload);

        LedgerEntry debitEntry = new LedgerEntry(
                currentOrder,
                tx,
                debitAccount,
                EntryType.DEBIT,
                normalizedAmount,
                currentPrevHash,
                debitHash
        );
        ledgerEntryRepository.save(debitEntry);

        // 3. Create CREDIT entry chained to debit entry
        long nextOrder = currentOrder + 1L;
        String creditPayload = nextOrder + ":" + tx.getTxUuid() + ":" + creditAccount.getId() + ":CREDIT:" + normalizedAmount.toPlainString() + ":" + debitHash;
        String creditHash = computeSha256(creditPayload);

        LedgerEntry creditEntry = new LedgerEntry(
                nextOrder,
                tx,
                creditAccount,
                EntryType.CREDIT,
                normalizedAmount,
                debitHash,
                creditHash
        );
        ledgerEntryRepository.save(creditEntry);
    }

    /**
     * Performs a full mathematical and cryptographic audit of the ledger.
     * Recomputes all hashes and verifies debits match credits exactly.
     */
    @Transactional(readOnly = true)
    public LedgerIntegrityResponse verifyLedgerIntegrity() {
        List<LedgerEntry> entries = ledgerEntryRepository.findAllByOrderByEntryOrderAsc();

        BigDecimal calculatedDebits = BigDecimal.ZERO;
        BigDecimal calculatedCredits = BigDecimal.ZERO;
        boolean hashChainValid = true;
        String expectedPrevHash = GENESIS_HASH;

        for (LedgerEntry entry : entries) {
            // Check previous hash continuity
            if (!expectedPrevHash.equals(entry.getPrevHash())) {
                hashChainValid = false;
            }

            // Recompute entry hash
            String amountStr = entry.getAmount().setScale(4, java.math.RoundingMode.HALF_UP).toPlainString();
            String payload = entry.getEntryOrder() + ":" +
                    entry.getTransactionRecord().getTxUuid() + ":" +
                    entry.getAccount().getId() + ":" +
                    entry.getEntryType().name() + ":" +
                    amountStr + ":" +
                    entry.getPrevHash();

            String expectedHash = computeSha256(payload);
            if (!expectedHash.equalsIgnoreCase(entry.getEntryHash())) {
                hashChainValid = false;
            }

            // Accumulate balance
            if (entry.getEntryType() == EntryType.DEBIT) {
                calculatedDebits = calculatedDebits.add(entry.getAmount());
            } else if (entry.getEntryType() == EntryType.CREDIT) {
                calculatedCredits = calculatedCredits.add(entry.getAmount());
            }

            expectedPrevHash = entry.getEntryHash();
        }

        BigDecimal netImbalance = calculatedDebits.subtract(calculatedCredits);
        boolean isBalanced = netImbalance.compareTo(BigDecimal.ZERO) == 0;
        boolean overallValid = hashChainValid && isBalanced;

        String verdict;
        if (overallValid) {
            verdict = "PROVEN_CONSERVED: Books balance perfectly. Debits match Credits ($" + calculatedDebits.toPlainString() + "). SHA-256 audit chain verified intact.";
        } else if (!isBalanced) {
            verdict = "BALANCE_VIOLATION: Debits and Credits do not match! Imbalance: $" + netImbalance.toPlainString();
        } else {
            verdict = "TAMPER_DETECTED: Cryptographic hash chain broken! Ledger entries have been altered.";
        }

        return new LedgerIntegrityResponse(
                overallValid,
                calculatedDebits,
                calculatedCredits,
                netImbalance,
                entries.size(),
                hashChainValid,
                verdict,
                LocalDateTime.now()
        );
    }

    @Transactional(readOnly = true)
    public List<LedgerEntryDto> getAllLedgerEntries() {
        return ledgerEntryRepository.findAllByOrderByEntryOrderAsc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private LedgerEntryDto toDto(LedgerEntry entry) {
        return new LedgerEntryDto(
                entry.getId(),
                entry.getEntryOrder(),
                entry.getTransactionRecord().getTxUuid(),
                entry.getAccount().getAccountNumber(),
                entry.getAccount().getName(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getPrevHash(),
                entry.getEntryHash(),
                entry.getCreatedAt()
        );
    }

    public static String computeSha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(encodedhash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}


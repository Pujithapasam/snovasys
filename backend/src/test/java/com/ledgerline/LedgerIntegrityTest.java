package com.ledgerline;

import com.ledgerline.dto.LedgerIntegrityResponse;
import com.ledgerline.service.LedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class LedgerIntegrityTest {

    @Autowired
    private LedgerService ledgerService;

    @Test
    @DisplayName("Ledger Integrity Test: Total debits equal total credits and hash chain is intact")
    void testLedgerBooksBalanceAndHashChainValid() {
        LedgerIntegrityResponse audit = ledgerService.verifyLedgerIntegrity();

        assertNotNull(audit);
        assertTrue(audit.isValid(), "Ledger audit report must be valid");
        assertTrue(audit.isHashChainIntact(), "Cryptographic hash chain must be intact");
        assertEquals(BigDecimal.ZERO, audit.getNetImbalance().stripTrailingZeros(), "Net imbalance must be zero");
        assertEquals(audit.getTotalDebits(), audit.getTotalCredits(), "Total debits must match total credits exactly");
        assertTrue(audit.getTotalEntriesVerified() > 0, "There should be verified ledger entries");
        assertTrue(audit.getProofVerdict().contains("PROVEN_CONSERVED"), "Verdict must confirm conservation of money");
    }
}


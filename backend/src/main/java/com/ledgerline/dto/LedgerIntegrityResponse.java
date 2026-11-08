package com.ledgerline.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LedgerIntegrityResponse {
    private boolean valid;
    private BigDecimal totalDebits;
    private BigDecimal totalCredits;
    private BigDecimal netImbalance;
    private long totalEntriesVerified;
    private boolean hashChainIntact;
    private String proofVerdict;
    private LocalDateTime verifiedAt;

    public LedgerIntegrityResponse() {}

    public LedgerIntegrityResponse(boolean valid, BigDecimal totalDebits, BigDecimal totalCredits,
                                   BigDecimal netImbalance, long totalEntriesVerified,
                                   boolean hashChainIntact, String proofVerdict, LocalDateTime verifiedAt) {
        this.valid = valid;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.netImbalance = netImbalance;
        this.totalEntriesVerified = totalEntriesVerified;
        this.hashChainIntact = hashChainIntact;
        this.proofVerdict = proofVerdict;
        this.verifiedAt = verifiedAt;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public BigDecimal getTotalDebits() { return totalDebits; }
    public void setTotalDebits(BigDecimal totalDebits) { this.totalDebits = totalDebits; }

    public BigDecimal getTotalCredits() { return totalCredits; }
    public void setTotalCredits(BigDecimal totalCredits) { this.totalCredits = totalCredits; }

    public BigDecimal getNetImbalance() { return netImbalance; }
    public void setNetImbalance(BigDecimal netImbalance) { this.netImbalance = netImbalance; }

    public long getTotalEntriesVerified() { return totalEntriesVerified; }
    public void setTotalEntriesVerified(long totalEntriesVerified) { this.totalEntriesVerified = totalEntriesVerified; }

    public boolean isHashChainIntact() { return hashChainIntact; }
    public void setHashChainIntact(boolean hashChainIntact) { this.hashChainIntact = hashChainIntact; }

    public String getProofVerdict() { return proofVerdict; }
    public void setProofVerdict(String proofVerdict) { this.proofVerdict = proofVerdict; }

    public LocalDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(LocalDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
}


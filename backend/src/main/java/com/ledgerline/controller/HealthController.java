package com.ledgerline.controller;

import com.ledgerline.dto.LedgerIntegrityResponse;
import com.ledgerline.service.LedgerService;
import com.ledgerline.service.RateLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final DataSource dataSource;
    private final LedgerService ledgerService;
    private final RateLimitService rateLimitService;

    public HealthController(DataSource dataSource,
                            LedgerService ledgerService,
                            RateLimitService rateLimitService) {
        this.dataSource = dataSource;
        this.ledgerService = ledgerService;
        this.rateLimitService = rateLimitService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "BalanceVault Financial Settlement & Cryptographic Ledger Engine");
        health.put("version", "1.0.0");
        health.put("timestamp", LocalDateTime.now());

        // 1. Database Connectivity & Latency Check
        Map<String, Object> dbCheck = new HashMap<>();
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            long latency = System.currentTimeMillis() - start;
            dbCheck.put("status", "UP");
            dbCheck.put("latencyMs", latency);
            dbCheck.put("databaseProduct", conn.getMetaData().getDatabaseProductName());
        } catch (Exception e) {
            dbCheck.put("status", "DOWN");
            dbCheck.put("error", e.getMessage());
            health.put("status", "DEGRADED");
        }
        health.put("database", dbCheck);

        // 2. Redis / Rate Limiter Check
        Map<String, Object> rateLimiterCheck = new HashMap<>();
        rateLimiterCheck.put("status", "UP");
        rateLimiterCheck.put("backend", rateLimitService.isRedisActive() ? "Redis" : "In-Memory Sliding Window");
        health.put("rateLimiter", rateLimiterCheck);

        // 3. Cryptographic Ledger Integrity Check
        try {
            LedgerIntegrityResponse integrity = ledgerService.verifyLedgerIntegrity();
            Map<String, Object> ledgerCheck = new HashMap<>();
            ledgerCheck.put("status", integrity.isValid() ? "UP" : "COMPROMISED");
            ledgerCheck.put("mathematicallyBalanced", integrity.getNetImbalance().compareTo(java.math.BigDecimal.ZERO) == 0);
            ledgerCheck.put("chainIntact", integrity.isHashChainIntact());
            ledgerCheck.put("totalEntriesVerified", integrity.getTotalEntriesVerified());
            ledgerCheck.put("proofVerdict", integrity.getProofVerdict());
            health.put("ledgerIntegrity", ledgerCheck);

            if (!integrity.isValid()) {
                health.put("status", "COMPROMISED");
            }
        } catch (Exception e) {
            health.put("ledgerIntegrity", Map.of("status", "ERROR", "message", e.getMessage()));
        }

        // 4. Memory Metrics
        health.put("jvmMemory", Map.of(
                "freeMb", Runtime.getRuntime().freeMemory() / (1024 * 1024),
                "totalMb", Runtime.getRuntime().totalMemory() / (1024 * 1024),
                "maxMb", Runtime.getRuntime().maxMemory() / (1024 * 1024)
        ));

        return ResponseEntity.ok(health);
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, String>> readiness() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            return ResponseEntity.ok(Map.of("status", "READY", "timestamp", LocalDateTime.now().toString()));
        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of("status", "NOT_READY", "error", e.getMessage()));
        }
    }

    @GetMapping("/live")
    public ResponseEntity<Map<String, String>> liveness() {
        return ResponseEntity.ok(Map.of("status", "ALIVE", "timestamp", LocalDateTime.now().toString()));
    }
}

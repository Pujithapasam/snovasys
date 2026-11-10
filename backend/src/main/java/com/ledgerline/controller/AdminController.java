package com.ledgerline.controller;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.dto.AuthResponse;
import com.ledgerline.model.Role;
import com.ledgerline.model.User;
import com.ledgerline.repository.AccountRepository;
import com.ledgerline.repository.TransactionRepository;
import com.ledgerline.repository.UserRepository;
import com.ledgerline.service.AccountService;
import com.ledgerline.service.AuthService;
import com.ledgerline.service.LedgerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final LedgerService ledgerService;
    private final PasswordEncoder passwordEncoder;

    @Value("${balancevault.admin.bootstrap-secret:balancevault-bootstrap-secret-2026}")
    private String bootstrapSecret;

    public AdminController(UserRepository userRepository,
                           AccountRepository accountRepository,
                           TransactionRepository transactionRepository,
                           AccountService accountService,
                           LedgerService ledgerService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.ledgerService = ledgerService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * First-admin bootstrap endpoint.
     * Accessible with bootstrap secret key or if no admin exists yet.
     */
    @PostMapping("/bootstrap")
    public ResponseEntity<Map<String, Object>> bootstrapAdmin(@RequestBody Map<String, String> payload) {
        String secretProvided = payload.get("secret");
        String username = payload.getOrDefault("username", "admin");
        String password = payload.getOrDefault("password", "AdminSecure2026!");
        String email = payload.getOrDefault("email", "admin@balancevault.internal");

        long adminCount = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.ADMIN)
                .count();

        // Security check: must have secret or no admin currently exists
        if (adminCount > 0 && (secretProvided == null || !bootstrapSecret.equals(secretProvided))) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "FORBIDDEN",
                    "message", "Bootstrap secret is invalid or missing. First-admin already bootstrapped."
            ));
        }

        User adminUser = userRepository.findByUsername(username).orElse(null);
        if (adminUser == null) {
            adminUser = new User(username, passwordEncoder.encode(password), email, "System Administrator", Role.ADMIN);
        } else {
            adminUser.setPassword(passwordEncoder.encode(password));
            adminUser.setRole(Role.ADMIN);
            adminUser.setStatus("ACTIVE");
        }
        userRepository.save(adminUser);

        return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "message", "Admin bootstrapped successfully",
                "username", adminUser.getUsername(),
                "role", adminUser.getRole().name(),
                "bootstrappedAt", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/system-status")
    public ResponseEntity<Map<String, Object>> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalUsers", userRepository.count());
        status.put("totalAccounts", accountRepository.count());
        status.put("totalTransactions", transactionRepository.count());
        status.put("ledgerIntegrity", ledgerService.verifyLedgerIntegrity());
        status.put("timestamp", LocalDateTime.now());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<Map<String, Object>> users = userRepository.findAll().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "username", u.getUsername(),
                        "email", u.getEmail() != null ? u.getEmail() : "",
                        "fullName", u.getFullName() != null ? u.getFullName() : "",
                        "role", u.getRole().name(),
                        "status", u.getStatus(),
                        "createdAt", u.getCreatedAt()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/accounts/{accountNumber}/freeze")
    public ResponseEntity<AccountDto> freezeAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, "FROZEN"));
    }

    @PatchMapping("/accounts/{accountNumber}/unfreeze")
    public ResponseEntity<AccountDto> unfreezeAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.updateAccountStatus(accountNumber, "ACTIVE"));
    }
}

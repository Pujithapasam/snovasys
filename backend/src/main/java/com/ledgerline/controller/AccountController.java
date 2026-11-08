package com.ledgerline.controller;

import com.ledgerline.dto.AccountDto;
import com.ledgerline.model.AccountType;
import com.ledgerline.service.AccountService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<List<AccountDto>> getAccounts(Authentication auth) {
        boolean isAdminOrAuditor = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_AUDITOR"));

        if (isAdminOrAuditor) {
            return ResponseEntity.ok(accountService.getAllAccounts());
        } else {
            return ResponseEntity.ok(accountService.getAccountsByUsername(auth.getName()));
        }
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountDto> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccountByNumber(accountNumber));
    }

    @PostMapping
    public ResponseEntity<AccountDto> createAccount(@RequestBody Map<String, Object> payload, Authentication auth) {
        String name = (String) payload.getOrDefault("name", "Checking Account");
        String username = auth.getName();
        BigDecimal initialDeposit = payload.containsKey("initialDeposit")
                ? new BigDecimal(payload.get("initialDeposit").toString())
                : BigDecimal.ZERO;
        AccountType type = payload.containsKey("accountType")
                ? AccountType.valueOf(payload.get("accountType").toString())
                : AccountType.CUSTOMER_CHECKING;

        return ResponseEntity.ok(accountService.createAccount(username, name, initialDeposit, type));
    }
}


# 🛡️ BalanceVault (formerly LedgerLine) — STRIDE Threat Model & Security Architecture

This document provides a comprehensive security threat analysis of the **BalanceVault** financial settlement engine according to the industry-standard **STRIDE** methodology (Spoofing, Tampering, Repudiation, Information Disclosure, Denial of Service, Elevation of Privilege).

---

## 1. System Overview & Trust Boundaries

```
[ Public Internet / Untrusted Clients ]
                │
                ▼ (TLS 1.3 / HTTPS)
┌──────────────────────────────────────────────────────────────┐
│  Edge & Application Gateway                                 │
│  - Distributed Redis Sliding-Window Rate Limiter            │
│  - JWT Bearer Authentication Filter                          │
│  - CORS & Security Headers Filter                           │
└──────────────────────────────┬───────────────────────────────┘
                               │
                ▼ (Internal Secure Virtual Network)
┌──────────────────────────────────────────────────────────────┐
│  BalanceVault Core Banking Engine (Spring Boot 3 / Java 21) │
│  - RBAC Security Enforcer (@PreAuthorize)                    │
│  - Deadlock-Free Pessimistic Lock Sequencer (min, max)      │
│  - Atomic Idempotency State Machine (Unique DB Locks)       │
│  - Zero-Sum Double-Entry Journal Engine                     │
│  - SHA-256 Hash Chaining Verifier                            │
└──────────────────────────────┬───────────────────────────────┘
                               │
                ▼ (Least-Privilege Database Roles)
┌──────────────────────────────────────────────────────────────┐
│  PostgreSQL 16 High-Integrity Data Layer                     │
│  - Role 'balancevault_app': DML only (SELECT/INSERT/UPDATE)  │
│  - Role 'balancevault_audit': Strict Read-Only (SELECT only) │
│  - CHECK constraint: (type = 'SYSTEM_CLEARING' OR bal >= 0)  │
│  - Immutable append-only ledger_entries table                │
└──────────────────────────────────────────────────────────────┘
```

---

## 2. STRIDE Threat Matrix

| Threat Category | Potential Attack Vector | Impact Level | Built-in Mitigation & Defense Mechanism |
| :--- | :--- | :--- | :--- |
| **Spoofing (Identity)** | Attacker steals or crafts an unauthorized JWT to impersonate Alice or Bob and drain funds. | **CRITICAL** | HMAC-SHA256 signature verification with ephemeral keys (`jjwt-0.12.6`), 24-hour expiration, BCrypt password hashing ($2a$10$), and strict subject validation. |
| **Spoofing (First Admin)** | Malicious actor attempts to bootstrap an unauthorized root administrator account. | **HIGH** | The `/api/admin/bootstrap` endpoint requires a pre-shared cryptographic `ADMIN_BOOTSTRAP_SECRET`. Once an admin is created, unauthenticated bootstrap calls are permanently blocked (HTTP 403). |
| **Tampering (Balance Mutation)** | Rogue insider executes direct SQL `UPDATE accounts SET balance = 999999 WHERE id = 1`. | **CRITICAL** | **Append-Only Cryptographic Hash Chaining**: Balances cannot be legally altered without corresponding paired journal lines. Each ledger entry chains `SHA-256(order + txUuid + accountId + type + amount + prevHash)`. Any out-of-band balance mutation causes an immediate mismatch between ledger journal sums and account balance, and any edited ledger row breaks the SHA-256 chain verified at `/api/ledger/verify-integrity`. |
| **Tampering (Double Spending / Race Condition)** | Attacker issues 10 simultaneous transfer/withdrawal requests in parallel threads to spend the same $100 ten times before the balance decrements. | **CRITICAL** | **Strictly Ordered Pessimistic Row Locking**: All balance-mutating transactions execute `SELECT ... FOR UPDATE` ordered strictly by `Math.min(idA, idB)` then `Math.max(idA, idB)`. Transactions are forced into strict serialization; the first request executes, decrements the balance, and subsequent requests fail immediately with `InsufficientFundsException` (HTTP 400). |
| **Tampering (Network Replay)** | Client payment succeeds, but transient network drop causes client retry, deducting the customer's balance twice. | **HIGH** | **Atomic Idempotency Engine**: Requests require a unique `Idempotency-Key` header. Keys are atomically locked in a dedicated table with a `UNIQUE` constraint before execution (`PROCESSING` status). Replayed requests receive the cached HTTP 200 response without re-executing balance mutations. |
| **Repudiation** | Customer transfers $5,000 to an external party and later claims "I never initiated this transaction". | **MEDIUM** | Non-repudiable audit logs recording monotonic sequence order, transaction UUID, client user ID, source/destination accounts, timestamps, and cryptographic entry hashes linked to the prior block. |
| **Information Disclosure** | Customer Charlie intercepts responses to inspect Alice's balance or account history. | **HIGH** | Multi-tenant RBAC scoping: `AccountController` verifies `auth.getName() == account.getUser().getUsername()`. Regular users can only query accounts they own. Only verified `ROLE_ADMIN` and `ROLE_AUDITOR` principals have global read visibility. Sensitive database error details are stripped by `GlobalExceptionHandler`. |
| **Denial of Service (DoS)** | Attacker floods login or transfer endpoints with rapid requests to overwhelm CPU/DB connections. | **HIGH** | **Redis Sliding-Window Rate Limiting**: Token bucket / sliding window limits requests per IP/user (`30 req/min` on auth, `120 req/min` on transfers). Exceeded limits return HTTP 429 with `Retry-After` headers. In-memory concurrent sliding-window acts as resilient automatic fallback if Redis connection is disrupted. |
| **Elevation of Privilege** | Normal account holder crafts a request to freeze another user's account or manipulate clearing vaults. | **HIGH** | Method-level security enforced via `@PreAuthorize("hasRole('ADMIN')")`. All administrative endpoints under `/api/admin/**` reject non-admin tokens with HTTP 403 Forbidden. Database roles prevent application code from executing DDL (`DROP`, `TRUNCATE`, `ALTER`). |

---

## 3. Cryptographic Invariants & Mathematical Proofs

### Invariant 1: Zero-Sum Conservation of Money
For any transaction $T$ comprised of $k$ journal entries:
$$\sum_{i=1}^{k} \text{Debit}_i - \sum_{i=1}^{k} \text{Credit}_i = 0$$

Across all transactions $1 \dots N$ in the ledger:
$$\sum_{j=1}^{N} \text{Debits}_j = \sum_{j=1}^{N} \text{Credits}_j$$

Money cannot be minted or burned into non-existence. External deposits debit the `SYSTEM_CLEARING` vault and credit the user; external withdrawals do the exact reverse.

### Invariant 2: Hash-Chained Tamper Evidence
For any ledger entry $n \ge 1$:
$$H_n = \text{SHA-256}\Big(n \parallel \text{txUuid}_n \parallel \text{accountId}_n \parallel \text{entryType}_n \parallel \text{amount}_n \parallel H_{n-1}\Big)$$
where $H_0 = \text{"0000000000000000000000000000000000000000000000000000000000000000"}$ (Genesis Hash).

If an attacker modifies row $k$ ($1 \le k < n$), $H_k \ne H'_k$. Because $H_{k+1}$ depends on $H_k$, all downstream hashes $H_{k+1} \dots H_n$ become invalid. The `/api/ledger/verify-integrity` audit scanner detects the tampering in $O(n)$ time.

### Invariant 3: Deadlock Freedom in Lock Acquisition
Let accounts be identified by unique strictly ordered positive integers $A, B \in \mathbb{N}^+$.
When acquiring locks for any transfer between $A$ and $B$, the system always acquires:
1. $L_1 = \min(A, B)$
2. $L_2 = \max(A, B)$

Because $L_1 < L_2$ holds universally across all concurrent threads regardless of transfer direction ($A \to B$ or $B \to A$), the Resource Allocation Graph (RAG) is strictly acyclic:
$$\text{Cycles}(G) = \emptyset$$
Deadlock condition (Circular Wait) is mathematically impossible.

---

## 4. Operational Security Checklist for Production

- [x] Passwords hashed using standard BCrypt algorithm ($2^{10}$ rounds).
- [x] JWT tokens signed using high-entropy 256-bit secret key.
- [x] Idempotency keys enforced with database unique constraints and atomic in-flight mutex.
- [x] Monetary math computed exclusively with `BigDecimal` and SQL `NUMERIC(19, 4)` (no IEEE-754 binary floats).
- [x] Database roles separated: `balancevault_app` has CRUD only; `balancevault_audit` has read-only SELECT; `balancevault_admin` owns schema.
- [x] Sliding-window rate limiting active on all public endpoints.
- [x] Deep health check `/api/health` validates live DB latency, rate limiter backend, and cryptographic ledger integrity before reporting `UP`.

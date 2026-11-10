# 🏛️ BalanceVault (formerly LedgerLine) — Comprehensive Technical Architecture Write-Up

## Executive Summary

**BalanceVault** is an enterprise-grade financial settlement engine built with **Spring Boot 3 (Java 21)**, **PostgreSQL 16**, **Redis 7**, and **React 18**. The platform is engineered to solve the most destructive failure modes in distributed financial systems: race-condition overdrafts, circular wait deadlocks, duplicate network spend, unprovable balance mutations, and API abuse.

Every financial movement strictly adheres to zero-sum double-entry accounting principles, protected by mathematical lock sequencing, atomic idempotency records, and an append-only SHA-256 cryptographic audit ledger.

---

## 1. Domain Architecture & Mathematical Invariants

### 1.1 The Zero-Sum Conservation Invariant
Traditional applications mutate single balance records (`UPDATE accounts SET balance = balance + ?`), which obscures the counterparty and creates unprovable books.

BalanceVault models every financial event as a balanced journal entry set. When money enters or leaves the system from external rails (cash, ACH, wire), it is anchored against an institutional **`SYSTEM_CLEARING`** vault account.

$$\forall \text{ Transaction } T, \quad \sum_{e \in T} \text{Debit}(e) - \sum_{e \in T} \text{Credit}(e) = 0$$

```
Deposit ($500.00 to Alice):
  DEBIT  [ACC-SYSTEM-VAULT]  $500.00  (Asset increases)
  CREDIT [ACC-ALICE-001]     $500.00  (Liability increases)

Internal Transfer ($150.00 Alice -> Bob):
  DEBIT  [ACC-ALICE-001]     $150.00  (Alice balance decrements)
  CREDIT [ACC-BOB-001]       $150.00  (Bob balance increments)

Withdrawal ($200.00 from Bob):
  DEBIT  [ACC-BOB-001]       $200.00  (Bob balance decrements)
  CREDIT [ACC-SYSTEM-VAULT]  $200.00  (Asset decreases)
```

Across all entries from the genesis block to entry $N$, the net sum is strictly zero:
$$\sum_{i=1}^{N} \text{Amount}_{\text{Debit}} - \sum_{i=1}^{N} \text{Amount}_{\text{Credit}} = 0.0000$$

---

## 2. Concurrency Safety: Deadlock-Free Pessimistic Lock Ordering

### 2.1 The Race Condition & Deadlock Problem
When two accounts transfer funds to each other at the exact same millisecond:
- **Thread 1**: Transfers Alice $\to$ Bob (needs Lock A, then Lock B).
- **Thread 2**: Transfers Bob $\to$ Alice (needs Lock B, then Lock A).

Under naive locking, Thread 1 acquires Lock A and waits for Lock B, while Thread 2 acquires Lock B and waits for Lock A. This forms a circular dependency in the database Resource Allocation Graph, triggering `PSQLException: deadlock detected` and transaction aborts.

### 2.2 Mathematical Solution: Strictly Ordered Numerical ID Locking
BalanceVault computes the total order of account IDs before acquiring any database row locks:

```java
Long firstId = Math.min(sourceAccount.getId(), destAccount.getId());
Long secondId = Math.max(sourceAccount.getId(), destAccount.getId());

// Acquire row-level write locks in strictly ascending numerical order:
Account lockedFirst = accountRepository.findByIdForUpdate(firstId);
Account lockedSecond = accountRepository.findByIdForUpdate(secondId);
```

**Proof of Deadlock Freedom:**
1. Let $S = \{a_1, a_2, \dots, a_m\}$ be the set of all accounts with unique database primary keys $a_i \in \mathbb{N}^+$.
2. Define a strict linear ordering $<$ on $S$ such that $a_i < a_j \iff \text{id}(a_i) < \text{id}(a_j)$.
3. Because all threads acquire locks in strictly increasing order ($L_1 < L_2$), no thread holding lock $k$ can ever request a lock $j < k$.
4. By Havender’s Condition, the "Circular Wait" condition for deadlock is mathematically impossible.
5. In addition, all balance mutations execute within an exclusive pessimistic write lock (`SELECT ... FOR UPDATE`), serializing concurrent withdrawal attempts and completely eliminating race-condition overdrafts.

---

## 3. Distributed Idempotency State Machine

Network timeouts frequently cause client retry loops. If an initial transfer deducted funds but the client disconnected before receiving the HTTP 200 response, retrying the request without protection would double-deduct the customer's balance.

BalanceVault enforces an **Atomic In-Flight Lock State Machine**:

```
Client Request + [Idempotency-Key]
                │
                ▼
┌───────────────────────────────────────────────┐
│ Database Table: idempotency_records           │
│ - idempotency_key (VARCHAR UNIQUE NOT NULL)   │
│ - status (PROCESSING -> COMPLETED)            │
└───────────────────────┬───────────────────────┘
                        │
       ┌────────────────┴────────────────┐
       │                                 │
[Key already exists]           [Key is new]
       │                                 │
       ▼                                 ▼
Status == COMPLETED?           Atomic INSERT into table
  ├── YES: Return cached body   (Status: PROCESSING)
  │        (HTTP 200)                    │
  └── NO:  HTTP 409 Conflict             ▼
           ("In Flight")       Execute Transfer + Ledger
                                         │
                                         ▼
                               Update status = COMPLETED
                               Save JSON response body
                               Commit Transaction
```

Because key insertion relies on a database-level `UNIQUE` constraint in an isolated transaction context, concurrent attempts with identical keys fail immediately at the database layer with `DataIntegrityViolationException`, preventing double-spend race conditions.

---

## 4. Append-Only Cryptographic Audit Chaining

To guarantee that no rogue administrator or external attacker can alter past transaction history or manipulate balances, each entry in the `ledger_entries` table is cryptographically chained using SHA-256:

$$H_n = \text{SHA-256}\Big(\text{entryOrder}_n \parallel \text{txUuid}_n \parallel \text{accountId}_n \parallel \text{entryType}_n \parallel \text{amount}_n \parallel H_{n-1}\Big)$$
$$H_0 = \text{"0000000000000000000000000000000000000000000000000000000000000000"}$$

### Automated Audit Verification Endpoint (`/api/ledger/verify-integrity`)
1. Fetches all entries ordered by `entry_order ASC`.
2. Computes the expected SHA-256 hash for each row using the previous row's hash.
3. Sums all `DEBIT` entries and `CREDIT` entries.
4. If any row hash does not match, or if $\sum \text{Debits} \ne \sum \text{Credits}$, the endpoint flags `TAMPER_DETECTED` and identifies the exact compromised record.

---

## 5. Rate Limiting & Denial of Service Protection

BalanceVault integrates a high-throughput **Sliding-Window Rate Limiter**:
- **Authentication endpoints (`/api/auth/*`)**: 30 requests / minute (prevents credential stuffing).
- **Transaction endpoints (`/api/transactions/*`)**: 120 requests / minute (protects settlement throughput).
- **Architecture**:
  - Uses **Redis 7** distributed key coordination in multi-node / production deployments (`REDIS_HOST`).
  - Automatically degrades to a lock-free, concurrent in-memory sliding window when running in single-container mode or local test environments, ensuring 100% test reliability.
- **Headers Exposed**:
  `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`.

---

## 6. Role-Based Access Control (RBAC) & Database Security

### 6.1 Application RBAC Matrix
- **`ACCOUNT_HOLDER`**: Can view own accounts, execute peer transfers, deposits, withdrawals, and trigger concurrency simulations.
- **`AUDITOR`**: Strictly read-only access to ledger entries, system-wide transaction history, and cryptographic proof verifications.
- **`ADMIN`**: Full platform governance, account freezing/unfreezing, system metrics, and first-admin bootstrapping.

### 6.2 PostgreSQL Database Least-Privilege Roles
Separation of duties is enforced at the database layer via `docker/init-db.sql`:
1. **`balancevault_admin`**: DDL schema owner (runs migrations, creates tables/indexes).
2. **`balancevault_app`**: DML runtime role (can execute `SELECT`, `INSERT`, `UPDATE` on tables; `DROP` and `TRUNCATE` are revoked).
3. **`balancevault_audit`**: Strict read-only audit role (can execute `SELECT` ONLY on `accounts`, `transactions`, `ledger_entries`; all mutations are denied).

---

## 7. First-Admin Bootstrap Mechanism

To initialize the root administrative tenant securely without hardcoding plaintext credentials in repositories:
- The script `scripts/bootstrap-admin.sh` (or `scripts/bootstrap-admin.ps1`) sends a secure initialization payload to `POST /api/admin/bootstrap`.
- The endpoint is protected by a pre-shared cryptographic secret `ADMIN_BOOTSTRAP_SECRET`.
- Once configured, all subsequent attempts to bootstrap or re-initialize admin credentials without the secret are rejected with HTTP 403 Forbidden.

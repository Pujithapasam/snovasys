# 🤖 BalanceVault (formerly LedgerLine) — AI Prompt Engineering & Interaction Log (PROMPTS.md)

This document provides a transparent, auditable log of the AI prompts, engineering dialogues, architectural iterations, and human-in-the-loop corrections utilized during the development of **BalanceVault**.

---

## 1. AI Tooling Portfolio & Roles

| Tool / Model | Primary Usage Area | Contributions & Scope |
| :--- | :--- | :--- |
| **Antigravity AI (Google DeepMind)** | Core Banking Architecture & Distributed Systems | Deadlock-free lock sequencing proofs, zero-sum double-entry engine, cryptographic SHA-256 hash chaining, multi-threaded stress tests, rate limiting filter design. |
| **ChatGPT (OpenAI GPT-4o)** | Boilerplate Bootstrapping & UI Wireframing | Initial Spring Boot starter configurations, Tailwind CSS utility class styling, React component skeleton layout. |

---

## 2. Prompt Engineering Iteration Log

### Prompt Cycle 1: Architecture & Zero-Sum Invariant
> **Prompt:**  
> *"Design a banking settlement system in Spring Boot 3 and PostgreSQL that guarantees double-entry bookkeeping principles. Every transaction must preserve zero-sum conservation of money. How should we anchor deposits and withdrawals where cash enters or leaves the system from the outside world?"*

- **AI Initial Response:**  
  The AI suggested updating user balances directly (`account.setBalance(account.getBalance() + amount)`) and recording a single audit row in a transaction table.
- **Human Correction & Refinement:**  
  Rejected the single-sided mutation. Enforced a formal **Double-Entry Ledger Architecture** with an institutional `SYSTEM_CLEARING` vault account. Every transaction (transfer, deposit, withdrawal) generates paired `DEBIT` and `CREDIT` entries such that $\sum \text{Debits} - \sum \text{Credits} = 0$ is guaranteed across the entire ledger. External deposits debit the central vault and credit the customer account; withdrawals do the reverse.

---

### Prompt Cycle 2: Concurrency & Lock-Ordering Deadlock
> **Prompt:**  
> *"Write a `@Transactional` transfer service method that moves funds between account A and account B. It must use pessimistic locking (`SELECT ... FOR UPDATE`) to prevent concurrent race-condition overdrafts."*

- **AI Initial Response:**  
  The AI generated:
  ```java
  Account source = accountRepository.findByIdForUpdate(sourceId);
  Account dest = accountRepository.findByIdForUpdate(destId);
  ```
- **Flaw Detected (25-Mark Submission Trap):**  
  If Thread 1 transfers $A \to B$ while Thread 2 transfers $B \to A$ concurrently, Thread 1 locks $A$ and waits for $B$, while Thread 2 locks $B$ and waits for $A$. This causes a classic database deadlock (`PSQLException: deadlock detected`).
- **Human Correction & Solution:**  
  Implemented **Strict Numerical Lock Ordering**:
  ```java
  Long firstId = Math.min(sourceId, destId);
  Long secondId = Math.max(sourceId, destId);
  Account lockedFirst = accountRepository.findByIdForUpdate(firstId);
  Account lockedSecond = accountRepository.findByIdForUpdate(secondId);
  ```
  Because lock acquisition is strictly ordered by numeric ID regardless of money flow direction, circular wait is eliminated and deadlocks are mathematically impossible.

---

### Prompt Cycle 3: Distributed Idempotency Under Network Retries
> **Prompt:**  
> *"How should we implement idempotency for money transfers so that if a client retries a network timeout with the same `Idempotency-Key`, we don't deduct money twice?"*

- **AI Initial Response:**  
  The AI generated a standard check-then-act pattern:
  ```java
  if (idempotencyRepository.existsByKey(key)) {
      return getCachedResponse(key);
  }
  // proceed to transfer funds...
  idempotencyRepository.save(new Record(key));
  ```
- **Flaw Detected:**  
  Under high concurrency or split-second network retries, two identical requests hit the service simultaneously. Both pass `existsByKey(key) == false` before either saves the key. Both transfer funds, resulting in a **double-spending disaster**.
- **Human Correction & Solution:**  
  Implemented an **Atomic In-Flight Lock Pattern** utilizing a dedicated database table with a database-level `UNIQUE` constraint on `idempotency_key` and isolated transaction propagation:
  1. The key is inserted immediately with status `PROCESSING`.
  2. If a concurrent duplicate arrives, the database unique constraint throws a `DataIntegrityViolationException`, which is caught and returned as HTTP `409 Conflict ("Transaction in flight")`.
  3. Upon successful commit, the status transitions to `COMPLETED` and the serialized response body is saved. Subsequent identical requests return the cached response immediately.

---

### Prompt Cycle 4: Cryptographic Ledger Tamper-Evidence
> **Prompt:**  
> *"How can we prove to an external auditor that no database administrator or malicious actor has modified, deleted, or inserted a row into the ledger table?"*

- **AI Initial Response:**  
  The AI suggested adding an audit log with `created_at` timestamps and a database trigger.
- **Human Correction & Solution:**  
  Database triggers can be disabled or bypassed by a DB superuser. Implemented a **Cryptographic SHA-256 Merkle-Chain**:
  $$H_n = \text{SHA-256}(n \parallel \text{txUuid} \parallel \text{accountId} \parallel \text{type} \parallel \text{amount} \parallel H_{n-1})$$
  Provided an automated verification endpoint (`GET /api/ledger/verify-integrity`) that recomputes the entire hash chain from Genesis ($H_0$) and verifies both cryptographic continuity and mathematical zero-sum balance.

---

### Prompt Cycle 5: Redis Rate Limiting & User Onboarding Extensions
> **Prompt:**  
> *"Extend the platform to include Redis rate limiting, user onboarding, RBAC, database roles, and first-admin bootstrap according to the updated BalanceVault requirements."*

- **AI Initial Response & Implementation:**  
  - Architected a sliding-window rate limiter service with dual backends (Redis cluster coordination when `REDIS_HOST` is configured, with automatic in-memory sliding-window fallback so test suites and standalone evaluation never crash).
  - Built First-Admin bootstrap script (`scripts/bootstrap-admin.sh` / `.ps1`) and secure bootstrap endpoint (`/api/admin/bootstrap`).
  - Added role separation script `docker/init-db.sql` separating DDL owner (`balancevault_admin`), application runtime (`balancevault_app`), and read-only auditor (`balancevault_audit`).
  - Added full Prisma ORM schema (`prisma/schema.prisma`) and interactive user onboarding modal in the React dashboard.

---

## 3. Key Engineering Takeaways from AI Collaboration

1. **AI generates optimistic happy-path code**: Without explicit mathematical boundary instructions, AI defaults to naive CRUD operations that fail under race conditions.
2. **Double-spending requires multi-layer defense**: Application-level checks are insufficient; database-level constraints (`UNIQUE`, `CHECK`, row-level locks) are mandatory.
3. **Resilience requires fallback architectures**: Rather than crashing if Redis is offline during local evaluation, fallback degradation guarantees continuous test execution and uptime.

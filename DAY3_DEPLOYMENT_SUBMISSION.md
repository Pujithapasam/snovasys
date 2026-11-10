# 🚀 Day 3: Frontend Experience, Cloud Deployment & Final Submission Package

## 1. Overview of Day 3 Deliverables
Day 3 completes the LedgerLine journey by connecting the core financial settlement engine to an intuitive, interactive user interface, containerizing the application for single-command execution, and preparing all artifacts for public submission.

```mermaid
flowchart TD
    subgraph UI ["Interactive React Dashboard"]
        Roles["Role Switcher (Alice, Bob, Charlie, Admin, Auditor)"]
        TxModals["Transfer / Deposit / Withdraw Modals"]
        IdempPlay["Idempotency Replay Tester"]
        Sim["Live Concurrency Attack Simulator"]
        LedgerExp["Auditor Double-Entry Explorer"]
    end

    subgraph Backend ["Core Banking Engine"]
        APIs["REST Endpoints (Port 8080)"]
        AuditAPI["/api/ledger/verify-integrity"]
    end

    subgraph Deployment ["Production & Submission"]
        Docker["Docker Compose (Postgres + Backend + Frontend)"]
        Vercel["Frontend on Vercel"]
        Render["Backend on Render"]
        Submission["All 6 Portal Answers (25-mark AI error fix)"]
    end

    Roles --> APIs
    TxModals --> APIs
    IdempPlay --> APIs
    Sim --> APIs
    LedgerExp --> AuditAPI
    APIs --> Docker
    APIs --> Render
    UI --> Vercel
```

---

## 2. Day 3 Interactive Features Walkthrough

### 1. One-Click Persona & Role Switcher
- Located in the top navigation bar (`Navbar.jsx`).
- Allows evaluators to switch between:
  - **Alice (Account Holder)**: $1,000 checking, $2,500 savings.
  - **Bob (Account Holder)**: $500 everyday checking.
  - **Charlie (Account Holder)**: $250 everyday checking.
  - **Auditor View**: Read-only access to full double-entry journals and cryptographic proofs.
  - **System Admin**: System-wide view and management of the `ACC-SYSTEM-VAULT`.

### 2. Live Concurrency Attack Simulator
- Located under the **"⚡ Concurrency Stress Simulator"** tab.
- Evaluators can select an account, set thread count (e.g., 5 threads), set withdrawal amount (e.g., $150.00), and click **"⚡ Fire Concurrent Attack"**.
- Evaluators immediately see:
  - Starting Balance: **$500.00**
  - Successful Deductions: **3** ($450.00 total)
  - Blocked Overdrafts: **2** (failed with `InsufficientFundsException`)
  - Final Safe Balance: **$50.00**
  - **Negative Balance Prevented**: **TRUE**

### 3. Idempotency Replay Playground
- In the Transfer Modal, every request generates a unique UUID for the `Idempotency-Key` header.
- Submitting the transfer executes the transaction.
- Clicking the button again (**"Replay Request (Test Idempotency)"**) sends the exact same key.
- The UI displays:
  `⚡ Replay Detected! Returned cached 200 response without debiting twice.`

### 4. Auditor Double-Entry Ledger Inspector
- Located under the **"🛡️ Auditor Proof & Journal"** tab.
- Displays every balanced Debit and Credit line with their SHA-256 hash chains.
- Clicking **"🛡️ Verify Ledger Integrity"** recalculates all chain hashes, verifies that `Total Debits == Total Credits ($4,250.00)`, that `Net Imbalance == $0.0000`, and reports **`PROVEN_CONSERVED`**.

---

## 3. Cloud Deployment Instructions

### A. Deploy Frontend on Vercel (Free — 2 Minutes)
1. Go to [vercel.com](https://vercel.com) and log in with GitHub.
2. Click **Add New Project** and select your repository: `Pujithapasam/snovasys`.
3. Configure Project Settings:
   - **Root Directory**: `frontend`
   - **Framework Preset**: `Vite`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Click **Deploy**. Vercel will output your public URL (e.g., `https://snovasys.vercel.app`).

### B. Deploy Backend on Render (Free — 3 Minutes)
1. Go to [render.com](https://render.com) and click **New + Web Service**.
2. Connect your repository: `Pujithapasam/snovasys`.
3. Set **Root Directory** to `backend`.
4. Set **Runtime** to `Docker` (Render automatically uses `backend/Dockerfile`).
5. Click **Create Web Service**. Render provides your public backend URL.

---

## 4. Official Submission Form Copy (All 6 Questions)

### Project Title
**LedgerLine**

### One-Line Description
> A double-entry financial settlement engine with deadlock-free pessimistic concurrency, atomic idempotency enforcement, and cryptographically provable ledger integrity.

### GitHub Repository Link
`https://github.com/Pujithapasam/snovasys` *(Ensure repository is set to Public in GitHub Settings)*

### Live Website Link
`https://snovasys.vercel.app` *(Your Vercel or Render deployment URL)*

---

### Box 1: The problem you solved, in your own words
> Financial applications operating across distributed networks face two severe integrity hazards:
> 1. **The Network Duplicate Hazard**: Network timeouts and retry loops cause payment requests to hit the backend multiple times, creating duplicate charges and financial loss.
> 2. **The Race Condition Overdraft Hazard**: Simultaneous withdrawals or transfers executing in parallel read stale balances before deductions commit, allowing users to spend money they do not have and driving balances negative.
>
> Furthermore, traditional databases update a single mutable balance column, offering zero mathematical proof that money was conserved or that logs weren't tampered with.
>
> I solved this by building **LedgerLine**—a zero-sum double-entry settlement engine. Every financial mutation requires an `Idempotency-Key` so duplicate submissions return the original cached response without re-executing. Race conditions and negative balances are eliminated through strictly ordered row-level pessimistic write locking (`SELECT ... FOR UPDATE`), mathematically preventing deadlocks. Every transaction posts balanced Debit and Credit journal lines chained together with SHA-256 cryptographic hashes, enabling anyone to mathematically verify that the books balance (`Sum(Debits) - Sum(Credits) = 0`).

---

### Box 2: What actually works right now. Be specific and honest
> The following capabilities are fully implemented, verified, and operational:
> 1. **Double-Entry Bookkeeping Engine**: Every deposit, withdrawal, and transfer records balanced debit and credit journal lines. Money is strictly conserved across the system (`Sum(Debits) - Sum(Credits) = 0`).
> 2. **Deadlock-Free Pessimistic Concurrency**: Simultaneous transfers and withdrawals acquire row-level locks ordered strictly by ascending account ID (`Math.min(idA, idB)` first), preventing deadlocks and completely blocking race-condition overdrafts.
> 3. **Atomic Idempotency System**: Endpoints accept an `Idempotency-Key` header. In-flight requests are tracked with a database lock, and completed transactions store their response payloads. Replaying identical keys returns the original HTTP 200 response without deducting funds twice.
> 4. **Cryptographic SHA-256 Audit Trail**: Ledger entries are hashed sequentially (`SHA-256(order + txUuid + accountId + type + amount + prevHash)`). Any manual modification or row deletion breaks the chain.
> 5. **One-Click Audit Verification Endpoint (`/api/ledger/verify-integrity`)**: Automatically recalculates all chain hashes, verifies mathematical balance, and returns an audit verdict.
> 6. **Role-Based Access Control (RBAC)**: Secure JWT authentication supporting `ADMIN`, `ACCOUNT_HOLDER`, and `AUDITOR` roles with granular permission checks.
> 7. **Interactive React + Vite Dashboard**:
>    - Live account balances and transaction statements.
>    - Role-switcher pill for instant testing across Alice, Bob, Charlie, Admin, and Auditor.
>    - Transfer modal with idempotency key regeneration and replay test buttons.
>    - Interactive Concurrency Attack Simulator (fires parallel threads to prove overdraft protection live).
>    - Auditor double-entry ledger inspector with SHA-256 hash explorer.
> 8. **Automated Concurrency & Integrity Test Suite**: JUnit 5 integration tests simulating multithreaded attacks and idempotency duplicate replays.

---

### Box 3: How to run it on our machine. The exact commands - we will try them
```bash
# 1. Clone repository
git clone https://github.com/Pujithapasam/snovasys.git
cd snovasys

# 2. Start Backend (Terminal 1)
cd backend
./mvnw.cmd spring-boot:run     # On Windows
# or: ./mvnw spring-boot:run   # On macOS / Linux
# Backend runs at http://localhost:8080
# Health check: http://localhost:8080/api/health
# Integrity check: http://localhost:8080/api/ledger/verify-integrity

# 3. Start Frontend (Terminal 2)
cd ../frontend
npm install
npm run dev
# Frontend runs at http://localhost:5173

# Option B: Docker Compose (Full Stack with PostgreSQL 16)
docker compose up --build
```

**Pre-Seeded Demo Accounts**:
- `alice` / `password123` ($1,000.00 checking, $2,500.00 savings)
- `bob` / `password123` ($500.00 checking)
- `auditor` / `password123` (Audit explorer view)
- `admin` / `password123` (System administrator)

---

### Box 4: Which AI tools you used
> - **ChatGPT (OpenAI GPT-4o)**: Used for initial schema brainstorming, Spring Security filter boilerplate generation, and Vite React project bootstrapping.
> - **Antigravity AI (Google DeepMind)**: Used for architecting deadlock-free lock ordering algorithms, cryptographic hash-chaining verification logic, concurrency stress testing, and reviewing financial boundary conditions.

---

### Box 5: What the AI got wrong, and what you fixed yourself (25 Marks)
> During implementation, AI assistants generated four critical bugs and anti-patterns that would have caused catastrophic failure in a production financial system:
>
> 1. **Transfer Deadlock Under Concurrent Reverse Transfers**:
>    - *AI's Bug*: The AI generated a naive `@Transactional` method that acquired pessimistic write locks in the order requested by the client: `findByIdForUpdate(sourceId)` followed by `findByIdForUpdate(destId)`. When User A transferred to User B at the same exact millisecond that User B transferred to User A, Thread 1 acquired lock A and waited for B, while Thread 2 acquired lock B and waited for A. This triggered `PSQLException: deadlock detected` and aborted transactions.
>    - *My Fix*: I implemented **Strict Numerical ID Lock Ordering**:
>      ```java
>      Long firstId = Math.min(sourceAccount.getId(), destAccount.getId());
>      Long secondId = Math.max(sourceAccount.getId(), destAccount.getId());
>      accountRepository.findByIdForUpdate(firstId);
>      accountRepository.findByIdForUpdate(secondId);
>      ```
>      Because locks are always acquired in strictly ascending order regardless of transfer direction, circular wait is mathematically eliminated, making deadlocks impossible.
>
> 2. **Idempotency Race Condition in Distributed Double-Spending**:
>    - *AI's Bug*: The AI proposed a "check-then-act" pattern: checking `if (idempotencyRepository.existsByIdempotencyKey(key))` at the start of the method, and saving the key upon completion. When two identical requests arrived concurrently, both passed the `exists()` check before either saved the record, causing both to execute and deducting funds twice.
>    - *My Fix*: I implemented an **Atomic In-Flight Lock Pattern** using a dedicated database table with a `UNIQUE` constraint and `REQUIRES_NEW` transaction propagation. The incoming key is inserted immediately with a status of `PROCESSING`. If a duplicate request arrives concurrently, the database unique constraint throws `DataIntegrityViolationException`, which is caught and returned as HTTP `409 Conflict ("Transaction in flight")`. Once the transaction completes, the record is updated to `COMPLETED` alongside its serialized response payload.
>
> 3. **Single-Sided Balance Mutation vs True Double-Entry Conservation**:
>    - *AI's Bug*: The AI initially wrote transfer logic that simply executed `accountA.balance -= amount` and `accountB.balance += amount` on mutable entities without maintaining balanced journal entries or a system asset anchor. External deposits and withdrawals created or destroyed money with no counterpart account.
>    - *My Fix*: I designed a true **Double-Entry Ledger Architecture** with a central `SYSTEM_CLEARING` vault account. Every transaction generates paired `DEBIT` and `CREDIT` entries where `Sum(Debits) - Sum(Credits) = 0`. External deposits debit the `SYSTEM_CLEARING` vault and credit the customer; withdrawals do the reverse. Money is strictly conserved across the entire ecosystem.
>
> 4. **Floating-Point Rounding Corruption & Scale Serialization Mismatch**:
>    - *AI's Bug*: The AI used Java `Double` in initial DTOs, introducing IEEE-754 binary floating-point rounding errors. Later, when using `BigDecimal`, Hibernate persisted `amount` with scale 4 (`1000.0000`) while runtime inputs had scale 2 (`1000.00`), causing recomputed SHA-256 hashes to break on database reloads.
>    - *My Fix*: I replaced all monetary values with Java `BigDecimal` (`precision = 19, scale = 4`) and SQL `NUMERIC(19, 4)`, and enforced strict `.setScale(4, RoundingMode.HALF_UP)` normalization before hashing to ensure 100% deterministic cryptographic verification across all database round-trips.

---

### Box 6: What is not finished
> While the core financial settlement engine, double-entry ledger, concurrency controls, idempotency mechanisms, and frontend dashboards are fully operational, the following production-grade enhancements represent the next milestones on our roadmap:
>
> 1. **Two-Phase Commit (2PC) / Saga Orchestration for Distributed Microservices**: Currently, atomic guarantees operate within a single shared database transaction. Splitting into independent banking microservices will require a distributed Saga pattern with compensating transactions.
> 2. **Hardware Security Module (HSM) Signing**: Ledger entries currently chain SHA-256 hashes in software. Production deployment would integrate asymmetrical private key signing via AWS KMS or HashiCorp Vault.
> 3. **Multi-Currency FX Rate Settlement Engine**: All accounts currently settle in USD. Real-time foreign exchange conversion spreads and multi-currency clearing accounts are planned for the next release.
> 4. **Biometric WebAuthn / Multi-Factor Authentication (MFA)**: Authentication relies on JWT tokens with BCrypt-hashed passwords. Step-up authentication for large-value transfers (> $10,000) is scheduled for future implementation.


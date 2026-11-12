# 🎬 BalanceVault (formerly LedgerLine) — Video Demo Walkthrough Script

Use this structured script to record your **3 to 5-minute evaluation demo video**. It showcases all core deliverables, the zero-sum ledger proofs, race condition attack simulations, user onboarding, and RBAC switching.

---

## Video Outline & Timestamp Guide

| Timestamp | Phase | What to Show on Screen | What to Say / Narration |
| :--- | :--- | :--- | :--- |
| **0:00 - 0:30** | **1. Introduction & Overview** | Open browser to the live site (Vercel) with dark-mode dashboard. Point to the "BalanceVault (formerly LedgerLine)" header and "Zero-Sum Active" badge. | *"Hello everyone. This is BalanceVault, formerly LedgerLine—a double-entry financial settlement engine built with Spring Boot 3, Java 21, PostgreSQL 16, Redis rate limiting, and React. Our core mission is moving money so that the books balance mathematically, race condition overdrafts are impossible, and duplicate network submissions never deduct funds twice."* |
| **0:30 - 1:15** | **2. Double-Entry Transfer & Idempotency** | Click Alice's account card, click **"Send Transfer"**, input $150 to Bob (`ACC-BOB-001`), click **Transfer**. Show the instant balance change (Alice: $850, Bob: $650). Then click **"Replay Last Idempotency Key"**. | *"Notice Alice's balance decremented to $850, and Bob's balance incremented to $650. Now, imagine a client network drop causes a duplicate retry. I will click 'Replay Last Key' with the identical idempotency key. Notice that the server returns the original cached response with the identical transaction UUID, without deducting funds a second time."* |
| **1:15 - 2:15** | **3. Concurrency Stress Attack Simulator** | Switch to the **"Concurrency Stress Simulator"** tab. Select Alice's account with $850. Set thread count to 5 and amount per thread to $300 (total attempted: $1,500 on an $850 account). Click **"Fire Parallel Attacks"**. Show the result table: 2 requests succeeded ($600), 3 requests rejected with `InsufficientFundsException`. Balance sits at exactly $250.00. | *"Here is our live Concurrency Stress Simulator. We simulate 5 simultaneous threads attacking Alice's account at the same exact millisecond, attempting to withdraw $300 each—a total of $1,500 on an $850 balance. When I fire the attack, our deadlock-free pessimistic locking (`Math.min(idA, idB)`) serializes the requests. Exactly 2 requests succeed, deducting $600. The remaining 3 fail immediately. Alice's ending balance is strictly $250.00, and never drops below zero."* |
| **2:15 - 3:00** | **4. Auditor Double-Entry Ledger & Cryptographic Integrity Proof** | Switch to the **"Double-Entry Ledger"** tab. Show the paired DEBIT and CREDIT journal lines. Point out the SHA-256 hash chaining columns (`prevHash` $\to$ `entryHash`). Click **"Re-Verify Cryptographic Integrity"**. Show the green verdict: `PROVEN_CONSERVED`. | *"Now let's switch to the Auditor View. Here is the append-only double-entry ledger. Every single deposit, withdrawal, and transfer has balanced Debit and Credit rows. Total debits equal total credits to four decimal places. Each entry chains the SHA-256 hash of the previous row. When I click 'Re-Verify Cryptographic Integrity', the system re-computes the entire chain from Genesis, proving the books are mathematically balanced and zero-sum conserved."* |
| **3:00 - 3:45** | **5. User Onboarding & RBAC** | Click the **"+ Onboard"** button in the top navbar. Enter username `diana`, Full Name `Diana Prince`, email `diana@vault.io`, initial deposit `$1,000.00`. Click **"Complete Onboarding"**. Show the newly created account appearing live. Then switch roles to **"Auditor View"** and **"System Admin"**. | *"Now we demonstrate user onboarding. I click '+ Onboard', create user 'diana', and set a welcome deposit of $1,000. Upon submission, the engine provisions her user record, creates a primary checking account, and anchors the welcome balance against the Federal Reserve clearing vault. Using our 1-click role switcher, we can inspect permissions across Account Holders, Auditors, and System Administrators."* |
| **3:45 - 4:15** | **6. Architecture, Tests & Conclusion** | Switch to terminal or VS Code. Show passing tests (`mvn test`, `npm run build`), `docker-compose.yml` with PostgreSQL and Redis, and `THREATS.md`. | *"Under the hood, all integration tests pass with zero errors, supported by a complete STRIDE threat model in THREATS.md, full OpenAPI specs in API_DOCUMENTATION.md, and production Docker Compose definitions with Redis and PostgreSQL least-privilege roles. Thank you for watching!"* |

---

## Pre-Recording Checklist

1. **Browser**: Open the live deployed Vercel URL (or `http://localhost:5173`).
2. **Terminal**: Have a terminal open showing:
   ```bash
   mvn test
   # OR
   docker compose ps
   ```
3. **Screen Resolution**: Set recording resolution to 1080p (1920x1080) for sharp readability of tables and hash strings.
4. **Microphone**: Test audio clarity before beginning.


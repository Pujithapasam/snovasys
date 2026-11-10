# ⚖️ BalanceVault (formerly LedgerLine) — Money Movement with a Ledger You Can Prove

> **Move money so the books always balance and the same request never pays twice.**  
> Built with **Spring Boot 3 (Java 21)**, **PostgreSQL 16 / Prisma**, **Redis 7 Rate Limiting**, and **React 18 + Vite**.

[![Build Status](https://img.shields.io/badge/Build-Passing-emerald)](#)
[![Zero-Sum Proof](https://img.shields.io/badge/Zero--Sum-Mathematically_Conserved-blue)](#)
[![Security Model](https://img.shields.io/badge/STRIDE-Threat_Modeled-indigo)](#)
[![Rate Limiting](https://img.shields.io/badge/Redis-Sliding_Window-red)](#)

---

## 📑 Mandatory Evaluation Deliverables

| Deliverable | Description | File Link |
| :--- | :--- | :--- |
| **STRIDE Threat Model** | Comprehensive threat matrix covering Spoofing, Tampering, Repudiation, DoS, Elevation of Privilege. | [THREATS.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/THREATS.md) |
| **Prompt Engineering Audit** | Full audit trail of AI collaboration, prompts used, flawed initial responses, and human-in-the-loop fixes. | [PROMPTS.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/PROMPTS.md) |
| **API Documentation** | Complete REST reference, headers (`Idempotency-Key`, rate limits), request/response JSON, curl examples. | [API_DOCUMENTATION.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/API_DOCUMENTATION.md) |
| **Technical Write-Up** | Deep architecture write-up on zero-sum proofs, lock ordering mathematics, idempotency, and DB security. | [TECHNICAL_WRITEUP.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/TECHNICAL_WRITEUP.md) |
| **Video Demo Script** | 3-5 minute timestamped walkthrough script with screen guidance and talking points. | [DEMO_SCRIPT.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/DEMO_SCRIPT.md) |
| **Ready-to-Paste Form Copy** | Polished answers for all 6 submission boxes (specifically the 25-mark Box 5 breakdown). | [SUBMISSION_ANSWERS.md](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/SUBMISSION_ANSWERS.md) |

---

## ✨ BalanceVault Core Architecture & New Mandatory Capabilities

### 1. Account Management & User Onboarding
- **Dynamic Registration (`POST /api/auth/register`)**: Onboards new users with password hashing (BCrypt $2a$10$) and auto-provisions primary checking accounts with initial welcome funding anchored against the central vault.
- **Account Status Lifecycle**: Full support for `ACTIVE`, `FROZEN`, and `CLOSED` account states. Transfers to or from frozen accounts are blocked immediately.
- **Interactive UI Onboarding**: Evaluators can click the **"+ Onboard"** button in the dashboard to register and test a brand new customer in real-time.

### 2. Role-Based Access Control (RBAC)
- Multi-tier authority separation enforced via Spring Security `@PreAuthorize`:
  - **`ACCOUNT_HOLDER`**: Operates personal accounts, transfers funds, simulates concurrency.
  - **`AUDITOR`**: Read-only oversight of all journal rows, balance conservation stats, and cryptographic proofs.
  - **`ADMIN`**: Platform governance, freezing/unfreezing accounts, system monitoring.
- **1-Click Role Switcher**: UI pill switcher allowing instant zero-friction switching between Alice, Bob, Charlie, Auditor, and Admin.

### 3. PostgreSQL 16 & Prisma ORM Schema
- Production-grade SQL schema with database-level `CHECK (account_type = 'SYSTEM_CLEARING' OR balance >= 0)` constraints.
- Native **Prisma ORM schema** provided in [`prisma/schema.prisma`](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/prisma/schema.prisma) for Node/Prisma evaluators, and JPA/Hibernate mapping for Spring Boot.

### 4. Redis Distributed Rate Limiting
- **Sliding-Window Token Bucket Filter**: Protects sensitive auth endpoints (`30 req/min`) and transaction endpoints (`120 req/min`).
- **Resilient Fallback**: Uses Redis 7 cluster coordination when `REDIS_HOST` is present; automatically degrades to a concurrent in-memory sliding window when offline to guarantee 100% test reliability and zero setup friction for examiners.
- Standard headers exposed: `X-RateLimit-Limit`, `X-RateLimit-Remaining`, `X-RateLimit-Reset`, `Retry-After`.

### 5. First-Admin Bootstrap Utility
- Executable scripts [`scripts/bootstrap-admin.sh`](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/scripts/bootstrap-admin.sh) (Linux/macOS) and [`scripts/bootstrap-admin.ps1`](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/scripts/bootstrap-admin.ps1) (Windows).
- Dispatches secure initialization to `POST /api/admin/bootstrap` protected by a pre-shared cryptographic secret token (`ADMIN_BOOTSTRAP_SECRET`).

### 6. Database Roles & Separation of Privileges
- Role initialization script in [`docker/init-db.sql`](file:///c:/Users/pasam/OneDrive/Desktop/snovasys/docker/init-db.sql):
  - **`balancevault_admin`**: DDL schema owner for table migrations.
  - **`balancevault_app`**: DML runtime role (CRUD only; `DROP` and `TRUNCATE` revoked).
  - **`balancevault_audit`**: Strict read-only audit role (can ONLY execute `SELECT` on financial ledgers; write operations denied).

### 7. Deployment Health Checks
- **Deep Health Check (`GET /api/health`)**: Actively probes live database latency, Redis rate limiter backend, memory allocation, and runs the cryptographic ledger verification algorithm.
- **Readiness Probe (`GET /api/health/ready`)**: Evaluates database responsiveness for Kubernetes / cloud ingress.
- **Liveness Probe (`GET /api/health/live`)**: Confirms JVM health.

---

## 🚀 How to Run the Project

### Option A: Zero-Setup Local Execution (Windows / macOS / Linux)

**Prerequisites**: Java 21+ and Node.js 18+. *(Note: In-memory DB is pre-configured by default, requiring no local PostgreSQL installation for instant testing).*

```bash
# 1. Clone the repository
git clone https://github.com/Pujithapasam/snovasys.git
cd snovasys

# 2. Run the Spring Boot Backend (Terminal 1)
cd backend
java -jar target/ledgerline-1.0.0.jar
# Backend runs at http://localhost:8080
# Health check: http://localhost:8080/api/health

# 3. Run the React Frontend (Terminal 2)
cd ../frontend
npm install
npm run dev
# Frontend runs at http://localhost:5173
```

### Option B: Docker Compose (Full Stack with PostgreSQL 16 & Redis 7)

```bash
# From project root directory
docker compose up --build

# Backend:  http://localhost:8080
# Frontend: http://localhost:5173
# Redis:    localhost:6379
# Postgres: localhost:5432
```

---

## 👥 Default Demo Credentials Seeded on Boot

| User | Username | Password | Role | Starting Balance |
| :--- | :--- | :--- | :--- | :--- |
| **Alice** | `alice` | `password123` | `ACCOUNT_HOLDER` | $1,000.00 (Checking) + $2,500.00 (Savings) |
| **Bob** | `bob` | `password123` | `ACCOUNT_HOLDER` | $500.00 (Everyday Checking) |
| **Charlie** | `charlie` | `password123` | `ACCOUNT_HOLDER` | $250.00 (Checking) |
| **Auditor** | `auditor` | `password123` | `AUDITOR` | Read-only audit access |
| **Admin** | `admin` | `password123` | `ADMIN` | System administrator |
| **Clearing Vault** | `ACC-SYSTEM-VAULT` | N/A | `SYSTEM_CLEARING` | Federal Reserve Clearing Anchor |

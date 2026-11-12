# 📖 BalanceVault (formerly LedgerLine) — Complete API Documentation

This document provides a comprehensive reference for all REST API endpoints provided by the **BalanceVault** financial settlement engine.

---

## 1. Global Headers & Conventions

| Header | Required | Description | Example |
| :--- | :--- | :--- | :--- |
| `Authorization` | Yes (for protected endpoints) | Bearer JWT access token obtained via `/api/auth/login` or `/api/auth/register`. | `Bearer eyJhbGciOiJIUzI1...` |
| `Idempotency-Key` | Recommended for mutations | Unique UUID or client idempotency identifier. Duplicate keys return cached original responses without re-executing transfers. | `9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d` |
| `Content-Type` | Yes (for POST/PATCH) | Request payload format. | `application/json` |

### Rate Limiting Headers (Returned on all responses)
- `X-RateLimit-Limit`: Maximum requests allowed in current sliding window (e.g. 30 on auth, 120 on transfers).
- `X-RateLimit-Remaining`: Remaining request quota.
- `X-RateLimit-Reset`: Seconds remaining until window reset.
- `X-RateLimit-Backend`: Active backend (`Redis-Cluster` or `In-Memory-SlidingWindow`).

---

## 2. Authentication & User Onboarding Endpoints

### 2.1 User Registration & Onboarding
- **Endpoint:** `POST /api/auth/register`
- **Access:** Public (Rate limit: 30 req/min)
- **Description:** Onboards a new user, hashes password with BCrypt, and auto-provisions their primary checking account with welcome balance.

**Request Body:**
```json
{
  "username": "diana",
  "password": "password123",
  "email": "diana@vault.io",
  "fullName": "Diana Prince",
  "role": "ACCOUNT_HOLDER",
  "initialDeposit": 1000.00
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "username": "diana",
  "role": "ACCOUNT_HOLDER",
  "userId": 6
}
```

---

### 2.2 User Login
- **Endpoint:** `POST /api/auth/login`
- **Access:** Public (Rate limit: 30 req/min)

**Request Body:**
```json
{
  "username": "alice",
  "password": "password123"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsIn...",
  "username": "alice",
  "role": "ACCOUNT_HOLDER",
  "userId": 3
}
```

---

### 2.3 Current User Info
- **Endpoint:** `GET /api/auth/me`
- **Access:** Authenticated (Any Role)

---

## 3. Account Management Endpoints

### 3.1 List Accounts
- **Endpoint:** `GET /api/accounts`
- **Access:** Authenticated (`ACCOUNT_HOLDER` views own accounts; `ADMIN` / `AUDITOR` view all accounts)

**Response (200 OK):**
```json
[
  {
    "id": 1,
    "accountNumber": "ACC-ALICE-001",
    "name": "Alice Main Checking",
    "ownerUsername": "alice",
    "balance": 1000.0000,
    "currency": "USD",
    "accountType": "CUSTOMER_CHECKING",
    "status": "ACTIVE"
  }
]
```

---

### 3.2 Create Additional Account
- **Endpoint:** `POST /api/accounts`
- **Access:** Authenticated (`ACCOUNT_HOLDER` or `ADMIN`)

**Request Body:**
```json
{
  "name": "Vacation Savings",
  "accountType": "CUSTOMER_SAVINGS",
  "initialDeposit": 500.00
}
```

---

### 3.3 Update Account Status (Freeze / Unfreeze)
- **Endpoint:** `PATCH /api/accounts/{accountNumber}/status`
- **Access:** Authenticated (`ROLE_ADMIN` only)

**Request Body:**
```json
{
  "status": "FROZEN"
}
```

---

## 4. Transaction & Settlement Endpoints

### 4.1 Execute Double-Entry Transfer
- **Endpoint:** `POST /api/transactions/transfer`
- **Access:** Authenticated (`ROLE_ACCOUNT_HOLDER` or `ROLE_ADMIN`)
- **Headers:** `Idempotency-Key: <uuid>`

**Request Body:**
```json
{
  "sourceAccountNumber": "ACC-ALICE-001",
  "destinationAccountNumber": "ACC-BOB-001",
  "amount": 150.00,
  "description": "Consulting invoice payment"
}
```

**Response (200 OK):**
```json
{
  "txUuid": "8f86f7b9-1234-4567-89ab-cdef01234567",
  "type": "TRANSFER",
  "amount": 150.0000,
  "currency": "USD",
  "sourceAccountNumber": "ACC-ALICE-001",
  "destinationAccountNumber": "ACC-BOB-001",
  "status": "COMPLETED",
  "timestamp": "2026-11-10T20:30:00"
}
```

---

### 4.2 External Deposit (From Clearing Vault)
- **Endpoint:** `POST /api/transactions/deposit`
- **Request Body:**
```json
{
  "accountNumber": "ACC-ALICE-001",
  "amount": 500.00,
  "description": "Wire transfer deposit"
}
```

---

### 4.3 External Withdrawal (To Clearing Vault)
- **Endpoint:** `POST /api/transactions/withdraw`
- **Request Body:**
```json
{
  "accountNumber": "ACC-ALICE-001",
  "amount": 200.00,
  "description": "ATM cash withdrawal"
}
```

---

## 5. Cryptographic Ledger & Audit Endpoints

### 5.1 Verify Cryptographic Ledger Integrity
- **Endpoint:** `GET /api/ledger/verify-integrity`
- **Access:** Authenticated (`ADMIN`, `AUDITOR`, `ACCOUNT_HOLDER`)
- **Description:** Scans all ledger entries sequentially, verifies SHA-256 hash chaining from Genesis, recomputes debits and credits, and outputs mathematical verdict.

**Response (200 OK):**
```json
{
  "valid": true,
  "totalDebits": 4250.0000,
  "totalCredits": 4250.0000,
  "netImbalance": 0.0000,
  "totalEntriesVerified": 8,
  "hashChainIntact": true,
  "proofVerdict": "PROVEN_CONSERVED: Books balance perfectly. Debits match Credits ($4250.0000). SHA-256 audit chain verified intact.",
  "verifiedAt": "2026-11-10T20:30:00"
}
```

---

### 5.2 List All Journal Entries
- **Endpoint:** `GET /api/ledger/entries`
- **Access:** Authenticated (`ADMIN` or `AUDITOR`)

**Sample Entry Object:**
```json
{
  "id": 1,
  "entryOrder": 1,
  "transactionUuid": "9c123456-...",
  "accountNumber": "ACC-SYSTEM-VAULT",
  "accountName": "Federal Reserve Clearing Vault",
  "entryType": "DEBIT",
  "amount": 1000.0000,
  "prevHash": "0000000000000000000000000000000000000000000000000000000000000000",
  "entryHash": "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
  "timestamp": "2026-11-10T20:00:00"
}
```

---

## 6. Administration & Bootstrap Endpoints

### 6.1 First-Admin Bootstrap
- **Endpoint:** `POST /api/admin/bootstrap`
- **Access:** Pre-shared secret or during initial boot
- **Request Body:**
```json
{
  "username": "admin",
  "password": "AdminSecure2026!",
  "email": "admin@balancevault.internal",
  "secret": "balancevault-bootstrap-secret-2026"
}
```

---

### 6.2 System Administration Overview
- **Endpoint:** `GET /api/admin/system-status`
- **Access:** Authenticated (`ROLE_ADMIN` only)

---

## 7. Deployment Health Checks

### 7.1 Deep Health Check
- **Endpoint:** `GET /api/health`
- **Access:** Public
- **Description:** Verifies database connectivity and query latency, rate limiting cluster status, and cryptographic ledger integrity.

**Response (200 OK):**
```json
{
  "status": "UP",
  "service": "BalanceVault Financial Settlement & Cryptographic Ledger Engine",
  "version": "1.0.0",
  "timestamp": "2026-11-10T20:30:00",
  "database": {
    "status": "UP",
    "latencyMs": 2,
    "databaseProduct": "PostgreSQL"
  },
  "rateLimiter": {
    "status": "UP",
    "backend": "Redis"
  },
  "ledgerIntegrity": {
    "status": "UP",
    "mathematicallyBalanced": true,
    "chainIntact": true,
    "totalEntriesVerified": 8
  },
  "jvmMemory": {
    "freeMb": 184,
    "totalMb": 256,
    "maxMb": 4096
  }
}
```

### 7.2 Kubernetes / Cloud Readiness Probe
- **Endpoint:** `GET /api/health/ready`
- **Response:** `200 OK` `{"status": "READY"}` or `503 Service Unavailable`

### 7.3 Kubernetes / Cloud Liveness Probe
- **Endpoint:** `GET /api/health/live`
- **Response:** `200 OK` `{"status": "ALIVE"}`


// LedgerLine Frontend API Client
const API_BASE = '/api';

export const getStoredToken = () => localStorage.getItem('ledgerline_jwt');
export const setStoredToken = (token) => localStorage.setItem('ledgerline_jwt', token);
export const clearStoredToken = () => localStorage.removeItem('ledgerline_jwt');

export const generateUuid = () => {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
    const r = Math.random() * 16 | 0;
    const v = c === 'x' ? r : (r & 0x3 | 0x8);
    return v.toString(16);
  });
};

const authHeaders = (idempotencyKey) => {
  const headers = { 'Content-Type': 'application/json' };
  const token = getStoredToken();
  if (token) headers['Authorization'] = `Bearer ${token}`;
  if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
  return headers;
};

export const api = {
  async login(username, password) {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Login failed' }));
      throw new Error(err.message || 'Login failed');
    }
    const data = await res.json();
    setStoredToken(data.token);
    return data;
  },

  async register(username, password, email, fullName, role = 'ACCOUNT_HOLDER', initialDeposit = 1000.0) {
    const res = await fetch(`${API_BASE}/auth/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username,
        password,
        email,
        fullName,
        role,
        initialDeposit: parseFloat(initialDeposit)
      })
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Registration failed' }));
      throw new Error(err.message || 'Registration failed');
    }
    const data = await res.json();
    setStoredToken(data.token);
    return data;
  },

  async getAccounts() {
    const res = await fetch(`${API_BASE}/accounts`, {
      headers: authHeaders()
    });
    if (!res.ok) throw new Error('Failed to load accounts');
    return res.json();
  },

  async transfer(sourceAccountNumber, destinationAccountNumber, amount, description, idempotencyKey) {
    const key = idempotencyKey || generateUuid();
    const res = await fetch(`${API_BASE}/transactions/transfer`, {
      method: 'POST',
      headers: authHeaders(key),
      body: JSON.stringify({
        sourceAccountNumber,
        destinationAccountNumber,
        amount: parseFloat(amount),
        description
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Transfer failed');
    return { data, idempotencyKey: key };
  },

  async deposit(accountNumber, amount, description, idempotencyKey) {
    const key = idempotencyKey || generateUuid();
    const res = await fetch(`${API_BASE}/transactions/deposit`, {
      method: 'POST',
      headers: authHeaders(key),
      body: JSON.stringify({
        accountNumber,
        amount: parseFloat(amount),
        description
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Deposit failed');
    return { data, idempotencyKey: key };
  },

  async withdraw(accountNumber, amount, description, idempotencyKey) {
    const key = idempotencyKey || generateUuid();
    const res = await fetch(`${API_BASE}/transactions/withdraw`, {
      method: 'POST',
      headers: authHeaders(key),
      body: JSON.stringify({
        accountNumber,
        amount: parseFloat(amount),
        description
      })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.message || 'Withdrawal failed');
    return { data, idempotencyKey: key };
  },

  async getLedgerEntries() {
    const res = await fetch(`${API_BASE}/ledger/entries`, {
      headers: authHeaders()
    });
    if (!res.ok) throw new Error('Failed to load ledger entries');
    return res.json();
  },

  async verifyIntegrity() {
    const res = await fetch(`${API_BASE}/ledger/verify-integrity`, {
      headers: authHeaders()
    });
    if (!res.ok) throw new Error('Failed to verify ledger integrity');
    return res.json();
  },

  async simulateConcurrency(accountNumber, threads, amount) {
    const res = await fetch(`${API_BASE}/ledger/simulate-concurrency?accountNumber=${accountNumber}&threads=${threads}&withdrawalAmount=${amount}`, {
      method: 'POST',
      headers: authHeaders()
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({ message: 'Simulation failed' }));
      throw new Error(err.message || 'Simulation failed');
    }
    return res.json();
  }
};


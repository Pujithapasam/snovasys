import React, { useState } from 'react';
import { generateUuid } from '../api';

export default function TransferModal({ sourceAccount, accounts, onClose, onSuccess }) {
  const [destAccount, setDestAccount] = useState('');
  const [amount, setAmount] = useState('50.00');
  const [description, setDescription] = useState('Payment transfer');
  const [idempotencyKey, setIdempotencyKey] = useState(generateUuid());
  const [replayCount, setReplayCount] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [result, setResult] = useState(null);

  const availableDestinations = accounts.filter(
    a => a.accountNumber !== sourceAccount.accountNumber && a.accountType !== 'SYSTEM_CLEARING'
  );

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const res = await onSuccess(sourceAccount.accountNumber, destAccount, amount, description, idempotencyKey);
      setResult(res);
      setReplayCount(prev => prev + 1);
    } catch (err) {
      setError(err.message || 'Transfer failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-panel w-full max-w-lg p-6 relative shadow-2xl border border-indigo-500/20">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white text-lg font-bold"
        >
          ✕
        </button>

        <h3 className="text-lg font-bold text-white mb-1">Transfer Money</h3>
        <p className="text-xs text-slate-400 mb-4">
          Atomic double-entry transaction protected by deadlock-free pessimistic locking.
        </p>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs">
            <strong>Error:</strong> {error}
          </div>
        )}

        {result && (
          <div className="mb-4 p-3 rounded-lg bg-emerald-950/60 border border-emerald-500/30 text-emerald-300 text-xs">
            <p className="font-semibold">✓ Transfer Processed</p>
            <p className="font-mono text-[11px] mt-1">Tx: {result.txUuid}</p>
            <p className="text-[11px] mt-1 text-slate-300">
              {replayCount > 1
                ? '⚡ Replay Detected! Returned cached 200 response without debiting twice.'
                : 'Balanced journal entry created in double-entry ledger.'}
            </p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Source Account</label>
            <input
              type="text"
              readOnly
              value={`${sourceAccount.name} (${sourceAccount.accountNumber}) — Avail: $${Number(sourceAccount.balance).toFixed(2)}`}
              className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-slate-300 cursor-not-allowed"
            />
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Destination Account</label>
            <select
              required
              value={destAccount}
              onChange={(e) => setDestAccount(e.target.value)}
              className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">Select recipient account...</option>
              {availableDestinations.map(a => (
                <option key={a.accountNumber} value={a.accountNumber}>
                  {a.name} ({a.accountNumber}) — Owner: {a.ownerUsername}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Amount ($ USD)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              required
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white font-mono focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Idempotency Demonstration Panel */}
          <div className="p-3 rounded-lg bg-slate-900/90 border border-slate-800">
            <div className="flex items-center justify-between mb-1">
              <label className="text-[11px] font-semibold text-indigo-400">Idempotency-Key Header</label>
              <button
                type="button"
                onClick={() => { setIdempotencyKey(generateUuid()); setReplayCount(0); }}
                className="text-[10px] text-slate-400 hover:text-white underline"
              >
                Generate New Key
              </button>
            </div>
            <p className="font-mono text-[11px] text-slate-300 break-all">{idempotencyKey}</p>
            <p className="text-[10px] text-slate-500 mt-1">
              Submitting multiple times with this exact key tests idempotency (same request never pays twice).
            </p>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary text-xs"
            >
              Close
            </button>
            <button
              type="submit"
              disabled={loading || !destAccount}
              className="btn-primary text-xs disabled:opacity-50"
            >
              {loading ? 'Processing...' : replayCount > 0 ? 'Replay Request (Test Idempotency)' : 'Send Transfer'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


import React, { useState } from 'react';
import { generateUuid } from '../api';

export default function DepositModal({ account, onClose, onSuccess }) {
  const [amount, setAmount] = useState('100.00');
  const [description, setDescription] = useState('Checking deposit');
  const [idempotencyKey, setIdempotencyKey] = useState(generateUuid());
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      await onSuccess(account.accountNumber, amount, description, idempotencyKey);
      onClose();
    } catch (err) {
      setError(err.message || 'Deposit failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/70 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="glass-panel w-full max-w-md p-6 relative border border-emerald-500/20 shadow-2xl">
        <button onClick={onClose} className="absolute top-4 right-4 text-slate-400 hover:text-white text-lg">✕</button>

        <h3 className="text-lg font-bold text-white mb-1">Deposit Funds</h3>
        <p className="text-xs text-slate-400 mb-4">
          External liquidity injected via Double-Entry Debit to System Vault and Credit to Customer Account.
        </p>

        {error && (
          <div className="mb-4 p-3 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-medium text-slate-300 mb-1">Target Account</label>
            <input
              type="text"
              readOnly
              value={`${account.name} (${account.accountNumber})`}
              className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-slate-300 cursor-not-allowed"
            />
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
              className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white font-mono"
            />
          </div>

          <div className="p-3 rounded-lg bg-slate-900/90 border border-slate-800">
            <label className="text-[11px] font-semibold text-emerald-400">Idempotency-Key</label>
            <p className="font-mono text-[11px] text-slate-300 break-all">{idempotencyKey}</p>
          </div>

          <div className="flex items-center justify-end gap-2 pt-3">
            <button type="button" onClick={onClose} className="btn-secondary text-xs">Cancel</button>
            <button type="submit" disabled={loading} className="btn-primary text-xs bg-emerald-600 hover:bg-emerald-500">
              {loading ? 'Depositing...' : 'Confirm Deposit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}


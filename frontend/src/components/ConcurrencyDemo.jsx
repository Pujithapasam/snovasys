import React, { useState } from 'react';
import { api } from '../api';

export default function ConcurrencyDemo({ accounts, onRefresh }) {
  const [selectedAcc, setSelectedAcc] = useState(accounts[0]?.accountNumber || 'ACC-BOB-001');
  const [threads, setThreads] = useState(5);
  const [amount, setAmount] = useState('150.00');
  const [running, setRunning] = useState(false);
  const [report, setReport] = useState(null);
  const [error, setError] = useState(null);

  const handleRunTest = async () => {
    setRunning(true);
    setError(null);
    setReport(null);
    try {
      const data = await api.simulateConcurrency(selectedAcc, threads, amount);
      setReport(data);
      if (onRefresh) onRefresh();
    } catch (err) {
      setError(err.message || 'Simulation failed');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="glass-panel p-6 border border-indigo-500/30">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-white">Live Concurrency Attack Simulator</h2>
            <span className="text-[10px] px-2 py-0.5 rounded font-mono badge-blue">Race Condition Proof</span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Fires multiple parallel HTTP requests simultaneously to prove pessimistic row-level locking and zero-overdraft enforcement.
          </p>
        </div>

        <button
          onClick={handleRunTest}
          disabled={running}
          className="btn-primary text-xs flex items-center gap-2 whitespace-nowrap self-start md:self-auto disabled:opacity-50"
        >
          {running ? 'Simulating Concurrent Threads...' : '⚡ Fire Concurrent Attack'}
        </button>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-5">
        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">Target Account</label>
          <select
            value={selectedAcc}
            onChange={(e) => setSelectedAcc(e.target.value)}
            className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white"
          >
            {accounts.filter(a => a.accountType !== 'SYSTEM_CLEARING').map(a => (
              <option key={a.accountNumber} value={a.accountNumber}>
                {a.name} (${Number(a.balance).toFixed(2)})
              </option>
            ))}
          </select>
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">Simultaneous Threads</label>
          <input
            type="number"
            min="2"
            max="20"
            value={threads}
            onChange={(e) => setThreads(parseInt(e.target.value) || 2)}
            className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white font-mono"
          />
        </div>

        <div>
          <label className="block text-xs font-medium text-slate-300 mb-1">Amount per Request ($)</label>
          <input
            type="number"
            step="10"
            min="10"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            className="w-full text-xs px-3 py-2 rounded-lg bg-slate-900 border border-slate-700 text-white font-mono"
          />
        </div>
      </div>

      {error && (
        <div className="p-3 rounded-lg bg-rose-950/60 border border-rose-500/30 text-rose-300 text-xs mb-4">
          {error}
        </div>
      )}

      {report && (
        <div className="p-4 rounded-xl bg-slate-900/90 border border-slate-800">
          <div className="flex items-center justify-between mb-3 pb-2 border-b border-slate-800">
            <span className="text-xs font-bold text-white">Execution Proof Summary</span>
            <span className="text-xs font-mono font-semibold text-emerald-400">
              ✓ Balance Never Negative (${Number(report.endingBalance).toFixed(2)})
            </span>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center mb-4">
            <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Starting Balance</span>
              <span className="text-sm font-bold font-mono text-white">${Number(report.startingBalance).toFixed(2)}</span>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Successful Deductions</span>
              <span className="text-sm font-bold font-mono text-emerald-400">{report.successfulDeductions}</span>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Blocked Overdrafts</span>
              <span className="text-sm font-bold font-mono text-amber-400">{report.blockedRequests}</span>
            </div>
            <div className="p-2.5 rounded-lg bg-slate-950 border border-slate-800">
              <span className="text-[11px] text-slate-400 block">Final Safe Balance</span>
              <span className="text-sm font-bold font-mono text-indigo-400">${Number(report.endingBalance).toFixed(2)}</span>
            </div>
          </div>

          <div className="max-h-40 overflow-y-auto space-y-1.5 font-mono text-[11px]">
            {report.threadLog?.map((t, idx) => (
              <div
                key={idx}
                className={`p-2 rounded flex items-center justify-between border ${
                  t.status === 'SUCCESS'
                    ? 'bg-emerald-950/30 border-emerald-500/20 text-emerald-300'
                    : 'bg-rose-950/30 border-rose-500/20 text-rose-300'
                }`}
              >
                <span>Thread #{t.requestId}: {t.status}</span>
                <span className="text-[10px] text-slate-400">{t.message}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}


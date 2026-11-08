import React, { useState } from 'react';
import { api } from '../api';

export default function LedgerInspector({ entries, integrityReport, onVerifyIntegrity }) {
  const [verifying, setVerifying] = useState(false);
  const [report, setReport] = useState(integrityReport);

  const handleVerify = async () => {
    setVerifying(true);
    try {
      const res = await onVerifyIntegrity();
      setReport(res);
    } catch (err) {
      console.error(err);
    } finally {
      setVerifying(false);
    }
  };

  return (
    <div className="glass-panel p-6 border border-emerald-500/20">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-4 pb-4 border-b border-slate-800">
        <div>
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-bold text-white">Cryptographic Double-Entry Ledger</h2>
            <span className="text-[10px] px-2 py-0.5 rounded font-mono badge-green">Append-Only Immutability</span>
          </div>
          <p className="text-xs text-slate-400 mt-1">
            Every transaction creates mathematically balanced Debit and Credit journal lines linked by SHA-256 hash chaining.
          </p>
        </div>

        <button
          onClick={handleVerify}
          disabled={verifying}
          className="btn-primary text-xs bg-emerald-600 hover:bg-emerald-500 flex items-center gap-2 whitespace-nowrap self-start md:self-auto disabled:opacity-50"
        >
          {verifying ? 'Verifying Hash Chain...' : '🛡️ Verify Ledger Integrity'}
        </button>
      </div>

      {/* Integrity Verdict Card */}
      {report && (
        <div className={`mb-5 p-4 rounded-xl border ${
          report.valid
            ? 'bg-emerald-950/40 border-emerald-500/30 text-emerald-200'
            : 'bg-rose-950/40 border-rose-500/30 text-rose-200'
        }`}>
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 mb-2">
            <div className="flex items-center gap-2">
              <span className="text-base">{report.valid ? '✅' : '❌'}</span>
              <h4 className="font-bold text-xs uppercase tracking-wider">
                {report.valid ? 'Mathematical Proof Verdict: Books Balanced' : 'Audit Violation'}
              </h4>
            </div>
            <span className="text-[11px] font-mono opacity-80">
              Verified {report.totalEntriesVerified} entries
            </span>
          </div>

          <p className="text-xs font-mono mb-3 bg-black/40 p-2 rounded">
            {report.proofVerdict}
          </p>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 text-xs font-mono">
            <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
              <span className="text-[10px] text-slate-400 block">Total Debits</span>
              <span className="font-bold text-white">${Number(report.totalDebits).toFixed(2)}</span>
            </div>
            <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
              <span className="text-[10px] text-slate-400 block">Total Credits</span>
              <span className="font-bold text-white">${Number(report.totalCredits).toFixed(2)}</span>
            </div>
            <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
              <span className="text-[10px] text-slate-400 block">Net Imbalance</span>
              <span className="font-bold text-emerald-400">${Number(report.netImbalance).toFixed(4)}</span>
            </div>
            <div className="p-2 rounded bg-slate-900/80 border border-slate-800">
              <span className="text-[10px] text-slate-400 block">SHA-256 Chain</span>
              <span className="font-bold text-emerald-400">{report.hashChainIntact ? 'VALID & UNTAMPERED' : 'BROKEN'}</span>
            </div>
          </div>
        </div>
      )}

      {/* Journal Table */}
      <div className="overflow-x-auto rounded-lg border border-slate-800">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-900 text-slate-400 uppercase font-mono text-[10px]">
            <tr>
              <th className="py-2.5 px-3">Order</th>
              <th className="py-2.5 px-3">Account</th>
              <th className="py-2.5 px-3">Type</th>
              <th className="py-2.5 px-3 text-right">Amount</th>
              <th className="py-2.5 px-3">Entry Hash (SHA-256)</th>
              <th className="py-2.5 px-3">Timestamp</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/80 font-mono text-[11px]">
            {entries.length === 0 ? (
              <tr>
                <td colSpan="6" className="py-4 text-center text-slate-500 font-sans">
                  No ledger entries recorded yet.
                </td>
              </tr>
            ) : (
              entries.map((e) => (
                <tr key={e.id} className="hover:bg-slate-900/50">
                  <td className="py-2 px-3 text-slate-400 font-bold">#{e.entryOrder}</td>
                  <td className="py-2 px-3 text-slate-200">
                    <span className="font-semibold">{e.accountName}</span>
                    <span className="text-[10px] text-slate-500 block">{e.accountNumber}</span>
                  </td>
                  <td className="py-2 px-3">
                    <span className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                      e.entryType === 'DEBIT'
                        ? 'bg-blue-950 text-blue-300 border border-blue-500/30'
                        : 'bg-emerald-950 text-emerald-300 border border-emerald-500/30'
                    }`}>
                      {e.entryType}
                    </span>
                  </td>
                  <td className="py-2 px-3 text-right font-bold text-white">
                    ${Number(e.amount).toFixed(2)}
                  </td>
                  <td className="py-2 px-3 text-slate-400">
                    <span title={e.entryHash} className="cursor-help">
                      {e.entryHash.substring(0, 10)}...{e.entryHash.substring(54)}
                    </span>
                  </td>
                  <td className="py-2 px-3 text-slate-500 text-[10px]">
                    {new Date(e.createdAt).toLocaleTimeString()}
                  </td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}


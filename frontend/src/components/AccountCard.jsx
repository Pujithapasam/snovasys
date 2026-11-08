import React from 'react';

export default function AccountCard({ account, onTransfer, onDeposit, onWithdraw }) {
  const isVault = account.accountType === 'SYSTEM_CLEARING';

  return (
    <div className="glass-panel-interactive p-5 flex flex-col justify-between">
      <div>
        <div className="flex items-start justify-between gap-2 mb-3">
          <div>
            <h3 className="font-semibold text-slate-100 text-base">{account.name}</h3>
            <p className="text-xs font-mono text-slate-400 mt-0.5">{account.accountNumber}</p>
          </div>
          <span className={`text-[11px] uppercase tracking-wider px-2 py-0.5 rounded font-semibold ${
            isVault ? 'badge-amber' : 'badge-blue'
          }`}>
            {isVault ? 'Clearing Vault' : account.accountType.replace('CUSTOMER_', '')}
          </span>
        </div>

        <div className="mt-4 mb-5">
          <p className="text-xs text-slate-400 font-medium">Verified Ledger Balance</p>
          <div className="text-2xl font-bold font-mono text-white mt-1">
            ${Number(account.balance).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
            <span className="text-xs font-normal text-slate-400 ml-1.5">{account.currency}</span>
          </div>
          <p className="text-[11px] text-emerald-400/90 flex items-center gap-1 mt-1">
            <span>✓</span> Double-entry balanced
          </p>
        </div>
      </div>

      {!isVault && (
        <div className="grid grid-cols-3 gap-2 pt-3 border-t border-slate-800">
          <button
            onClick={() => onTransfer(account)}
            className="text-xs py-1.5 px-2 rounded-lg bg-indigo-600/20 text-indigo-300 hover:bg-indigo-600/30 font-medium border border-indigo-500/30 transition text-center"
          >
            Transfer
          </button>
          <button
            onClick={() => onDeposit(account)}
            className="text-xs py-1.5 px-2 rounded-lg bg-emerald-600/20 text-emerald-300 hover:bg-emerald-600/30 font-medium border border-emerald-500/30 transition text-center"
          >
            Deposit
          </button>
          <button
            onClick={() => onWithdraw(account)}
            className="text-xs py-1.5 px-2 rounded-lg bg-amber-600/20 text-amber-300 hover:bg-amber-600/30 font-medium border border-amber-500/30 transition text-center"
          >
            Withdraw
          </button>
        </div>
      )}
    </div>
  );
}


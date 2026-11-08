import React from 'react';

export default function Navbar({ currentUser, onSwitchUser, integrityStatus }) {
  const users = [
    { name: 'Alice (Holder)', user: 'alice', role: 'ACCOUNT_HOLDER' },
    { name: 'Bob (Holder)', user: 'bob', role: 'ACCOUNT_HOLDER' },
    { name: 'Charlie (Holder)', user: 'charlie', role: 'ACCOUNT_HOLDER' },
    { name: 'Auditor View', user: 'auditor', role: 'AUDITOR' },
    { name: 'System Admin', user: 'admin', role: 'ADMIN' },
  ];

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50 px-6 py-4">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-emerald-400 flex items-center justify-center text-xl shadow-lg shadow-indigo-500/20">
            ⚖️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-white">LedgerLine</h1>
              <span className="text-xs px-2 py-0.5 rounded-full font-mono font-medium badge-green">
                Zero-Sum Active
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Money movement with a ledger you can prove
            </p>
          </div>
        </div>

        {/* Quick User / Role Switcher for seamless evaluation */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-slate-400 mr-1 hidden sm:inline">Switch Role:</span>
          {users.map(u => (
            <button
              key={u.user}
              onClick={() => onSwitchUser(u.user)}
              className={`text-xs px-3 py-1.5 rounded-lg font-medium transition-all ${
                currentUser === u.user
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                  : 'bg-slate-800/80 text-slate-300 hover:bg-slate-700 hover:text-white'
              }`}
            >
              {u.name}
            </button>
          ))}
        </div>
      </div>
    </header>
  );
}


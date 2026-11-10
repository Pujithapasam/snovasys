import React from 'react';

export default function Navbar({ currentUser, onSwitchUser, onOpenOnboard, userList = [] }) {
  const defaultUsers = [
    { name: 'Alice (Holder)', user: 'alice', role: 'ACCOUNT_HOLDER' },
    { name: 'Bob (Holder)', user: 'bob', role: 'ACCOUNT_HOLDER' },
    { name: 'Charlie (Holder)', user: 'charlie', role: 'ACCOUNT_HOLDER' },
    { name: 'Auditor View', user: 'auditor', role: 'AUDITOR' },
    { name: 'System Admin', user: 'admin', role: 'ADMIN' },
  ];

  const allUsers = [...defaultUsers];
  userList.forEach(u => {
    if (!allUsers.find(x => x.user === u.username)) {
      allUsers.push({ name: `${u.fullName || u.username} (${u.role})`, user: u.username, role: u.role });
    }
  });

  return (
    <header className="border-b border-slate-800 bg-slate-900/80 backdrop-blur sticky top-0 z-50 px-6 py-4">
      <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-emerald-400 flex items-center justify-center text-xl shadow-lg shadow-indigo-500/20">
            ⚖️
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-white">BalanceVault</h1>
              <span className="text-[10px] text-slate-400 font-mono italic">(formerly LedgerLine)</span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-mono font-medium badge-green">
                Zero-Sum Active
              </span>
              <span className="text-[10px] px-2 py-0.5 rounded-full font-mono font-medium bg-red-950/80 text-red-300 border border-red-500/30 hidden lg:inline">
                Redis Rate-Limited
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Double-entry settlement engine with provable integrity & RBAC
            </p>
          </div>
        </div>

        {/* Quick User / Role Switcher + Onboarding trigger */}
        <div className="flex flex-wrap items-center gap-2">
          <span className="text-xs text-slate-400 mr-1 hidden sm:inline">Role:</span>
          {allUsers.map(u => (
            <button
              key={u.user}
              onClick={() => onSwitchUser(u.user)}
              className={`text-xs px-2.5 py-1.5 rounded-lg font-medium transition-all ${
                currentUser === u.user
                  ? 'bg-indigo-600 text-white shadow-md shadow-indigo-600/30'
                  : 'bg-slate-800/80 text-slate-300 hover:bg-slate-700 hover:text-white'
              }`}
            >
              {u.name}
            </button>
          ))}

          {onOpenOnboard && (
            <button
              onClick={onOpenOnboard}
              className="text-xs px-3 py-1.5 rounded-lg font-bold bg-emerald-600 hover:bg-emerald-500 text-white shadow-md shadow-emerald-600/30 flex items-center gap-1 ml-1"
            >
              <span>+</span>
              <span>Onboard</span>
            </button>
          )}
        </div>
      </div>
    </header>
  );
}

"use client";

import React, { useState } from "react";
import { WalletDetails } from "@/types";
import { Copy, Check, ShieldCheck, AlertTriangle, ArrowUpRight, PlusCircle } from "lucide-react";

interface WalletCardProps {
  wallet: WalletDetails;
  onDepositClick: () => void;
  onTransferClick: () => void;
}

export default function WalletCard({
  wallet,
  onDepositClick,
  onTransferClick,
}: WalletCardProps) {
  const [copied, setCopied] = useState(false);

  const copyWalletNumber = () => {
    navigator.clipboard.writeText(wallet.walletNumber);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="relative overflow-hidden rounded-2xl bg-gradient-to-br from-indigo-900 via-slate-900 to-slate-950 p-6 sm:p-8 border border-indigo-500/20 shadow-xl shadow-indigo-950/40">
      {/* Decorative background glow */}
      <div className="absolute -top-24 -right-24 w-64 h-64 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <div>
          <span className="text-xs uppercase tracking-wider text-indigo-300 font-semibold">
            Primary Digital Wallet
          </span>
          <div className="flex items-center space-x-2 mt-1">
            <span className="font-mono text-sm sm:text-base text-slate-300 bg-slate-800/80 px-3 py-1 rounded-lg border border-slate-700">
              {wallet.walletNumber}
            </span>
            <button
              onClick={copyWalletNumber}
              className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-400 hover:text-white transition-colors"
              title="Copy Wallet Number"
            >
              {copied ? <Check className="w-4 h-4 text-emerald-400" /> : <Copy className="w-4 h-4" />}
            </button>
          </div>
        </div>

        {/* Ledger Integrity Badge */}
        <div
          className={`flex items-center space-x-1.5 px-3 py-1 rounded-full text-xs font-medium border ${
            wallet.ledgerAudited
              ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
              : "bg-amber-500/10 text-amber-400 border-amber-500/20"
          }`}
        >
          {wallet.ledgerAudited ? (
            <>
              <ShieldCheck className="w-4 h-4" />
              <span>Ledger Balanced ($0.00 Discrepancy)</span>
            </>
          ) : (
            <>
              <AlertTriangle className="w-4 h-4" />
              <span>Ledger Audit Pending</span>
            </>
          )}
        </div>
      </div>

      <div className="my-4">
        <span className="text-sm text-slate-400 font-medium">Available Balance</span>
        <div className="flex items-baseline space-x-2 mt-1">
          <span className="text-4xl sm:text-5xl font-extrabold tracking-tight text-white font-mono">
            ${Number(wallet.balance).toLocaleString("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
          </span>
          <span className="text-sm font-semibold text-indigo-400">{wallet.currency}</span>
        </div>
      </div>

      <div className="mt-8 flex flex-wrap gap-3">
        <button
          onClick={onTransferClick}
          className="flex-1 min-w-[140px] flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 transition-all hover:translate-y-[-1px]"
        >
          <ArrowUpRight className="w-4 h-4" />
          <span>Send Money</span>
        </button>

        <button
          onClick={onDepositClick}
          className="flex-1 min-w-[140px] flex items-center justify-center space-x-2 py-3 px-4 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 font-semibold transition-all hover:translate-y-[-1px]"
        >
          <PlusCircle className="w-4 h-4" />
          <span>Add Funds</span>
        </button>
      </div>
    </div>
  );
}

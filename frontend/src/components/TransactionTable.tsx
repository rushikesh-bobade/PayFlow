"use client";

import React from "react";
import { TransactionItem } from "@/types";
import { ArrowDownLeft, ArrowUpRight, Clock, AlertCircle } from "lucide-react";

interface TransactionTableProps {
  transactions: TransactionItem[];
  currentWalletNumber: string;
}

export default function TransactionTable({
  transactions,
  currentWalletNumber,
}: TransactionTableProps) {
  if (transactions.length === 0) {
    return (
      <div className="text-center py-12 border border-slate-800 rounded-2xl bg-slate-900/40">
        <Clock className="w-8 h-8 text-slate-500 mx-auto mb-2" />
        <p className="text-slate-400 text-sm">No transactions recorded yet.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto border border-slate-800 rounded-2xl bg-slate-900/60 backdrop-blur-sm">
      <table className="w-full text-left text-sm text-slate-300">
        <thead className="bg-slate-800/80 text-xs uppercase tracking-wider text-slate-400 font-semibold border-b border-slate-700">
          <tr>
            <th className="py-3.5 px-4">Type</th>
            <th className="py-3.5 px-4">Reference ID</th>
            <th className="py-3.5 px-4">Counterparty</th>
            <th className="py-3.5 px-4">Amount</th>
            <th className="py-3.5 px-4">Status</th>
            <th className="py-3.5 px-4 text-right">Timestamp</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/80">
          {transactions.map((txn) => {
            const isOutflow = txn.sourceWalletNumber === currentWalletNumber;
            const isDeposit = txn.type === "DEPOSIT";

            return (
              <tr key={txn.referenceId} className="hover:bg-slate-800/40 transition-colors">
                <td className="py-3.5 px-4">
                  <div className="flex items-center space-x-2">
                    <div
                      className={`p-1.5 rounded-lg ${
                        isDeposit
                          ? "bg-emerald-500/10 text-emerald-400"
                          : isOutflow
                          ? "bg-rose-500/10 text-rose-400"
                          : "bg-emerald-500/10 text-emerald-400"
                      }`}
                    >
                      {isDeposit || !isOutflow ? (
                        <ArrowDownLeft className="w-4 h-4" />
                      ) : (
                        <ArrowUpRight className="w-4 h-4" />
                      )}
                    </div>
                    <span className="font-medium text-white">{txn.type}</span>
                  </div>
                </td>
                <td className="py-3.5 px-4 font-mono text-xs text-slate-400">
                  {txn.referenceId}
                </td>
                <td className="py-3.5 px-4 font-mono text-xs">
                  {isDeposit
                    ? "External Deposit"
                    : isOutflow
                    ? `To: ${txn.targetWalletNumber}`
                    : `From: ${txn.sourceWalletNumber}`}
                </td>
                <td
                  className={`py-3.5 px-4 font-mono font-semibold ${
                    isDeposit || !isOutflow ? "text-emerald-400" : "text-rose-400"
                  }`}
                >
                  {isDeposit || !isOutflow ? "+" : "-"}${Number(txn.amount).toFixed(2)}
                </td>
                <td className="py-3.5 px-4">
                  <span
                    className={`px-2.5 py-0.5 rounded-full text-[11px] font-semibold tracking-wide uppercase ${
                      txn.status === "COMPLETED"
                        ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                        : txn.status === "PENDING"
                        ? "bg-amber-500/10 text-amber-400 border border-amber-500/20"
                        : "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                    }`}
                  >
                    {txn.status}
                  </span>
                </td>
                <td className="py-3.5 px-4 text-right text-xs text-slate-400">
                  {new Date(txn.createdAt).toLocaleDateString()} {new Date(txn.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>
    </div>
  );
}

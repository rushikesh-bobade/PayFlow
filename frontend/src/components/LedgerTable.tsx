"use client";

import React from "react";
import { LedgerEntryItem } from "@/types";
import { BookOpen, ShieldCheck } from "lucide-react";

interface LedgerTableProps {
  entries: LedgerEntryItem[];
}

export default function LedgerTable({ entries }: LedgerTableProps) {
  if (entries.length === 0) {
    return (
      <div className="text-center py-12 border border-slate-800 rounded-2xl bg-slate-900/40">
        <BookOpen className="w-8 h-8 text-slate-500 mx-auto mb-2" />
        <p className="text-slate-400 text-sm">No ledger entries posted yet.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto border border-slate-800 rounded-2xl bg-slate-900/60 backdrop-blur-sm">
      <div className="px-5 py-4 border-b border-slate-800 flex justify-between items-center bg-slate-800/40">
        <div className="flex items-center space-x-2">
          <BookOpen className="w-4 h-4 text-indigo-400" />
          <h3 className="text-sm font-semibold text-white">Immutable Double-Entry Ledger</h3>
        </div>
        <div className="flex items-center space-x-1.5 text-xs text-emerald-400">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Cryptographically Consistent</span>
        </div>
      </div>

      <table className="w-full text-left text-sm text-slate-300">
        <thead className="bg-slate-800/80 text-xs uppercase tracking-wider text-slate-400 font-semibold border-b border-slate-700">
          <tr>
            <th className="py-3 px-4">Entry ID</th>
            <th className="py-3 px-4">Txn Ref</th>
            <th className="py-3 px-4">Posting Type</th>
            <th className="py-3 px-4">Amount</th>
            <th className="py-3 px-4">Balance After</th>
            <th className="py-3 px-4">Description</th>
            <th className="py-3 px-4 text-right">Timestamp</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800/80 font-mono text-xs">
          {entries.map((entry) => (
            <tr key={entry.entryId} className="hover:bg-slate-800/40 transition-colors">
              <td className="py-3 px-4 text-slate-400">{entry.entryId}</td>
              <td className="py-3 px-4 text-indigo-300">{entry.transactionReference}</td>
              <td className="py-3 px-4">
                <span
                  className={`px-2 py-0.5 rounded text-[10px] font-bold tracking-wider ${
                    entry.type === "CREDIT"
                      ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20"
                      : "bg-rose-500/10 text-rose-400 border border-rose-500/20"
                  }`}
                >
                  {entry.type}
                </span>
              </td>
              <td
                className={`py-3 px-4 font-semibold ${
                  entry.type === "CREDIT" ? "text-emerald-400" : "text-rose-400"
                }`}
              >
                {entry.type === "CREDIT" ? "+" : "-"}${Number(entry.amount).toFixed(2)}
              </td>
              <td className="py-3 px-4 text-white font-semibold">
                ${Number(entry.balanceAfter).toFixed(2)}
              </td>
              <td className="py-3 px-4 text-slate-400 font-sans">{entry.description}</td>
              <td className="py-3 px-4 text-right text-slate-500 font-sans">
                {new Date(entry.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

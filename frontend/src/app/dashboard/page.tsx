"use client";

import React, { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import Navbar from "@/components/Navbar";
import WalletCard from "@/components/WalletCard";
import TransferModal from "@/components/TransferModal";
import TransactionTable from "@/components/TransactionTable";
import LedgerTable from "@/components/LedgerTable";
import { api } from "@/lib/api";
import { getSession } from "@/lib/auth";
import { UserSession, WalletDetails, TransactionItem, LedgerEntryItem, AuditReport } from "@/types";
import { RefreshCw, BookOpen, Clock, ShieldCheck, CheckCircle, AlertCircle } from "lucide-react";

export default function DashboardPage() {
  const router = useRouter();
  const [session, setSession] = useState<UserSession | null>(null);
  const [wallet, setWallet] = useState<WalletDetails | null>(null);
  const [transactions, setTransactions] = useState<TransactionItem[]>([]);
  const [ledgerEntries, setLedgerEntries] = useState<LedgerEntryItem[]>([]);
  const [activeTab, setActiveTab] = useState<"transactions" | "ledger">("transactions");
  const [loading, setLoading] = useState(true);
  const [transferModalOpen, setTransferModalOpen] = useState(false);
  const [auditReport, setAuditReport] = useState<AuditReport | null>(null);
  const [auditLoading, setAuditLoading] = useState(false);

  // Deposit state
  const [depositAmount, setDepositAmount] = useState("100.00");
  const [depositLoading, setDepositLoading] = useState(false);
  const [depositOpen, setDepositOpen] = useState(false);

  const loadData = async () => {
    try {
      const walletData = await api.getMyWallet();
      setWallet(walletData);

      const txnData = await api.getMyTransactions(0, 15);
      setTransactions(txnData.content || []);

      const ledgerData = await api.getMyLedgerEntries(0, 15);
      setLedgerEntries(ledgerData.content || []);
    } catch (err) {
      console.error("Failed to load dashboard data:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const activeSession = getSession();
    if (!activeSession) {
      router.push("/login");
      return;
    }
    setSession(activeSession);
    loadData();
  }, [router]);

  const handleDeposit = async () => {
    if (!wallet) return;
    const parsed = parseFloat(depositAmount);
    if (isNaN(parsed) || parsed <= 0) return;

    setDepositLoading(true);
    try {
      const idempotencyKey = crypto.randomUUID();
      await api.deposit(wallet.walletNumber, parsed, "Dashboard Top-up", idempotencyKey);
      setDepositOpen(false);
      await loadData();
    } catch (err: any) {
      alert(err.message || "Deposit failed");
    } finally {
      setDepositLoading(false);
    }
  };

  const handleAudit = async () => {
    setAuditLoading(true);
    try {
      const report = await api.auditWallet();
      setAuditReport(report);
    } catch (err: any) {
      alert("Audit failed: " + err.message);
    } finally {
      setAuditLoading(false);
    }
  };

  if (loading || !wallet) {
    return (
      <div className="min-h-screen bg-slate-950 flex flex-col">
        <Navbar session={session} />
        <div className="flex-1 flex items-center justify-center">
          <div className="flex items-center space-x-3 text-slate-400">
            <RefreshCw className="w-6 h-6 animate-spin text-indigo-500" />
            <span className="font-mono text-sm">Loading Wallet State...</span>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <Navbar session={session} />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header Action Bar */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-8">
          <div>
            <h1 className="text-2xl sm:text-3xl font-extrabold text-white">
              Welcome back, {wallet.ownerName.split(" ")[0]}
            </h1>
            <p className="text-sm text-slate-400 mt-1">
              Account status:{" "}
              <span className="text-emerald-400 font-semibold uppercase">{wallet.status}</span>
            </p>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={handleAudit}
              disabled={auditLoading}
              className="flex items-center space-x-2 px-3.5 py-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 text-sm font-medium transition-all"
            >
              <ShieldCheck className="w-4 h-4 text-indigo-400" />
              <span>{auditLoading ? "Auditing..." : "Audit Ledger"}</span>
            </button>
            <button
              onClick={loadData}
              className="p-2 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-400 hover:text-white transition-colors"
              title="Refresh State"
            >
              <RefreshCw className="w-4 h-4" />
            </button>
          </div>
        </div>

        {/* Audit Report Banner */}
        {auditReport && (
          <div className="mb-8 p-4 rounded-2xl bg-indigo-950/40 border border-indigo-500/30 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center space-x-3">
              <div className="w-10 h-10 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center shrink-0">
                <CheckCircle className="w-5 h-5" />
              </div>
              <div>
                <p className="text-sm font-semibold text-white">Ledger Financial Audit Result</p>
                <p className="text-xs text-slate-300">
                  Wallet Balance: <span className="font-mono text-white">${auditReport.currentBalance}</span> &bull; 
                  Sum(Credit - Debit): <span className="font-mono text-white">${auditReport.calculatedLedgerSum}</span> &bull; 
                  Discrepancy: <span className="font-mono text-emerald-400">${auditReport.discrepancy}</span>
                </p>
              </div>
            </div>
            <button
              onClick={() => setAuditReport(null)}
              className="text-xs text-slate-400 hover:text-white underline self-end sm:self-auto"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Primary Wallet Hero Card */}
        <div className="mb-10 max-w-xl">
          <WalletCard
            wallet={wallet}
            onDepositClick={() => setDepositOpen(true)}
            onTransferClick={() => setTransferModalOpen(true)}
          />
        </div>

        {/* Deposit Modal / Drawer */}
        {depositOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
            <div className="w-full max-w-sm rounded-2xl bg-slate-900 border border-slate-800 p-6 shadow-2xl">
              <h3 className="text-lg font-bold text-white mb-2">Deposit Funds</h3>
              <p className="text-xs text-slate-400 mb-4">
                Simulate a bank transfer into wallet: <span className="font-mono text-indigo-300">{wallet.walletNumber}</span>
              </p>
              <div className="space-y-4">
                <div>
                  <label className="block text-xs uppercase font-semibold text-slate-400 mb-1">Amount ($ USD)</label>
                  <input
                    type="number"
                    min="1.00"
                    step="0.01"
                    value={depositAmount}
                    onChange={(e) => setDepositAmount(e.target.value)}
                    className="w-full px-4 py-2 rounded-xl bg-slate-800 border border-slate-700 text-white font-mono text-sm focus:outline-none focus:border-indigo-500"
                  />
                </div>
                <div className="flex gap-2">
                  {["50.00", "100.00", "500.00"].map((amt) => (
                    <button
                      key={amt}
                      type="button"
                      onClick={() => setDepositAmount(amt)}
                      className="flex-1 py-1 text-xs rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 border border-slate-700"
                    >
                      +${amt}
                    </button>
                  ))}
                </div>
                <div className="flex gap-3 pt-2">
                  <button
                    onClick={() => setDepositOpen(false)}
                    className="flex-1 py-2.5 rounded-xl bg-slate-800 text-slate-300 font-semibold text-sm hover:bg-slate-700"
                  >
                    Cancel
                  </button>
                  <button
                    onClick={handleDeposit}
                    disabled={depositLoading}
                    className="flex-1 py-2.5 rounded-xl bg-indigo-600 text-white font-semibold text-sm hover:bg-indigo-500 shadow-md shadow-indigo-600/30"
                  >
                    {depositLoading ? "Posting..." : "Confirm"}
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Tab Navigation */}
        <div className="flex items-center space-x-4 border-b border-slate-800 mb-6">
          <button
            onClick={() => setActiveTab("transactions")}
            className={`flex items-center space-x-2 pb-3 text-sm font-semibold transition-all border-b-2 ${
              activeTab === "transactions"
                ? "border-indigo-500 text-white"
                : "border-transparent text-slate-400 hover:text-slate-300"
            }`}
          >
            <Clock className="w-4 h-4" />
            <span>Transaction History</span>
            <span className="ml-1.5 px-2 py-0.5 rounded-full bg-slate-800 text-xs text-slate-400">
              {transactions.length}
            </span>
          </button>

          <button
            onClick={() => setActiveTab("ledger")}
            className={`flex items-center space-x-2 pb-3 text-sm font-semibold transition-all border-b-2 ${
              activeTab === "ledger"
                ? "border-indigo-500 text-white"
                : "border-transparent text-slate-400 hover:text-slate-300"
            }`}
          >
            <BookOpen className="w-4 h-4" />
            <span>Double-Entry Ledger Audit</span>
            <span className="ml-1.5 px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 text-xs">
              {ledgerEntries.length}
            </span>
          </button>
        </div>

        {/* Content based on Active Tab */}
        {activeTab === "transactions" ? (
          <TransactionTable
            transactions={transactions}
            currentWalletNumber={wallet.walletNumber}
          />
        ) : (
          <LedgerTable entries={ledgerEntries} />
        )}
      </main>

      {/* Transfer Modal */}
      <TransferModal
        isOpen={transferModalOpen}
        onClose={() => setTransferModalOpen(false)}
        onSuccess={loadData}
        currentWalletNumber={wallet.walletNumber}
      />
    </div>
  );
}

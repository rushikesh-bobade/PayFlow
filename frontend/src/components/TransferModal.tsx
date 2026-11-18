"use client";

import React, { useState, useEffect } from "react";
import { api } from "@/lib/api";
import { X, KeyRound, ShieldAlert, CheckCircle2, ArrowRight } from "lucide-react";

interface TransferModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: () => void;
  currentWalletNumber: string;
}

export default function TransferModal({
  isOpen,
  onClose,
  onSuccess,
  currentWalletNumber,
}: TransferModalProps) {
  const [targetWallet, setTargetWallet] = useState("");
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [idempotencyKey, setIdempotencyKey] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successTxn, setSuccessTxn] = useState<string | null>(null);

  useEffect(() => {
    if (isOpen) {
      // Generate a new idempotency key on modal open
      setIdempotencyKey(crypto.randomUUID());
      setError(null);
      setSuccessTxn(null);
      setTargetWallet("");
      setAmount("");
      setDescription("");
    }
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    const parsedAmount = parseFloat(amount);
    if (isNaN(parsedAmount) || parsedAmount <= 0) {
      setError("Please enter a valid amount greater than 0");
      return;
    }

    if (targetWallet.trim().toUpperCase() === currentWalletNumber.toUpperCase()) {
      setError("Cannot transfer funds to your own wallet");
      return;
    }

    setLoading(true);
    try {
      const txn = await api.transfer(
        targetWallet.trim().toUpperCase(),
        parsedAmount,
        description.trim() || undefined,
        idempotencyKey
      );
      setSuccessTxn(txn.referenceId);
      setTimeout(() => {
        onSuccess();
        onClose();
      }, 1800);
    } catch (err: any) {
      setError(err.message || "Transfer failed. Please check wallet number and balance.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
      <div className="relative w-full max-w-md rounded-2xl bg-slate-900 border border-slate-800 p-6 shadow-2xl">
        <div className="flex justify-between items-center mb-5">
          <h2 className="text-xl font-bold text-white">Send Money</h2>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Idempotency Key Tag */}
        <div className="mb-5 p-3 rounded-xl bg-slate-800/60 border border-slate-700/60 flex items-center space-x-2.5">
          <KeyRound className="w-4 h-4 text-indigo-400 shrink-0" />
          <div className="overflow-hidden">
            <span className="text-[10px] uppercase font-semibold text-indigo-300 block">
              Distributed Idempotency Protection
            </span>
            <span className="text-xs font-mono text-slate-400 truncate block">
              {idempotencyKey}
            </span>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-3 rounded-xl bg-rose-500/10 border border-rose-500/20 text-rose-400 text-sm flex items-start space-x-2">
            <ShieldAlert className="w-4 h-4 mt-0.5 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {successTxn && (
          <div className="mb-4 p-3 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 text-sm flex items-start space-x-2">
            <CheckCircle2 className="w-4 h-4 mt-0.5 shrink-0" />
            <div>
              <p className="font-semibold">Transfer Atomic &amp; Committed</p>
              <p className="font-mono text-xs text-emerald-300 mt-0.5">Ref: {successTxn}</p>
            </div>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
              Recipient Wallet Number
            </label>
            <input
              type="text"
              required
              placeholder="e.g. WLT1048295831"
              value={targetWallet}
              onChange={(e) => setTargetWallet(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-mono text-sm"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
              Amount (USD)
            </label>
            <div className="relative">
              <span className="absolute left-3.5 top-2.5 text-slate-400 font-mono">$</span>
              <input
                type="number"
                step="0.01"
                min="0.01"
                required
                placeholder="0.00"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
                className="w-full pl-8 pr-4 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 font-mono text-sm"
              />
            </div>
          </div>

          <div>
            <label className="block text-xs font-semibold uppercase tracking-wider text-slate-400 mb-1">
              Note (Optional)
            </label>
            <input
              type="text"
              placeholder="Invoice payment, split bill, etc."
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-800 border border-slate-700 text-white placeholder-slate-500 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 text-sm"
            />
          </div>

          <div className="pt-2">
            <button
              type="submit"
              disabled={loading || !!successTxn}
              className="w-full py-3 rounded-xl bg-indigo-600 hover:bg-indigo-500 disabled:bg-indigo-600/50 text-white font-semibold shadow-lg shadow-indigo-600/30 transition-all flex items-center justify-center space-x-2"
            >
              {loading ? (
                <span>Executing Atomic Transfer...</span>
              ) : (
                <>
                  <span>Confirm &amp; Transfer</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

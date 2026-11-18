"use client";

import React from "react";
import Link from "next/link";
import Navbar from "@/components/Navbar";
import { ShieldCheck, Zap, Lock, RefreshCw, Database, Server, ArrowRight } from "lucide-react";

export default function HomePage() {
  return (
    <div className="min-h-screen bg-slate-950 flex flex-col">
      <Navbar />

      {/* Hero Section */}
      <section className="relative overflow-hidden pt-20 pb-24 sm:pt-28 sm:pb-32">
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_80%_80%_at_50%_-20%,rgba(99,102,241,0.25),rgba(255,255,255,0))]" />

        <div className="relative max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <div className="inline-flex items-center space-x-2 px-3 py-1 rounded-full bg-indigo-500/10 border border-indigo-500/20 text-indigo-300 text-xs font-semibold mb-6">
            <span className="flex h-2 w-2 rounded-full bg-emerald-400 animate-pulse" />
            <span>Production-Grade Concurrency &amp; Ledger Architecture</span>
          </div>

          <h1 className="text-4xl sm:text-6xl font-extrabold tracking-tight text-white mb-6">
            PayFlow Engine
            <span className="block bg-clip-text text-transparent bg-gradient-to-r from-indigo-400 to-cyan-400 mt-2">
              Secure Digital Wallet &amp; Transaction Processor
            </span>
          </h1>

          <p className="max-w-2xl mx-auto text-lg text-slate-400 mb-10">
            Engineered with Java 17, Spring Boot 3, and MySQL. Features pessimistic locking with deterministic lock-ordering, distributed idempotency keys, and immutable double-entry bookkeeping.
          </p>

          <div className="flex flex-col sm:flex-row items-center justify-center gap-4">
            <Link
              href="/register"
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-indigo-600 hover:bg-indigo-500 text-white font-semibold shadow-lg shadow-indigo-600/30 transition-all flex items-center justify-center space-x-2"
            >
              <span>Launch Demo Console</span>
              <ArrowRight className="w-4 h-4" />
            </Link>
            <a
              href="http://localhost:8080/swagger-ui.html"
              target="_blank"
              rel="noreferrer"
              className="w-full sm:w-auto px-8 py-3.5 rounded-xl bg-slate-800 hover:bg-slate-700 border border-slate-700 text-slate-200 font-semibold transition-all flex items-center justify-center space-x-2"
            >
              <Server className="w-4 h-4" />
              <span>Explore OpenAPI / Swagger</span>
            </a>
          </div>
        </div>
      </section>

      {/* Architectural Highlights */}
      <section className="py-16 border-t border-slate-800 bg-slate-900/50">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-12">
            <h2 className="text-2xl sm:text-3xl font-bold text-white">Engineered For Financial Invariance</h2>
            <p className="text-slate-400 text-sm mt-2">How PayFlow solves the toughest problems in transactional systems</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-indigo-500/10 text-indigo-400 flex items-center justify-center mb-4">
                <Lock className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">Pessimistic Concurrency</h3>
              <p className="text-sm text-slate-400">
                Guarantees zero double-spending using row-level exclusive locks with sorted ID lock acquisition to permanently eradicate deadlock hazards under high load.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-cyan-500/10 text-cyan-400 flex items-center justify-center mb-4">
                <RefreshCw className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">Distributed Idempotency</h3>
              <p className="text-sm text-slate-400">
                Network retry and double-click safe. Endpoints enforce unique UUID idempotency keys to ensure identical responses without re-executing state mutations.
              </p>
            </div>

            <div className="p-6 rounded-2xl bg-slate-900 border border-slate-800 hover:border-indigo-500/30 transition-all">
              <div className="w-12 h-12 rounded-xl bg-emerald-500/10 text-emerald-400 flex items-center justify-center mb-4">
                <Database className="w-6 h-6" />
              </div>
              <h3 className="text-lg font-semibold text-white mb-2">Double-Entry Ledger</h3>
              <p className="text-sm text-slate-400">
                Wallet balances are backed by an immutable ledger of debit and credit rows. Audited continuously where balance must strictly equal Sum(Credits) - Sum(Debits).
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* Footer */}
      <footer className="mt-auto py-8 border-t border-slate-800 text-center text-xs text-slate-500">
        PayFlow Open Architecture &bull; Built with Spring Boot 3, MySQL, Next.js 14, and Docker.
      </footer>
    </div>
  );
}

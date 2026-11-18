"use client";

import React from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { UserSession } from "@/types";
import { clearSession } from "@/lib/auth";
import { Wallet, LogOut, ShieldCheck, ArrowRightLeft } from "lucide-react";

interface NavbarProps {
  session?: UserSession | null;
}

export default function Navbar({ session }: NavbarProps) {
  const router = useRouter();

  const handleLogout = () => {
    clearSession();
    router.push("/login");
  };

  return (
    <nav className="border-b border-slate-800 bg-slate-900/80 backdrop-blur-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between h-16 items-center">
          <Link href={session ? "/dashboard" : "/"} className="flex items-center space-x-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 flex items-center justify-center shadow-lg shadow-indigo-500/30">
              <Wallet className="w-5 h-5 text-white" />
            </div>
            <div>
              <span className="text-xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-white to-slate-400">
                PayFlow
              </span>
              <span className="ml-2 text-xs px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                Core Engine
              </span>
            </div>
          </Link>

          {session ? (
            <div className="flex items-center space-x-4">
              <div className="hidden sm:flex flex-col text-right">
                <span className="text-sm font-semibold text-slate-200">{session.fullName}</span>
                <span className="text-xs text-slate-400 font-mono">{session.email}</span>
              </div>
              <button
                onClick={handleLogout}
                className="flex items-center space-x-1.5 px-3 py-1.5 rounded-lg border border-slate-700 hover:border-slate-600 bg-slate-800 text-slate-300 hover:text-white transition-colors text-sm"
              >
                <LogOut className="w-4 h-4" />
                <span>Sign Out</span>
              </button>
            </div>
          ) : (
            <div className="flex items-center space-x-3">
              <Link
                href="/login"
                className="px-4 py-2 text-sm text-slate-300 hover:text-white transition-colors"
              >
                Sign In
              </Link>
              <Link
                href="/register"
                className="px-4 py-2 text-sm font-medium rounded-lg bg-indigo-600 hover:bg-indigo-500 text-white shadow-md shadow-indigo-600/30 transition-all"
              >
                Get Started
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}

import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PayFlow | Secure Digital Wallet & Transaction Engine",
  description: "High-reliability digital wallet with double-entry ledger accounting, pessimistic concurrency control, and distributed idempotency.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body className="antialiased font-sans bg-slate-900 text-slate-100 min-h-screen">
        {children}
      </body>
    </html>
  );
}

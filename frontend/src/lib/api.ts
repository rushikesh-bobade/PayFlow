import { ApiResponse, UserSession, WalletDetails, TransactionItem, LedgerEntryItem, AuditReport } from "@/types";
import { getSession } from "./auth";

const BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080/api/v1";

interface RequestOptions extends RequestInit {
  idempotencyKey?: string;
}

async function request<T>(endpoint: string, options: RequestOptions = {}): Promise<T> {
  const session = getSession();
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (session?.token) {
    (headers as Record<string, string>)["Authorization"] = `Bearer ${session.token}`;
  }

  if (options.idempotencyKey) {
    (headers as Record<string, string>)["Idempotency-Key"] = options.idempotencyKey;
  }

  const response = await fetch(`${BASE_URL}${endpoint}`, {
    ...options,
    headers,
  });

  const body: ApiResponse<T> = await response.json();

  if (!response.ok || !body.success) {
    throw new Error(body.message || `HTTP ${response.status}: Request failed`);
  }

  return body.data;
}

export const api = {
  // Auth
  register: (data: { email: string; password: string; fullName: string }) =>
    request<UserSession>("/auth/register", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  login: (data: { email: string; password: string }) =>
    request<UserSession>("/auth/login", {
      method: "POST",
      body: JSON.stringify(data),
    }),

  // Wallet
  getMyWallet: () => request<WalletDetails>("/wallets/me"),

  deposit: (walletNumber: string, amount: number, description?: string, idempotencyKey?: string) =>
    request<WalletDetails>(`/wallets/${walletNumber}/deposit`, {
      method: "POST",
      body: JSON.stringify({ amount, description }),
      idempotencyKey,
    }),

  // Transactions
  transfer: (targetWalletNumber: string, amount: number, description?: string, idempotencyKey?: string) =>
    request<TransactionItem>("/transactions/transfer", {
      method: "POST",
      body: JSON.stringify({ targetWalletNumber, amount, description }),
      idempotencyKey,
    }),

  getMyTransactions: (page = 0, size = 10) =>
    request<{ content: TransactionItem[]; totalElements: number }>(`/transactions/my-transactions?page=${page}&size=${size}`),

  // Ledger & Audit
  getMyLedgerEntries: (page = 0, size = 15) =>
    request<{ content: LedgerEntryItem[]; totalElements: number }>(`/ledger/my-entries?page=${page}&size=${size}`),

  auditWallet: () => request<AuditReport>("/ledger/audit"),
};

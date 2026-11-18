export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

export interface UserSession {
  token: string;
  userId: number;
  email: string;
  fullName: string;
  role: string;
  walletNumber: string;
}

export interface WalletDetails {
  walletNumber: string;
  ownerName: string;
  ownerEmail: string;
  balance: number;
  currency: string;
  status: string;
  ledgerNetBalance: number;
  ledgerAudited: boolean;
}

export interface TransactionItem {
  referenceId: string;
  type: "DEPOSIT" | "WITHDRAWAL" | "TRANSFER";
  status: "PENDING" | "COMPLETED" | "FAILED";
  amount: number;
  fee: number;
  sourceWalletNumber?: string;
  targetWalletNumber?: string;
  description: string;
  failureReason?: string;
  createdAt: string;
}

export interface LedgerEntryItem {
  entryId: string;
  transactionReference: string;
  walletNumber: string;
  type: "DEBIT" | "CREDIT";
  amount: number;
  balanceAfter: number;
  description: string;
  createdAt: string;
}

export interface AuditReport {
  walletNumber: string;
  currentBalance: number;
  calculatedLedgerSum: number;
  discrepancy: number;
  ledgerIntegrityVerified: boolean;
}

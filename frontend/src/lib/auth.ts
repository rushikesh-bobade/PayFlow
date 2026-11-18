import { UserSession } from "@/types";

const AUTH_KEY = "payflow_auth_session";

export function saveSession(session: UserSession): void {
  if (typeof window !== "undefined") {
    localStorage.setItem(AUTH_KEY, JSON.stringify(session));
  }
}

export function getSession(): UserSession | null {
  if (typeof window === "undefined") return null;
  const raw = localStorage.getItem(AUTH_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw);
  } catch {
    return null;
  }
}

export function clearSession(): void {
  if (typeof window !== "undefined") {
    localStorage.removeItem(AUTH_KEY);
  }
}

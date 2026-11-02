import type { LoginResponse, UserIdentity } from '../api/types';

/**
 * Sessão do usuário autenticado no navegador.
 * Fica no sessionStorage: some ao fechar a aba e não é compartilhada entre abas.
 */
export interface Session {
  accessToken: string;
  tokenType: string;
  expiresAt: number;
  usuario: UserIdentity;
}

const STORAGE_KEY = 'authsystem.session';

export function createSession(response: LoginResponse, now: number = Date.now()): Session {
  return {
    accessToken: response.accessToken,
    tokenType: response.tokenType,
    expiresAt: now + response.expiresIn * 1000,
    usuario: response.usuario,
  };
}

export function saveSession(session: Session): void {
  sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session));
}

/** Devolve a sessão salva, ou null se não houver, estiver corrompida ou o token tiver expirado. */
export function loadSession(now: number = Date.now()): Session | null {
  const raw = sessionStorage.getItem(STORAGE_KEY);
  if (!raw) return null;
  try {
    const session = JSON.parse(raw) as Session;
    if (typeof session.expiresAt !== 'number' || session.expiresAt <= now || !session.usuario) {
      clearSession();
      return null;
    }
    return session;
  } catch {
    clearSession();
    return null;
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(STORAGE_KEY);
}

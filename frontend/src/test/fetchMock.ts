import { vi } from 'vitest';

/** Simula uma resposta JSON da API. */
export function mockFetchResponse(status: number, body: unknown) {
  return vi.spyOn(globalThis, 'fetch').mockResolvedValue(
    new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } }),
  );
}

export function mockFetchNetworkFailure() {
  return vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'));
}

export const LOGIN_OK = {
  accessToken: 'token-jwt',
  tokenType: 'Bearer',
  expiresIn: 3600,
  usuario: { id: '3f0c5b0e-6a64-4b1e-9d52-2f1f0b6a9c11', nome: 'Maria Silva', email: 'maria@exemplo.com' },
};

export const INVALID_CREDENTIALS = {
  timestamp: '2026-11-02T12:00:00Z',
  status: 401,
  code: 'INVALID_CREDENTIALS',
  message: 'E-mail ou senha inválidos.',
  path: '/api/auth/login',
  traceId: 'abc123',
};

export const UNAUTHORIZED = {
  timestamp: '2026-11-02T12:00:00Z',
  status: 401,
  code: 'UNAUTHORIZED',
  message: 'Autenticação necessária.',
  path: '/api/auth/me',
  traceId: 'def456',
};

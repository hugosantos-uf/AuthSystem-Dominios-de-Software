import { ApiError, NETWORK_ERROR } from './ApiError';
import type { ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse, UserIdentity } from './types';

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

/** Única saída do frontend para a API: monta a requisição e converte falhas em {@link ApiError}. */
async function request<TResponse>(path: string, init: RequestInit): Promise<TResponse> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, init);
  } catch {
    throw new ApiError(0, NETWORK_ERROR, 'Não foi possível conectar ao servidor. Verifique se a API está em execução.');
  }

  const payload: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = payload as Partial<ErrorResponse> | null;
    throw new ApiError(
      response.status,
      error?.code ?? 'INTERNAL_ERROR',
      error?.message ?? 'Erro inesperado. Tente novamente.',
      error?.errors ?? [],
      error?.traceId,
    );
  }
  return payload as TResponse;
}

function postJson<TResponse>(path: string, body: unknown): Promise<TResponse> {
  return request<TResponse>(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  });
}

export function register(body: RegisterRequest): Promise<RegisterResponse> {
  return postJson<RegisterResponse>('/api/auth/register', body);
}

export function login(body: LoginRequest): Promise<LoginResponse> {
  return postJson<LoginResponse>('/api/auth/login', body);
}

/** Pede ao servidor que valide o token e devolva o dono dele. Token inválido ou expirado → ApiError 401. */
export function me(accessToken: string): Promise<UserIdentity> {
  return request<UserIdentity>('/api/auth/me', {
    method: 'GET',
    headers: { Authorization: `Bearer ${accessToken}` },
  });
}

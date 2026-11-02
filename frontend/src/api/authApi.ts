import { ApiError, NETWORK_ERROR } from './ApiError';
import type { ErrorResponse, LoginRequest, LoginResponse, RegisterRequest, RegisterResponse } from './types';

const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:8080').replace(/\/$/, '');

async function postJson<TResponse>(path: string, body: unknown): Promise<TResponse> {
  let response: Response;
  try {
    response = await fetch(`${API_URL}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
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

export function register(request: RegisterRequest): Promise<RegisterResponse> {
  return postJson<RegisterResponse>('/api/auth/register', request);
}

export function login(request: LoginRequest): Promise<LoginResponse> {
  return postJson<LoginResponse>('/api/auth/login', request);
}

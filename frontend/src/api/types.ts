/**
 * Contrato da API do backend (docs/ARQUITETURA.md, seção 5).
 * Os nomes dos campos seguem exatamente o JSON trocado com o servidor.
 */

export interface RegisterRequest {
  nome: string;
  email: string;
  senha: string;
  confirmarSenha: string;
}

export interface RegisterResponse {
  id: string;
  nome: string;
  email: string;
  criadoEm: string;
}

export interface LoginRequest {
  email: string;
  senha: string;
}

/** Identificação do usuário autenticado, devolvida no login. */
export interface UserIdentity {
  id: string;
  nome: string;
  email: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  usuario: UserIdentity;
}

export interface FieldErrorDetail {
  field: string;
  message: string;
}

/** Formato padrão de erro da API (docs/ARQUITETURA.md, seção 7.2). */
export interface ErrorResponse {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  traceId?: string;
  errors?: FieldErrorDetail[];
}

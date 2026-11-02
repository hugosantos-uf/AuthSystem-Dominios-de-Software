import type { FieldErrorDetail } from './types';

/** Código usado quando o servidor não pôde ser alcançado (sem resposta HTTP). */
export const NETWORK_ERROR = 'NETWORK_ERROR';

/**
 * Erro de chamada à API. A interface decide o que mostrar pelo {@link code},
 * nunca pelo texto da mensagem (ADR-06).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldErrorDetail[];
  readonly traceId?: string;

  constructor(status: number, code: string, message: string, fieldErrors: FieldErrorDetail[] = [], traceId?: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
    this.traceId = traceId;
  }
}

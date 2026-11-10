import { ApiError, NETWORK_ERROR } from './ApiError';

/** Mensagens exibidas ao usuário, escolhidas pelo código de erro da API (ADR-06). */
const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: 'E-mail ou senha inválidos.',
  UNAUTHORIZED: 'Sua sessão expirou. Entre novamente.',
  EMAIL_ALREADY_EXISTS: 'Este e-mail já está cadastrado.',
  VALIDATION_ERROR: 'Dados inválidos. Corrija os campos indicados.',
  [NETWORK_ERROR]: 'Não foi possível conectar ao servidor. Verifique se a API está em execução.',
};

const FALLBACK = 'Erro inesperado. Tente novamente.';

export function messageFor(error: unknown): string {
  if (!(error instanceof ApiError)) return FALLBACK;
  const message = MESSAGES[error.code] ?? FALLBACK;
  // Em erro interno, o traceId ajuda a localizar o problema no log do servidor.
  return error.code === 'INTERNAL_ERROR' && error.traceId ? `${message} (código: ${error.traceId})` : message;
}

/** Converte os erros por campo da API num mapa campo → mensagem. */
export function fieldErrorsFrom(error: unknown): Record<string, string> {
  if (!(error instanceof ApiError)) return {};
  return Object.fromEntries(error.fieldErrors.map((e) => [e.field, e.message]));
}

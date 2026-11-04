import { describe, expect, it, vi } from 'vitest';
import { INVALID_CREDENTIALS, LOGIN_OK, UNAUTHORIZED, mockFetchNetworkFailure, mockFetchResponse } from '../test/fetchMock';
import { ApiError, NETWORK_ERROR } from './ApiError';
import { login, me, register } from './authApi';
import { fieldErrorsFrom, messageFor } from './errorMessages';

describe('authApi', () => {
  it('CT-A01: login envia JSON para /api/auth/login e devolve a identidade', async () => {
    const fetchSpy = mockFetchResponse(200, LOGIN_OK);

    const response = await login({ email: 'maria@exemplo.com', senha: 'Senha@123' });

    expect(response.usuario.nome).toBe('Maria Silva');
    const [url, init] = fetchSpy.mock.calls[0]!;
    expect(url).toBe('http://localhost:8080/api/auth/login');
    expect(init?.method).toBe('POST');
    expect(init?.headers).toEqual({ 'Content-Type': 'application/json' });
    expect(JSON.parse(init?.body as string)).toEqual({ email: 'maria@exemplo.com', senha: 'Senha@123' });
  });

  it('CT-A02: erro da API vira ApiError com o código', async () => {
    mockFetchResponse(401, INVALID_CREDENTIALS);

    const error = await login({ email: 'x@x.com', senha: 'y' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 401, code: 'INVALID_CREDENTIALS' });
  });

  it('CT-A03: falha de rede vira NETWORK_ERROR', async () => {
    mockFetchNetworkFailure();

    await expect(register({ nome: 'a', email: 'b', senha: 'c', confirmarSenha: 'c' })).rejects.toMatchObject({
      code: NETWORK_ERROR,
    });
  });

  it('CT-A04: resposta de erro sem corpo JSON vira INTERNAL_ERROR', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response('Bad Gateway', { status: 502 }));

    await expect(login({ email: 'x@x.com', senha: 'y' })).rejects.toMatchObject({ status: 502, code: 'INTERNAL_ERROR' });
  });

  it('CT-A08: me envia o token no cabeçalho Authorization e devolve a identidade', async () => {
    const fetchSpy = mockFetchResponse(200, LOGIN_OK.usuario);

    const identity = await me('token-jwt');

    expect(identity).toEqual(LOGIN_OK.usuario);
    const [url, init] = fetchSpy.mock.calls[0]!;
    expect(url).toBe('http://localhost:8080/api/auth/me');
    expect(init?.method).toBe('GET');
    expect(init?.headers).toEqual({ Authorization: 'Bearer token-jwt' });
    expect(init?.body).toBeUndefined();
  });

  it('CT-A09: token recusado pelo servidor vira ApiError 401 UNAUTHORIZED', async () => {
    mockFetchResponse(401, UNAUTHORIZED);

    await expect(me('token-expirado')).rejects.toMatchObject({ status: 401, code: 'UNAUTHORIZED' });
  });
});

describe('errorMessages', () => {
  it('CT-A05: mensagem escolhida pelo código, não pelo texto do servidor', () => {
    expect(messageFor(new ApiError(409, 'EMAIL_ALREADY_EXISTS', 'qualquer texto'))).toBe('Este e-mail já está cadastrado.');
  });

  it('CT-A10: sessão recusada pelo servidor tem mensagem própria', () => {
    expect(messageFor(new ApiError(401, 'UNAUTHORIZED', 'Autenticação necessária.'))).toBe(
      'Sua sessão expirou. Entre novamente.',
    );
  });

  it('CT-A06: erro interno inclui o traceId', () => {
    expect(messageFor(new ApiError(500, 'INTERNAL_ERROR', '', [], 'a1b2'))).toContain('a1b2');
  });

  it('CT-A07: erros por campo viram mapa campo → mensagem', () => {
    const error = new ApiError(400, 'VALIDATION_ERROR', '', [{ field: 'senha', message: 'fraca' }]);
    expect(fieldErrorsFrom(error)).toEqual({ senha: 'fraca' });
  });
});

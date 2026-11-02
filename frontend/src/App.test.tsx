import type React from 'react';
import { render as rtlRender, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { App } from './App';
import { INVALID_CREDENTIALS, LOGIN_OK, mockFetchNetworkFailure, mockFetchResponse } from './test/fetchMock';
import { ToastProvider } from './toast/ToastProvider';

function render(ui: React.ReactElement) {
  return rtlRender(<ToastProvider>{ui}</ToastProvider>);
}

async function fillLogin(email: string, senha: string) {
  const user = userEvent.setup();
  await user.type(screen.getByLabelText('E-mail'), email);
  await user.type(screen.getByLabelText('Senha'), senha);
  await user.click(screen.getByRole('button', { name: 'Entrar' }));
  return user;
}

describe('Fluxo de login', () => {
  it('CT-F01: credenciais válidas mostram a identidade do usuário', async () => {
    mockFetchResponse(200, LOGIN_OK);
    render(<App />);

    await fillLogin('maria@exemplo.com', 'Senha@123');

    expect(await screen.findByRole('heading', { name: 'Olá, Maria!' })).toBeInTheDocument();
    expect(screen.getByText('maria@exemplo.com')).toBeInTheDocument();
    expect(screen.getByText(LOGIN_OK.usuario.id)).toBeInTheDocument();
  });

  it('CT-F02: credenciais inválidas mostram toast de erro genérico e limpam a senha', async () => {
    mockFetchResponse(401, INVALID_CREDENTIALS);
    render(<App />);

    await fillLogin('maria@exemplo.com', 'Errada@123');

    expect(await screen.findByRole('alert')).toHaveTextContent('E-mail ou senha inválidos.');
    expect(screen.getByLabelText('Senha')).toHaveValue('');
    expect(screen.queryByRole('heading', { name: 'Olá, Maria!' })).not.toBeInTheDocument();
  });

  it('CT-F03: campos vazios não chamam a API', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    render(<App />);

    await userEvent.setup().click(screen.getByRole('button', { name: 'Entrar' }));

    expect(screen.getByText('O e-mail é obrigatório.')).toBeInTheDocument();
    expect(screen.getByLabelText('E-mail')).toHaveAttribute('aria-invalid', 'true');
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('CT-F04: servidor fora do ar mostra toast de conexão', async () => {
    mockFetchNetworkFailure();
    render(<App />);

    await fillLogin('maria@exemplo.com', 'Senha@123');

    expect(await screen.findByRole('alert')).toHaveTextContent('Não foi possível conectar ao servidor');
  });

  it('CT-F05: sair encerra a sessão', async () => {
    mockFetchResponse(200, LOGIN_OK);
    render(<App />);
    const user = await fillLogin('maria@exemplo.com', 'Senha@123');

    await user.click(await screen.findByRole('button', { name: 'Sair' }));

    expect(screen.getByRole('heading', { name: 'Bem-vindo de volta' })).toBeInTheDocument();
    expect(sessionStorage.length).toBe(0);
  });
});

describe('Fluxo de cadastro', () => {
  async function fillRegister(confirmarSenha = 'Senha@123') {
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Cadastre-se' }));
    await user.type(screen.getByLabelText('Nome'), 'Maria Silva');
    await user.type(screen.getByLabelText('E-mail'), 'maria@exemplo.com');
    await user.type(screen.getByLabelText('Senha'), 'Senha@123');
    await user.type(screen.getByLabelText('Confirmar senha'), confirmarSenha);
    await user.click(screen.getByRole('button', { name: 'Cadastrar' }));
  }

  it('CT-F06: cadastro válido mostra toast de sucesso e leva ao login com o e-mail preenchido', async () => {
    mockFetchResponse(201, { id: '1', nome: 'Maria Silva', email: 'maria@exemplo.com', criadoEm: '2026-11-02T12:00:00Z' });
    render(<App />);

    await fillRegister();

    expect(await screen.findByRole('status')).toHaveTextContent('Cadastro realizado');
    expect(screen.getByLabelText('E-mail')).toHaveValue('maria@exemplo.com');
  });

  it('CT-F07: senhas diferentes são barradas no cliente', async () => {
    const fetchSpy = vi.spyOn(globalThis, 'fetch');
    render(<App />);

    await fillRegister('Outra@123');

    expect(screen.getByText('As senhas não coincidem.')).toBeInTheDocument();
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it('CT-F08: e-mail já cadastrado mostra o erro da API em toast', async () => {
    mockFetchResponse(409, { ...INVALID_CREDENTIALS, status: 409, code: 'EMAIL_ALREADY_EXISTS' });
    render(<App />);

    await fillRegister();

    expect(await screen.findByRole('alert')).toHaveTextContent('Este e-mail já está cadastrado.');
  });

  it('CT-F09: erros de campo do servidor aparecem no campo', async () => {
    mockFetchResponse(400, {
      ...INVALID_CREDENTIALS,
      status: 400,
      code: 'VALIDATION_ERROR',
      errors: [{ field: 'nome', message: 'Nome recusado pelo servidor.' }],
    });
    render(<App />);

    await fillRegister();

    expect(await screen.findByText('Nome recusado pelo servidor.')).toBeInTheDocument();
  });
});

describe('Sessão persistida', () => {
  it('CT-F10: sessão válida no sessionStorage abre direto a tela autenticada', () => {
    sessionStorage.setItem(
      'authsystem.session',
      JSON.stringify({ ...LOGIN_OK, expiresAt: Date.now() + 60_000 }),
    );
    render(<App />);

    expect(screen.getByRole('heading', { name: 'Olá, Maria!' })).toBeInTheDocument();
  });
});

describe('Interface', () => {
  it('CT-F11: botão de mostrar senha alterna a visibilidade', async () => {
    render(<App />);
    const user = userEvent.setup();
    const senha = screen.getByLabelText('Senha');

    await user.click(screen.getByRole('button', { name: 'Mostrar senha' }));
    expect(senha).toHaveAttribute('type', 'text');

    await user.click(screen.getByRole('button', { name: 'Ocultar senha' }));
    expect(senha).toHaveAttribute('type', 'password');
  });

  it('CT-F12: checklist de senha marca os requisitos atendidos', async () => {
    render(<App />);
    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: 'Cadastre-se' }));

    await user.type(screen.getByLabelText('Senha'), 'Senha@123');

    expect(screen.getByRole('list', { name: 'Requisitos da senha' })).not.toHaveTextContent('pendente');
  });
});

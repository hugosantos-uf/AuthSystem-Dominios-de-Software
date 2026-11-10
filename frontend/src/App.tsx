import { useEffect, useState } from 'react';
import { ApiError } from './api/ApiError';
import { me } from './api/authApi';
import { messageFor } from './api/errorMessages';
import type { LoginResponse, RegisterResponse } from './api/types';
import { AuthLayout } from './components/AuthLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { useToast } from './toast/ToastContext';
import { clearSession, createSession, loadSession, saveSession, type Session } from './session/session';

type View = { name: 'login'; email?: string } | { name: 'register' };

export function App() {
  const [restoredSession] = useState<Session | null>(loadSession);
  const [session, setSession] = useState<Session | null>(restoredSession);
  const [view, setView] = useState<View>({ name: 'login' });
  const toast = useToast();

  // Sessão vinda do sessionStorage: confirma com o servidor (GET /api/auth/me) que o token ainda vale.
  // Só um 401 encerra a sessão; se a API estiver fora do ar, a sessão local é mantida.
  useEffect(() => {
    if (!restoredSession) return;
    let active = true;
    me(restoredSession.accessToken).catch((error: unknown) => {
      if (!active || !(error instanceof ApiError) || error.status !== 401) return;
      clearSession();
      setSession(null);
      setView({ name: 'login' });
      toast.error(messageFor(error));
    });
    return () => {
      active = false;
    };
  }, [restoredSession, toast]);

  function handleAuthenticated(response: LoginResponse) {
    const created = createSession(response);
    saveSession(created);
    setSession(created);
  }

  function handleRegistered(response: RegisterResponse) {
    // ADR-07: o cadastro não autentica; o usuário segue para o login com o e-mail preenchido.
    setView({ name: 'login', email: response.email });
    toast.success('Cadastro realizado. Entre com seu e-mail e senha.');
  }

  function handleLogout() {
    clearSession();
    setSession(null);
    setView({ name: 'login' });
  }

  return <AuthLayout>{renderView()}</AuthLayout>;

  function renderView() {
    if (session) {
      return <HomePage session={session} onLogout={handleLogout} />;
    }
    if (view.name === 'register') {
      return <RegisterPage onRegistered={handleRegistered} onGoToLogin={() => setView({ name: 'login' })} />;
    }
    return (
      <LoginPage
        key={view.email ?? ''}
        initialEmail={view.email}
        onAuthenticated={handleAuthenticated}
        onGoToRegister={() => setView({ name: 'register' })}
      />
    );
  }
}

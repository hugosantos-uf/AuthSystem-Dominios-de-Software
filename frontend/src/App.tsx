import { useState } from 'react';
import type { LoginResponse, RegisterResponse } from './api/types';
import { AuthLayout } from './components/AuthLayout';
import { HomePage } from './pages/HomePage';
import { LoginPage } from './pages/LoginPage';
import { RegisterPage } from './pages/RegisterPage';
import { useToast } from './toast/ToastContext';
import { clearSession, createSession, loadSession, saveSession, type Session } from './session/session';

type View = { name: 'login'; email?: string } | { name: 'register' };

export function App() {
  const [session, setSession] = useState<Session | null>(() => loadSession());
  const [view, setView] = useState<View>({ name: 'login' });
  const toast = useToast();

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

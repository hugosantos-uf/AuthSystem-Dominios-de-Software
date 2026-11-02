import { useState, type FormEvent } from 'react';
import { login } from '../api/authApi';
import { fieldErrorsFrom, messageFor } from '../api/errorMessages';
import type { LoginResponse } from '../api/types';
import { LockIcon, MailIcon } from '../components/icons';
import { SubmitButton } from '../components/SubmitButton';
import { TextField } from '../components/TextField';
import { useToast } from '../toast/ToastContext';
import { hasErrors, validateLogin, type FieldErrors } from '../validation/rules';

interface LoginPageProps {
  initialEmail?: string;
  onAuthenticated: (response: LoginResponse) => void;
  onGoToRegister: () => void;
}

type LoginForm = { email: string; senha: string };

export function LoginPage({ initialEmail = '', onAuthenticated, onGoToRegister }: LoginPageProps) {
  const [form, setForm] = useState<LoginForm>({ email: initialEmail, senha: '' });
  const [errors, setErrors] = useState<FieldErrors<LoginForm>>({});
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function update(field: keyof LoginForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const validation = validateLogin(form);
    setErrors(validation);
    if (hasErrors(validation)) return;

    setSubmitting(true);
    try {
      onAuthenticated(await login({ email: form.email.trim(), senha: form.senha }));
    } catch (error) {
      setErrors(fieldErrorsFrom(error));
      toast.error(messageFor(error));
      // Nunca manter a senha preenchida após uma falha de autenticação.
      setForm((current) => ({ ...current, senha: '' }));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h1>Bem-vindo de volta</h1>
      <p className="subtitle">Entre com seu e-mail e senha para continuar.</p>
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          icon={<MailIcon />}
          autoFocus={!initialEmail}
          value={form.email}
          error={errors.email}
          onChange={(e) => update('email', e.target.value)}
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="current-password"
          placeholder="Sua senha"
          icon={<LockIcon />}
          revealable
          autoFocus={Boolean(initialEmail)}
          value={form.senha}
          error={errors.senha}
          onChange={(e) => update('senha', e.target.value)}
        />
        <SubmitButton loading={submitting} label="Entrar" loadingLabel="Entrando…" />
      </form>
      <p className="switch">
        Não tem conta?{' '}
        <button type="button" className="link" onClick={onGoToRegister}>
          Cadastre-se
        </button>
      </p>
    </section>
  );
}

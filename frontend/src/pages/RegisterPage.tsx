import { useState, type FormEvent } from 'react';
import { register } from '../api/authApi';
import { fieldErrorsFrom, messageFor } from '../api/errorMessages';
import type { RegisterResponse } from '../api/types';
import { LockIcon, MailIcon, UserIcon } from '../components/icons';
import { PasswordChecklist } from '../components/PasswordChecklist';
import { SubmitButton } from '../components/SubmitButton';
import { TextField } from '../components/TextField';
import { useToast } from '../toast/ToastContext';
import { hasErrors, validateRegister, type FieldErrors } from '../validation/rules';

interface RegisterPageProps {
  onRegistered: (response: RegisterResponse) => void;
  onGoToLogin: () => void;
}

type RegisterForm = { nome: string; email: string; senha: string; confirmarSenha: string };

const EMPTY_FORM: RegisterForm = { nome: '', email: '', senha: '', confirmarSenha: '' };

export function RegisterPage({ onRegistered, onGoToLogin }: RegisterPageProps) {
  const [form, setForm] = useState<RegisterForm>(EMPTY_FORM);
  const [errors, setErrors] = useState<FieldErrors<RegisterForm>>({});
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  function update(field: keyof RegisterForm, value: string) {
    setForm((current) => ({ ...current, [field]: value }));
    setErrors((current) => ({ ...current, [field]: undefined }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();

    const validation = validateRegister(form);
    setErrors(validation);
    if (hasErrors(validation)) return;

    setSubmitting(true);
    try {
      onRegistered(await register({ ...form, nome: form.nome.trim(), email: form.email.trim() }));
    } catch (error) {
      setErrors(fieldErrorsFrom(error));
      toast.error(messageFor(error));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <section>
      <h1>Crie sua conta</h1>
      <p className="subtitle">Leva menos de um minuto.</p>
      <form onSubmit={handleSubmit} noValidate>
        <TextField
          label="Nome"
          autoComplete="name"
          placeholder="Seu nome completo"
          icon={<UserIcon />}
          autoFocus
          value={form.nome}
          error={errors.nome}
          onChange={(e) => update('nome', e.target.value)}
        />
        <TextField
          label="E-mail"
          type="email"
          autoComplete="email"
          placeholder="voce@exemplo.com"
          icon={<MailIcon />}
          value={form.email}
          error={errors.email}
          onChange={(e) => update('email', e.target.value)}
        />
        <TextField
          label="Senha"
          type="password"
          autoComplete="new-password"
          placeholder="Crie uma senha forte"
          icon={<LockIcon />}
          revealable
          hint={<PasswordChecklist senha={form.senha} />}
          value={form.senha}
          error={errors.senha}
          onChange={(e) => update('senha', e.target.value)}
        />
        <TextField
          label="Confirmar senha"
          type="password"
          autoComplete="new-password"
          placeholder="Repita a senha"
          icon={<LockIcon />}
          revealable
          value={form.confirmarSenha}
          error={errors.confirmarSenha}
          onChange={(e) => update('confirmarSenha', e.target.value)}
        />
        <SubmitButton loading={submitting} label="Cadastrar" loadingLabel="Cadastrando…" />
      </form>
      <p className="switch">
        Já tem conta?{' '}
        <button type="button" className="link" onClick={onGoToLogin}>
          Entrar
        </button>
      </p>
    </section>
  );
}

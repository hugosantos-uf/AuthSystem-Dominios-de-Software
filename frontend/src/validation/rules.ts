/**
 * Validações no cliente, espelhando as regras de negócio do backend (RN01–RN04).
 * Servem apenas para dar retorno imediato ao usuário: a validação que vale é a do servidor.
 * As mensagens são as mesmas do backend, para a experiência ser consistente.
 */

export type FieldErrors<T> = Partial<Record<keyof T, string>>;

// Mesma ideia do @Email do Bean Validation: algo@algo, sem espaços.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+$/;
const PASSWORD_MIN = 8;
const PASSWORD_MAX = 72;
const NAME_MIN = 2;
const NAME_MAX = 100;
const EMAIL_MAX = 254;

export function validateEmail(email: string): string | undefined {
  const value = email.trim();
  if (!value) return 'O e-mail é obrigatório.';
  if (value.length > EMAIL_MAX) return 'O e-mail deve ter no máximo 254 caracteres.';
  if (!EMAIL_PATTERN.test(value)) return 'O e-mail informado é inválido.';
  return undefined;
}

export function validateName(nome: string): string | undefined {
  const value = nome.trim();
  if (!value) return 'O nome é obrigatório.';
  if (value.length < NAME_MIN || value.length > NAME_MAX) return 'O nome deve ter de 2 a 100 caracteres.';
  return undefined;
}

export interface PasswordCheck {
  label: string;
  ok: boolean;
}

/** Cada requisito da RN03, para exibir ao usuário o que falta enquanto ele digita. */
export function passwordChecks(senha: string): PasswordCheck[] {
  return [
    { label: 'De 8 a 72 caracteres', ok: senha.length >= PASSWORD_MIN && senha.length <= PASSWORD_MAX },
    { label: 'Uma letra maiúscula', ok: /\p{Lu}/u.test(senha) },
    { label: 'Uma letra minúscula', ok: /\p{Ll}/u.test(senha) },
    { label: 'Um número', ok: /\p{Nd}/u.test(senha) },
    // Igual ao StrongPasswordValidator: símbolo é o que não é letra, dígito nem espaço.
    { label: 'Um símbolo (ex.: @ # !)', ok: /[^\p{L}\p{Nd}\s]/u.test(senha) },
  ];
}

export function isStrongPassword(senha: string): boolean {
  return passwordChecks(senha).every((check) => check.ok);
}

export function validateLogin(values: { email: string; senha: string }): FieldErrors<typeof values> {
  const errors: FieldErrors<typeof values> = {};
  const email = validateEmail(values.email);
  if (email) errors.email = email;
  if (!values.senha) errors.senha = 'A senha é obrigatória.';
  return errors;
}

export function validateRegister(values: {
  nome: string;
  email: string;
  senha: string;
  confirmarSenha: string;
}): FieldErrors<typeof values> {
  const errors: FieldErrors<typeof values> = {};
  const nome = validateName(values.nome);
  if (nome) errors.nome = nome;
  const email = validateEmail(values.email);
  if (email) errors.email = email;

  if (!values.senha) {
    errors.senha = 'A senha é obrigatória.';
  } else if (!isStrongPassword(values.senha)) {
    errors.senha = 'A senha deve ter ao menos 8 caracteres, com maiúscula, minúscula, número e símbolo.';
  }

  if (!values.confirmarSenha) {
    errors.confirmarSenha = 'A confirmação de senha é obrigatória.';
  } else if (values.senha !== values.confirmarSenha) {
    errors.confirmarSenha = 'As senhas não coincidem.';
  }
  return errors;
}

export function hasErrors(errors: object): boolean {
  return Object.keys(errors).length > 0;
}

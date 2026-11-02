import { describe, expect, it } from 'vitest';
import { hasErrors, isStrongPassword, passwordChecks, validateLogin, validateRegister } from './rules';

const VALID = { nome: 'Maria Silva', email: 'maria@exemplo.com', senha: 'Senha@123', confirmarSenha: 'Senha@123' };

describe('validateLogin', () => {
  it('CT-V01: aceita e-mail e senha preenchidos', () => {
    expect(hasErrors(validateLogin({ email: 'maria@exemplo.com', senha: 'x' }))).toBe(false);
  });

  it('CT-V02: exige e-mail e senha', () => {
    expect(validateLogin({ email: ' ', senha: '' })).toEqual({
      email: 'O e-mail é obrigatório.',
      senha: 'A senha é obrigatória.',
    });
  });

  it('CT-V03: rejeita e-mail malformado', () => {
    expect(validateLogin({ email: 'maria.exemplo.com', senha: 'x' }).email).toBe('O e-mail informado é inválido.');
  });
});

describe('validateRegister', () => {
  it('CT-V04: aceita cadastro válido', () => {
    expect(hasErrors(validateRegister(VALID))).toBe(false);
  });

  it('CT-V05: nome com menos de 2 caracteres (RN01)', () => {
    expect(validateRegister({ ...VALID, nome: ' M ' }).nome).toBe('O nome deve ter de 2 a 100 caracteres.');
  });

  it('CT-V06: e-mail acima de 254 caracteres (RN02)', () => {
    const email = `${'a'.repeat(250)}@x.com`;
    expect(validateRegister({ ...VALID, email }).email).toBe('O e-mail deve ter no máximo 254 caracteres.');
  });

  it('CT-V07: senha fraca (RN03)', () => {
    expect(validateRegister({ ...VALID, senha: 'senha123', confirmarSenha: 'senha123' }).senha).toMatch(/ao menos 8/);
  });

  it('CT-V08: senha e confirmação diferentes (RN04)', () => {
    expect(validateRegister({ ...VALID, confirmarSenha: 'Outra@123' }).confirmarSenha).toBe('As senhas não coincidem.');
  });
});

describe('isStrongPassword (RN03, mesma regra do StrongPasswordValidator)', () => {
  it.each([
    ['Senha@123', true],
    ['Ação@2026', true],
    ['Sh@1', false],
    ['senha@123', false],
    ['SENHA@123', false],
    ['Senha@abc', false],
    ['Senha1234', false],
    ['Senha 1234', false],
    [`Aa1@${'x'.repeat(69)}`, false],
  ])('CT-V09: %s → %s', (senha, esperado) => {
    expect(isStrongPassword(senha)).toBe(esperado);
  });
});

describe('passwordChecks', () => {
  it('CT-V10: indica cada requisito atendido ou pendente', () => {
    expect(passwordChecks('abc').map((c) => c.ok)).toEqual([false, false, true, false, false]);
    expect(passwordChecks('Senha@123').every((c) => c.ok)).toBe(true);
  });
});

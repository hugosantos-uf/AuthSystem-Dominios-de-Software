import { describe, expect, it } from 'vitest';
import { LOGIN_OK } from '../test/fetchMock';
import { clearSession, createSession, loadSession, saveSession } from './session';

describe('session', () => {
  it('CT-S01: calcula a expiração a partir de expiresIn', () => {
    expect(createSession(LOGIN_OK, 1_000).expiresAt).toBe(1_000 + 3_600_000);
  });

  it('CT-S02: salva e recupera a sessão válida', () => {
    const session = createSession(LOGIN_OK, 0);
    saveSession(session);
    expect(loadSession(1_000)).toEqual(session);
  });

  it('CT-S03: descarta sessão expirada', () => {
    saveSession(createSession(LOGIN_OK, 0));
    expect(loadSession(3_600_000)).toBeNull();
    expect(sessionStorage.length).toBe(0);
  });

  it('CT-S04: descarta conteúdo corrompido', () => {
    sessionStorage.setItem('authsystem.session', '{não é json');
    expect(loadSession()).toBeNull();
  });

  it('CT-S05: clearSession remove a sessão', () => {
    saveSession(createSession(LOGIN_OK));
    clearSession();
    expect(loadSession()).toBeNull();
  });
});

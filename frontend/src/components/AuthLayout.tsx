import type { ReactNode } from 'react';
import { Logo } from './Logo';

/** Layout das telas: painel da marca à esquerda (some em telas pequenas) e conteúdo à direita. */
export function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <div className="auth-layout">
      <aside className="brand-panel">
        <div className="brand-content">
          <Logo variant="white" className="brand-logo" />
          <h2>AuthSystem</h2>
          <p>Autenticação de usuários · Domínios de Software</p>
          <ul className="brand-features">
            <li>Senhas protegidas com BCrypt</li>
            <li>Sessão com expiração automática</li>
            <li>Mensagens de erro claras</li>
          </ul>
        </div>
      </aside>
      <main className="form-panel">
        <div className="form-card">
          <Logo className="mobile-logo" />
          {children}
        </div>
      </main>
    </div>
  );
}

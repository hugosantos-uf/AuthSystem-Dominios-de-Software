import type { Session } from '../session/session';

interface HomePageProps {
  session: Session;
  onLogout: () => void;
}

function initials(nome: string): string {
  const parts = nome.trim().split(/\s+/);
  const first = parts[0]?.[0] ?? '';
  const last = parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : '';
  return (first + last).toUpperCase();
}

/** Tela exibida após a autenticação: mostra a identidade devolvida pela API. */
export function HomePage({ session, onLogout }: HomePageProps) {
  const { usuario } = session;
  return (
    <section>
      <div className="profile">
        <div className="avatar" aria-hidden="true">
          {initials(usuario.nome)}
        </div>
        <div>
          <span className="badge">Autenticado</span>
          <h1>Olá, {usuario.nome.split(' ')[0]}!</h1>
          <p className="subtitle">Suas credenciais foram verificadas com sucesso.</p>
        </div>
      </div>
      <dl className="identity">
        <dt>Nome</dt>
        <dd>{usuario.nome}</dd>
        <dt>E-mail</dt>
        <dd>{usuario.email}</dd>
        <dt>Identificador</dt>
        <dd>
          <code>{usuario.id}</code>
        </dd>
        <dt>Sessão válida até</dt>
        <dd>{new Date(session.expiresAt).toLocaleString('pt-BR')}</dd>
      </dl>
      <button type="button" className="secondary" onClick={onLogout}>
        Sair
      </button>
    </section>
  );
}

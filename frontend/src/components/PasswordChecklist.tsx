import { passwordChecks } from '../validation/rules';
import { CheckIcon, DotIcon } from './icons';

/** Lista dos requisitos da senha (RN03), marcando os já atendidos. */
export function PasswordChecklist({ senha }: { senha: string }) {
  return (
    <ul className="password-checklist" aria-label="Requisitos da senha">
      {passwordChecks(senha).map((check) => (
        <li key={check.label} className={check.ok ? 'ok' : undefined}>
          {check.ok ? <CheckIcon /> : <DotIcon />}
          <span>
            {check.label}
            <span className="sr-only">{check.ok ? ' (atendido)' : ' (pendente)'}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}

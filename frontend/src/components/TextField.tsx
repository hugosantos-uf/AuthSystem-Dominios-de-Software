import { useId, useState, type InputHTMLAttributes, type ReactNode } from 'react';
import { EyeIcon, EyeOffIcon } from './icons';

interface TextFieldProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'id'> {
  label: string;
  error?: string;
  hint?: ReactNode;
  icon?: ReactNode;
  /** Mostra o botão "mostrar/ocultar" (para campos de senha). */
  revealable?: boolean;
}

/** Campo de formulário com rótulo, ícone, dica e mensagem de erro ligados por ARIA. */
export function TextField({ label, error, hint, icon, revealable, type, ...inputProps }: TextFieldProps) {
  const id = useId();
  const [revealed, setRevealed] = useState(false);
  const hintId = hint ? `${id}-hint` : undefined;
  const errorId = error ? `${id}-error` : undefined;
  const describedBy = [hintId, errorId].filter(Boolean).join(' ') || undefined;

  return (
    <div className={`field${error ? ' field-invalid' : ''}`}>
      <label htmlFor={id}>{label}</label>
      <div className="input-wrap">
        {icon && <span className="input-icon">{icon}</span>}
        <input
          id={id}
          type={revealable && revealed ? 'text' : type}
          aria-invalid={error ? true : undefined}
          aria-describedby={describedBy}
          {...inputProps}
        />
        {revealable && (
          <button
            type="button"
            className="reveal"
            aria-label={revealed ? 'Ocultar senha' : 'Mostrar senha'}
            aria-pressed={revealed}
            onClick={() => setRevealed((r) => !r)}
          >
            {revealed ? <EyeOffIcon /> : <EyeIcon />}
          </button>
        )}
      </div>
      {hint && (
        <div id={hintId} className="field-hint">
          {hint}
        </div>
      )}
      {error && (
        <small id={errorId} className="field-error">
          {error}
        </small>
      )}
    </div>
  );
}

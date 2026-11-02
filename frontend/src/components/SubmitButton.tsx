interface SubmitButtonProps {
  loading: boolean;
  label: string;
  loadingLabel: string;
}

export function SubmitButton({ loading, label, loadingLabel }: SubmitButtonProps) {
  return (
    <button type="submit" className="primary" disabled={loading} aria-busy={loading}>
      {loading && <span className="spinner" aria-hidden="true" />}
      {loading ? loadingLabel : label}
    </button>
  );
}

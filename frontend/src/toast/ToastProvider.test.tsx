import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { useToast } from './ToastContext';
import { TOAST_DURATION_MS, ToastProvider } from './ToastProvider';

function Trigger() {
  const toast = useToast();
  return (
    <>
      <button onClick={() => toast.error('Falhou')}>erro</button>
      <button onClick={() => toast.success('Deu certo')}>sucesso</button>
    </>
  );
}

function renderWithProvider() {
  render(
    <ToastProvider>
      <Trigger />
    </ToastProvider>,
  );
}

describe('ToastProvider', () => {
  afterEach(() => vi.useRealTimers());

  it('CT-T01: erro aparece com role="alert" e sucesso com role="status"', async () => {
    renderWithProvider();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'erro' }));
    await user.click(screen.getByRole('button', { name: 'sucesso' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Falhou');
    expect(screen.getByRole('status')).toHaveTextContent('Deu certo');
  });

  it('CT-T02: toast some sozinho após o tempo definido', () => {
    vi.useFakeTimers();
    renderWithProvider();

    act(() => screen.getByRole('button', { name: 'erro' }).click());
    expect(screen.getByRole('alert')).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS));
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('CT-T03: botão fechar remove o toast', async () => {
    renderWithProvider();
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'erro' }));
    await user.click(screen.getByRole('button', { name: 'Fechar' }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('CT-T04: useToast fora do provider falha com mensagem clara', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {});
    expect(() => render(<Trigger />)).toThrow('useToast deve ser usado dentro de <ToastProvider>.');
  });
});

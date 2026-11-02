import { createContext, useContext } from 'react';

export type ToastKind = 'error' | 'success';

export interface ToastApi {
  error: (message: string) => void;
  success: (message: string) => void;
}

export const ToastContext = createContext<ToastApi | null>(null);

/** Acesso aos toasts. Só funciona dentro de um {@link ToastProvider}. */
export function useToast(): ToastApi {
  const toast = useContext(ToastContext);
  if (!toast) throw new Error('useToast deve ser usado dentro de <ToastProvider>.');
  return toast;
}

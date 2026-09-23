import { HttpErrorResponse } from '@angular/common/http';

export interface ApiError {
  status: number;
  message: string;
  erreurs?: Record<string, string>;
}

/** Message lisible à partir d'une erreur HTTP renvoyée par le backend Nafsiti. */
export function messageErreur(err: unknown, parDefaut = 'Une erreur est survenue. Veuillez réessayer.'): string {
  if (err instanceof HttpErrorResponse) {
    if (err.status === 0) {
      return 'Serveur injoignable. Vérifiez que le backend est démarré.';
    }
    const body = err.error as ApiError | null;
    if (body?.erreurs && Object.keys(body.erreurs).length > 0) {
      return Object.values(body.erreurs).join(' · ');
    }
    if (body?.message) {
      return body.message;
    }
  }
  return parDefaut;
}

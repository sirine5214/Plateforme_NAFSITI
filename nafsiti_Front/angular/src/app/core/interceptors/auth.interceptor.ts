import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

import { environment } from 'src/environments/environment';
import { AuthService } from '../services/auth.service';

/** Ajoute le JWT aux appels API et ferme la session si le backend le refuse (401). */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const estApi = req.url.startsWith(environment.apiUrl);
  const estAuthPublique = req.url.includes('/auth/login') || req.url.includes('/auth/register');

  const token = auth.token;
  const requete = estApi && token && !estAuthPublique ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;

  return next(requete).pipe(
    catchError((err: HttpErrorResponse) => {
      if (estApi && !estAuthPublique && !req.url.includes('/auth/logout') && err.status === 401) {
        auth.logout();
      }
      return throwError(() => err);
    })
  );
};

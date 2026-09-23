import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { Role } from '../models/utilisateur.model';
import { AuthService } from '../services/auth.service';

/** Pages réservées aux utilisateurs connectés. */
export const authGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  if (auth.estConnecte()) {
    return true;
  }
  auth.logout(false);
  return inject(Router).createUrlTree(['/login'], { queryParams: { returnUrl: state.url } });
};

/** Login / inscription : inutile si déjà connecté. */
export const guestGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.estConnecte() ? inject(Router).createUrlTree(['/analytics']) : true;
};

/** À utiliser avec `data: { roles: [...] }` sur la route. */
export const roleGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const roles = (route.data?.['roles'] as Role[] | undefined) ?? [];
  return roles.length === 0 || auth.aUnRole(roles) ? true : inject(Router).createUrlTree(['/analytics']);
};

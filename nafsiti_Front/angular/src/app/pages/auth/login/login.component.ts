import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { AuthService } from 'src/app/core/services/auth.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { AuthBrandComponent } from '../auth-brand.component';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterModule, AuthBrandComponent],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly soumis = signal(false);
  readonly afficherMotDePasse = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required]]
  });

  invalide(champ: 'email' | 'motDePasse'): boolean {
    const c = this.form.controls[champ];
    return c.invalid && (c.touched || this.soumis());
  }

  onSubmit() {
    this.soumis.set(true);
    this.erreur.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.chargement.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
        this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/analytics');
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Connexion impossible.'));
        this.chargement.set(false);
      }
    });
  }
}

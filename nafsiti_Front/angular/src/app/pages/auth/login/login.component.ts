import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';

import { AuthService } from 'src/app/core/services/auth.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { AuthResponse } from 'src/app/core/models/utilisateur.model';
import { CameraCaptureComponent } from 'src/app/theme/shared/components/camera-capture/camera-capture.component';
import { AuthBrandComponent } from '../auth-brand.component';

type Mode = 'mot-de-passe' | 'visage';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterModule, AuthBrandComponent, CameraCaptureComponent],
  templateUrl: './login.component.html'
})
export class LoginComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);
  private route = inject(ActivatedRoute);

  readonly mode = signal<Mode>('mot-de-passe');
  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly soumis = signal(false);
  readonly afficherMotDePasse = signal(false);
  /** Jeton intermédiaire : l'IA a jugé la connexion inhabituelle, le visage doit la confirmer. */
  readonly mfaToken = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    motDePasse: ['', [Validators.required]]
  });

  invalide(champ: 'email' | 'motDePasse'): boolean {
    const c = this.form.controls[champ];
    return c.invalid && (c.touched || this.soumis());
  }

  changerMode(mode: Mode) {
    this.mode.set(mode);
    this.erreur.set('');
    this.soumis.set(false);
  }

  /** Connexion manuelle : email + mot de passe. */
  onSubmit() {
    this.soumis.set(true);
    this.erreur.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.chargement.set(true);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: (r) => this.apresConnexion(r),
      error: (err) => this.echec(err, 'Connexion impossible.')
    });
  }

  /** Connexion par reconnaissance faciale : l'email désigne le compte, la capture le confirme. */
  connexionVisage(images: string[]) {
    this.soumis.set(true);
    this.erreur.set('');
    const email = this.form.controls.email;
    if (email.invalid) {
      email.markAsTouched();
      this.erreur.set("Saisissez d'abord votre adresse email.");
      return;
    }
    this.chargement.set(true);
    this.auth.loginVisage({ email: email.value, image: images[0] }).subscribe({
      next: (r) => this.apresConnexion(r),
      error: (err) => this.echec(err, 'Reconnaissance faciale impossible.')
    });
  }

  /** Second facteur demandé après un mot de passe correct. */
  validerMfa(images: string[]) {
    const token = this.mfaToken();
    if (!token) return;
    this.erreur.set('');
    this.chargement.set(true);
    this.auth.validerMfaVisage(token, images[0]).subscribe({
      next: (r) => this.apresConnexion(r),
      error: (err) => this.echec(err, 'Vérification impossible.')
    });
  }

  annulerMfa() {
    this.mfaToken.set(null);
    this.erreur.set('');
    this.form.controls.motDePasse.reset();
  }

  private apresConnexion(r: AuthResponse) {
    this.chargement.set(false);
    if (r.mfaRequis && r.mfaToken) {
      this.mfaToken.set(r.mfaToken);
      return;
    }
    const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
    this.router.navigateByUrl(returnUrl && returnUrl.startsWith('/') ? returnUrl : '/analytics');
  }

  private echec(err: unknown, parDefaut: string) {
    this.erreur.set(messageErreur(err, parDefaut));
    this.chargement.set(false);
  }
}

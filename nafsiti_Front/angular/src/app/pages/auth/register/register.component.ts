import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';

import { AuthService } from 'src/app/core/services/auth.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { PASSWORD_PATTERN, RegisterRequest } from 'src/app/core/models/utilisateur.model';
import { AuthBrandComponent } from '../auth-brand.component';

function motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
  const mdp = group.get('motDePasse')?.value;
  const confirmation = group.get('confirmation')?.value;
  return mdp && confirmation && mdp !== confirmation ? { differents: true } : null;
}

type Champ = 'nom' | 'prenom' | 'email' | 'motDePasse' | 'confirmation';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterModule, AuthBrandComponent],
  templateUrl: './register.component.html'
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private router = inject(Router);

  readonly chargement = signal(false);
  readonly erreur = signal('');
  readonly soumis = signal(false);
  readonly afficherMotDePasse = signal(false);

  readonly roles: { value: RegisterRequest['role']; label: string; description: string; icon: string }[] = [
    { value: 'PATIENT', label: 'Patient', description: 'Je souhaite prendre soin de mon bien-être', icon: 'icon-user' },
    { value: 'THERAPEUTE', label: 'Thérapeute', description: "J'accompagne des patients", icon: 'icon-award' }
  ];

  readonly form = this.fb.nonNullable.group(
    {
      prenom: ['', [Validators.required, Validators.maxLength(80)]],
      nom: ['', [Validators.required, Validators.maxLength(80)]],
      email: ['', [Validators.required, Validators.email]],
      role: this.fb.nonNullable.control<RegisterRequest['role']>('PATIENT'),
      motDePasse: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmation: ['', [Validators.required]]
    },
    { validators: motsDePasseIdentiques }
  );

  invalide(champ: Champ): boolean {
    const c = this.form.controls[champ];
    const touche = c.touched || this.soumis();
    if (champ === 'confirmation') {
      return touche && (c.invalid || this.form.hasError('differents'));
    }
    return touche && c.invalid;
  }

  /** Règles du mot de passe affichées en direct sous le champ. */
  regle(type: 'longueur' | 'lettre' | 'chiffre'): boolean {
    const v = this.form.controls.motDePasse.value;
    switch (type) {
      case 'longueur':
        return v.length >= 8;
      case 'lettre':
        return /[A-Za-z]/.test(v);
      case 'chiffre':
        return /\d/.test(v);
    }
  }

  onSubmit() {
    this.soumis.set(true);
    this.erreur.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { nom, prenom, email, role, motDePasse } = this.form.getRawValue();
    this.chargement.set(true);
    this.auth.register({ nom, prenom, email, role, motDePasse }).subscribe({
      next: () => this.router.navigate(['/analytics']),
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Inscription impossible.'));
        this.chargement.set(false);
      }
    });
  }
}

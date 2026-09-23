import { Component, OnInit, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';

import { AuthService } from 'src/app/core/services/auth.service';
import { UtilisateurService } from 'src/app/core/services/utilisateur.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { PASSWORD_PATTERN, ROLE_LABELS, initiales } from 'src/app/core/models/utilisateur.model';

type Message = { type: 'success' | 'danger'; texte: string } | null;

function motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
  const mdp = group.get('nouveauMotDePasse')?.value;
  const confirmation = group.get('confirmation')?.value;
  return mdp && confirmation && mdp !== confirmation ? { differents: true } : null;
}

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './profil.component.html'
})
export class ProfilComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private utilisateurService = inject(UtilisateurService);

  readonly utilisateur = this.auth.utilisateur;
  readonly roleLabels = ROLE_LABELS;
  readonly initiales = initiales;

  readonly enregistrementProfil = signal(false);
  readonly enregistrementMdp = signal(false);
  readonly messageProfil = signal<Message>(null);
  readonly messageMdp = signal<Message>(null);

  readonly profilForm = this.fb.nonNullable.group({
    prenom: ['', [Validators.required, Validators.maxLength(80)]],
    nom: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]]
  });

  readonly mdpForm = this.fb.nonNullable.group(
    {
      ancienMotDePasse: ['', Validators.required],
      nouveauMotDePasse: ['', [Validators.required, Validators.pattern(PASSWORD_PATTERN)]],
      confirmation: ['', Validators.required]
    },
    { validators: motsDePasseIdentiques }
  );

  ngOnInit() {
    this.remplirProfil();
    // Resynchronise avec le backend (le rôle ou l'email ont pu changer)
    this.auth.rafraichir().subscribe({ next: () => this.remplirProfil() });
  }

  invalideProfil(champ: 'prenom' | 'nom' | 'email'): boolean {
    const c = this.profilForm.controls[champ];
    return c.invalid && c.touched;
  }

  invalideMdp(champ: 'ancienMotDePasse' | 'nouveauMotDePasse' | 'confirmation'): boolean {
    const c = this.mdpForm.controls[champ];
    if (champ === 'confirmation') {
      return c.touched && (c.invalid || this.mdpForm.hasError('differents'));
    }
    return c.invalid && c.touched;
  }

  enregistrerProfil() {
    this.messageProfil.set(null);
    if (this.profilForm.invalid) {
      this.profilForm.markAllAsTouched();
      return;
    }
    this.enregistrementProfil.set(true);
    this.utilisateurService.modifierProfil(this.profilForm.getRawValue()).subscribe({
      next: (u) => {
        this.auth.majUtilisateur(u);
        this.profilForm.markAsPristine();
        this.messageProfil.set({ type: 'success', texte: 'Vos informations ont été mises à jour.' });
        this.enregistrementProfil.set(false);
      },
      error: (err) => {
        this.messageProfil.set({ type: 'danger', texte: messageErreur(err) });
        this.enregistrementProfil.set(false);
      }
    });
  }

  changerMotDePasse() {
    this.messageMdp.set(null);
    if (this.mdpForm.invalid) {
      this.mdpForm.markAllAsTouched();
      return;
    }
    const { ancienMotDePasse, nouveauMotDePasse } = this.mdpForm.getRawValue();
    this.enregistrementMdp.set(true);
    this.utilisateurService.changerMotDePasse({ ancienMotDePasse, nouveauMotDePasse }).subscribe({
      next: () => {
        this.mdpForm.reset();
        this.messageMdp.set({ type: 'success', texte: 'Votre mot de passe a été modifié.' });
        this.enregistrementMdp.set(false);
      },
      error: (err) => {
        this.messageMdp.set({ type: 'danger', texte: messageErreur(err) });
        this.enregistrementMdp.set(false);
      }
    });
  }

  private remplirProfil() {
    const u = this.utilisateur();
    if (u && this.profilForm.pristine) {
      this.profilForm.reset({ prenom: u.prenom, nom: u.nom, email: u.email });
    }
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';

import { PASSWORD_PATTERN, ROLE_LABELS, ROLES, Role, Utilisateur, UtilisateurRequest } from 'src/app/core/models/utilisateur.model';
import { UtilisateurService } from 'src/app/core/services/utilisateur.service';
import { messageErreur } from 'src/app/core/services/api-error';

type Champ = 'prenom' | 'nom' | 'email' | 'motDePasse' | 'role';

/** Modale de création / modification d'un utilisateur (administrateur). */
@Component({
  selector: 'app-utilisateur-form',
  imports: [ReactiveFormsModule],
  templateUrl: './utilisateur-form.component.html'
})
export class UtilisateurFormComponent implements OnInit {
  readonly modal = inject(NgbActiveModal);
  private fb = inject(FormBuilder);
  private utilisateurService = inject(UtilisateurService);

  /** Renseigné par l'appelant via `componentInstance` ; null = création. */
  utilisateur: Utilisateur | null = null;
  /** Id de l'administrateur connecté (il ne peut pas se retirer ses droits). */
  utilisateurConnecteId: number | null = null;

  readonly roles = ROLES;
  readonly roleLabels = ROLE_LABELS;
  readonly enregistrement = signal(false);
  readonly erreur = signal('');
  readonly soumis = signal(false);

  readonly form = this.fb.nonNullable.group({
    prenom: ['', [Validators.required, Validators.maxLength(80)]],
    nom: ['', [Validators.required, Validators.maxLength(80)]],
    email: ['', [Validators.required, Validators.email]],
    role: this.fb.nonNullable.control<Role>('PATIENT', Validators.required),
    actif: [true],
    motDePasse: ['', [Validators.pattern(PASSWORD_PATTERN)]]
  });

  get estEdition(): boolean {
    return this.utilisateur !== null;
  }

  get estSoiMeme(): boolean {
    return this.estEdition && this.utilisateur?.id === this.utilisateurConnecteId;
  }

  ngOnInit() {
    if (this.utilisateur) {
      const { prenom, nom, email, role, actif } = this.utilisateur;
      this.form.patchValue({ prenom, nom, email, role, actif });
    } else {
      this.form.controls.motDePasse.addValidators(Validators.required);
      this.form.controls.motDePasse.updateValueAndValidity();
    }
    if (this.estSoiMeme) {
      this.form.controls.role.disable();
      this.form.controls.actif.disable();
    }
  }

  invalide(champ: Champ): boolean {
    const c = this.form.controls[champ];
    return c.invalid && (c.touched || this.soumis());
  }

  enregistrer() {
    this.soumis.set(true);
    this.erreur.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req: UtilisateurRequest = {
      prenom: v.prenom,
      nom: v.nom,
      email: v.email,
      role: v.role,
      actif: v.actif,
      ...(v.motDePasse ? { motDePasse: v.motDePasse } : {})
    };
    this.enregistrement.set(true);
    const appel = this.utilisateur ? this.utilisateurService.modifier(this.utilisateur.id, req) : this.utilisateurService.creer(req);
    appel.subscribe({
      next: (u) => this.modal.close(u),
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.enregistrement.set(false);
      }
    });
  }
}

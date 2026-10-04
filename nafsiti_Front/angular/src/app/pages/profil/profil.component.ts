import { Component, OnInit, TemplateRef, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { AuthService } from 'src/app/core/services/auth.service';
import { UtilisateurService } from 'src/app/core/services/utilisateur.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { PASSWORD_PATTERN, ROLE_LABELS, VisageStatut, initiales } from 'src/app/core/models/utilisateur.model';
import { CameraCaptureComponent } from 'src/app/theme/shared/components/camera-capture/camera-capture.component';

type Message = { type: 'success' | 'danger'; texte: string } | null;

function motsDePasseIdentiques(group: AbstractControl): ValidationErrors | null {
  const mdp = group.get('nouveauMotDePasse')?.value;
  const confirmation = group.get('confirmation')?.value;
  return mdp && confirmation && mdp !== confirmation ? { differents: true } : null;
}

@Component({
  selector: 'app-profil',
  imports: [ReactiveFormsModule, DatePipe, CameraCaptureComponent],
  templateUrl: './profil.component.html'
})
export class ProfilComponent implements OnInit {
  private fb = inject(FormBuilder);
  private auth = inject(AuthService);
  private utilisateurService = inject(UtilisateurService);
  private modalService = inject(NgbModal);

  readonly utilisateur = this.auth.utilisateur;
  readonly roleLabels = ROLE_LABELS;
  readonly initiales = initiales;

  readonly enregistrementProfil = signal(false);
  readonly enregistrementMdp = signal(false);
  readonly messageProfil = signal<Message>(null);
  readonly messageMdp = signal<Message>(null);

  // Reconnaissance faciale
  readonly visage = signal<VisageStatut | null>(null);
  readonly enregistrementVisage = signal(false);
  readonly capturerVisage = signal(false);
  readonly messageVisage = signal<Message>(null);

  // Profil thérapeute (matching IA)
  readonly enregistrementPro = signal(false);
  readonly messagePro = signal<Message>(null);
  readonly proForm = this.fb.nonNullable.group({
    specialites: ['', Validators.maxLength(500)],
    approche: ['', Validators.maxLength(500)],
    langues: ['fr', [Validators.maxLength(50), Validators.pattern(/^$|^[a-zA-Z]{2}(\s*,\s*[a-zA-Z]{2})*$/)]]
  });

  // RGPD
  readonly messageRgpd = signal<Message>(null);
  readonly exportEnCours = signal(false);

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
    this.utilisateurService.statutVisage().subscribe({ next: (s) => this.visage.set(s) });
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

  // ===== Reconnaissance faciale =====

  /** Trois captures légèrement différentes donnent une empreinte de référence plus robuste. */
  enregistrerVisage(images: string[]) {
    this.messageVisage.set(null);
    this.enregistrementVisage.set(true);
    this.utilisateurService.enregistrerVisage(images).subscribe({
      next: (s) => {
        this.visage.set(s);
        this.capturerVisage.set(false);
        this.enregistrementVisage.set(false);
        this.messageVisage.set({ type: 'success', texte: 'Votre visage est enregistré : vous pouvez vous connecter avec la caméra.' });
      },
      error: (err) => {
        this.enregistrementVisage.set(false);
        this.messageVisage.set({ type: 'danger', texte: messageErreur(err, "Enregistrement du visage impossible.") });
      }
    });
  }

  supprimerVisage() {
    this.utilisateurService.supprimerVisage().subscribe({
      next: () => {
        this.visage.set({ enregistre: false, dateEnregistrement: null });
        this.messageVisage.set({ type: 'success', texte: 'Votre empreinte faciale a été supprimée.' });
      },
      error: (err) => this.messageVisage.set({ type: 'danger', texte: messageErreur(err) })
    });
  }

  // ===== Profil thérapeute =====

  enregistrerProfilPro() {
    this.messagePro.set(null);
    if (this.proForm.invalid) {
      this.proForm.markAllAsTouched();
      return;
    }
    this.enregistrementPro.set(true);
    this.utilisateurService.modifierProfilTherapeute(this.proForm.getRawValue()).subscribe({
      next: (u) => {
        this.auth.majUtilisateur(u);
        this.proForm.markAsPristine();
        this.enregistrementPro.set(false);
        this.messagePro.set({ type: 'success', texte: 'Votre profil professionnel a été mis à jour.' });
      },
      error: (err) => {
        this.enregistrementPro.set(false);
        this.messagePro.set({ type: 'danger', texte: messageErreur(err) });
      }
    });
  }

  // ===== RGPD =====

  changerPartageAlertes(partage: boolean) {
    this.utilisateurService.modifierConsentements(partage).subscribe({
      next: (u) => {
        this.auth.majUtilisateur(u);
        this.messageRgpd.set({
          type: 'success',
          texte: partage ? 'Vos thérapeutes seront prévenus en cas de signal de détresse.' : 'Le partage des alertes est désactivé.'
        });
      },
      error: (err) => this.messageRgpd.set({ type: 'danger', texte: messageErreur(err) })
    });
  }

  /** Droit d'accès / portabilité : téléchargement JSON de toutes ses données. */
  exporterDonnees() {
    this.exportEnCours.set(true);
    this.utilisateurService.exporterMesDonnees().subscribe({
      next: (donnees) => {
        const blob = new Blob([JSON.stringify(donnees, null, 2)], { type: 'application/json' });
        const lien = document.createElement('a');
        lien.href = URL.createObjectURL(blob);
        lien.download = `nafsiti-mes-donnees-${new Date().toISOString().substring(0, 10)}.json`;
        lien.click();
        URL.revokeObjectURL(lien.href);
        this.exportEnCours.set(false);
      },
      error: (err) => {
        this.exportEnCours.set(false);
        this.messageRgpd.set({ type: 'danger', texte: messageErreur(err) });
      }
    });
  }

  /** Droit à l'effacement : suppression définitive après confirmation. */
  supprimerCompte(modele: TemplateRef<unknown>) {
    this.modalService.open(modele, { centered: true }).closed.subscribe(() => {
      this.utilisateurService.supprimerMonCompte().subscribe({
        next: () => this.auth.logout(),
        error: (err) => this.messageRgpd.set({ type: 'danger', texte: messageErreur(err) })
      });
    });
  }

  private remplirProfil() {
    const u = this.utilisateur();
    if (u && this.profilForm.pristine) {
      this.profilForm.reset({ prenom: u.prenom, nom: u.nom, email: u.email });
    }
    if (u?.role === 'THERAPEUTE' && this.proForm.pristine) {
      this.proForm.reset({ specialites: u.specialites ?? '', approche: u.approche ?? '', langues: u.langues ?? 'fr' });
    }
  }
}

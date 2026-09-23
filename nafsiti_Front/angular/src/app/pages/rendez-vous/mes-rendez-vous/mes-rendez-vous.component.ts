import { Component, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { RendezVous, STATUT_RDV_LABELS, StatutRendezVous, estPasse } from 'src/app/core/models/rendez-vous.model';
import { initiales } from 'src/app/core/models/utilisateur.model';
import { RendezVousService } from 'src/app/core/services/rendez-vous.service';
import { AuthService } from 'src/app/core/services/auth.service';
import { messageErreur } from 'src/app/core/services/api-error';

type Filtre = 'A_VENIR' | StatutRendezVous | 'PASSES' | 'TOUS';

@Component({
  selector: 'app-mes-rendez-vous',
  imports: [DatePipe, LowerCasePipe, RouterLink],
  templateUrl: './mes-rendez-vous.component.html'
})
export class MesRendezVousComponent implements OnInit {
  private rdvService = inject(RendezVousService);
  private auth = inject(AuthService);
  private modalService = inject(NgbModal);

  readonly statutLabels = STATUT_RDV_LABELS;
  readonly initiales = initiales;
  readonly estPasse = estPasse;

  readonly role = computed(() => this.auth.utilisateur()?.role);
  readonly estPatient = computed(() => this.role() === 'PATIENT');
  readonly estTherapeute = computed(() => this.role() === 'THERAPEUTE');
  readonly estAdmin = computed(() => this.role() === 'ADMINISTRATEUR');

  readonly filtres: { valeur: Filtre; label: string }[] = [
    { valeur: 'A_VENIR', label: 'À venir' },
    { valeur: 'EN_ATTENTE', label: 'En attente' },
    { valeur: 'CONFIRME', label: 'Confirmés' },
    { valeur: 'ANNULE', label: 'Annulés' },
    { valeur: 'PASSES', label: 'Passés' },
    { valeur: 'TOUS', label: 'Tous' }
  ];

  readonly rendezVous = signal<RendezVous[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly filtre = signal<Filtre>('A_VENIR');
  readonly enCours = signal<number | null>(null);
  readonly notification = signal<{ type: 'success' | 'danger'; texte: string } | null>(null);

  readonly stats = computed(() => {
    const aVenir = this.rendezVous().filter((r) => !estPasse(r.dateHeure));
    return {
      aVenir: aVenir.filter((r) => r.statut !== 'ANNULE').length,
      enAttente: aVenir.filter((r) => r.statut === 'EN_ATTENTE').length,
      confirmes: aVenir.filter((r) => r.statut === 'CONFIRME').length,
      annules: this.rendezVous().filter((r) => r.statut === 'ANNULE').length
    };
  });

  readonly rendezVousFiltres = computed(() => {
    const liste = this.rendezVous();
    const f = this.filtre();
    switch (f) {
      case 'A_VENIR':
        // chronologique : le prochain en premier
        return liste.filter((r) => !estPasse(r.dateHeure) && r.statut !== 'ANNULE').reverse();
      case 'PASSES':
        return liste.filter((r) => estPasse(r.dateHeure) && r.statut !== 'ANNULE');
      case 'TOUS':
        return liste;
      default:
        return liste.filter((r) => r.statut === f);
    }
  });

  private timerNotification?: ReturnType<typeof setTimeout>;

  ngOnInit() {
    this.charger();
    if (history.state?.reserve) {
      this.notifier('success', 'Votre demande a été envoyée. Le thérapeute doit maintenant la confirmer.');
    }
  }

  charger() {
    this.chargement.set(true);
    this.erreur.set('');
    this.rdvService.lister().subscribe({
      next: (liste) => {
        this.rendezVous.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger les rendez-vous.'));
        this.chargement.set(false);
      }
    });
  }

  /** La personne en face : le thérapeute pour un patient, le patient sinon. */
  interlocuteur(r: RendezVous) {
    return this.estPatient() ? r.therapeute : r.patient;
  }

  peutConfirmer(r: RendezVous): boolean {
    return this.estTherapeute() && r.statut === 'EN_ATTENTE' && !estPasse(r.dateHeure);
  }

  peutAnnuler(r: RendezVous): boolean {
    return r.statut !== 'ANNULE' && !estPasse(r.dateHeure);
  }

  confirmer(r: RendezVous) {
    this.enCours.set(r.id);
    this.rdvService.confirmer(r.id).subscribe({
      next: (maj) => {
        this.remplacer(maj);
        this.notifier('success', `Rendez-vous avec ${maj.patient.prenom} ${maj.patient.nom} confirmé.`);
      },
      error: (err) => this.echec(err)
    });
  }

  demanderAnnulation(r: RendezVous, modele: TemplateRef<unknown>) {
    this.modalService.open(modele, { centered: true }).closed.subscribe(() => {
      this.enCours.set(r.id);
      this.rdvService.annuler(r.id).subscribe({
        next: (maj) => {
          this.remplacer(maj);
          this.notifier('success', 'Le rendez-vous a été annulé.');
        },
        error: (err) => this.echec(err)
      });
    });
  }

  private remplacer(maj: RendezVous) {
    this.rendezVous.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
    this.enCours.set(null);
  }

  private echec(err: unknown) {
    this.notifier('danger', messageErreur(err));
    this.enCours.set(null);
    this.charger();
  }

  private notifier(type: 'success' | 'danger', texte: string) {
    clearTimeout(this.timerNotification);
    this.notification.set({ type, texte });
    this.timerNotification = setTimeout(() => this.notification.set(null), 4000);
  }
}

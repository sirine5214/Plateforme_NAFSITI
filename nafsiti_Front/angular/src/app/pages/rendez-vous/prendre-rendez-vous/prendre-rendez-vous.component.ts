import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { Disponibilite, Therapeute, grouperParJour } from 'src/app/core/models/rendez-vous.model';
import { initiales } from 'src/app/core/models/utilisateur.model';
import { RendezVousService } from 'src/app/core/services/rendez-vous.service';
import { messageErreur } from 'src/app/core/services/api-error';

@Component({
  selector: 'app-prendre-rendez-vous',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './prendre-rendez-vous.component.html'
})
export class PrendreRendezVousComponent implements OnInit {
  private rdvService = inject(RendezVousService);
  private router = inject(Router);
  private fb = inject(FormBuilder);

  readonly initiales = initiales;

  readonly therapeutes = signal<Therapeute[]>([]);
  readonly chargementTherapeutes = signal(true);
  readonly therapeute = signal<Therapeute | null>(null);

  readonly creneaux = signal<Disponibilite[]>([]);
  readonly chargementCreneaux = signal(false);
  readonly creneau = signal<Disponibilite | null>(null);

  readonly erreur = signal('');
  readonly envoi = signal(false);

  readonly jours = computed(() => grouperParJour(this.creneaux()));

  readonly form = this.fb.nonNullable.group({
    motif: ['', Validators.maxLength(500)]
  });

  ngOnInit() {
    this.rdvService.therapeutes().subscribe({
      next: (liste) => {
        this.therapeutes.set(liste);
        this.chargementTherapeutes.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger les thérapeutes.'));
        this.chargementTherapeutes.set(false);
      }
    });
  }

  choisirTherapeute(t: Therapeute) {
    this.therapeute.set(t);
    this.creneau.set(null);
    this.erreur.set('');
    this.chargerCreneaux(t.id);
  }

  choisirCreneau(c: Disponibilite) {
    this.creneau.set(this.creneau()?.id === c.id ? null : c);
    this.erreur.set('');
  }

  reserver() {
    const c = this.creneau();
    if (!c || this.form.invalid) return;
    this.envoi.set(true);
    this.erreur.set('');
    this.rdvService.prendre({ disponibiliteId: c.id, motif: this.form.getRawValue().motif.trim() || undefined }).subscribe({
      next: () => this.router.navigate(['/rendez-vous'], { state: { reserve: true } }),
      error: (err) => {
        this.erreur.set(messageErreur(err, 'La réservation a échoué.'));
        this.envoi.set(false);
        this.creneau.set(null);
        // le créneau a pu être pris entre-temps : on rafraîchit
        const t = this.therapeute();
        if (t) this.chargerCreneaux(t.id);
      }
    });
  }

  private chargerCreneaux(therapeuteId: number) {
    this.chargementCreneaux.set(true);
    this.rdvService.creneauxLibres(therapeuteId).subscribe({
      next: (liste) => {
        this.creneaux.set(liste);
        this.chargementCreneaux.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger les créneaux.'));
        this.chargementCreneaux.set(false);
      }
    });
  }
}

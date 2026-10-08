import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { RouterLink } from '@angular/router';

import { Alerte, NIVEAU_RISQUE_LABELS, SOURCE_ALERTE_LABELS } from 'src/app/core/models/alerte.model';
import { initiales } from 'src/app/core/models/utilisateur.model';
import { AlerteService } from 'src/app/core/services/alerte.service';
import { AuthService } from 'src/app/core/services/auth.service';
import { TempsReelService } from 'src/app/core/services/temps-reel.service';
import { messageErreur } from 'src/app/core/services/api-error';

/** Module 6 — signaux de détresse à revoir par un professionnel (humain dans la boucle). */
@Component({
  selector: 'app-alertes',
  imports: [DatePipe, PercentPipe, RouterLink],
  templateUrl: './alertes.component.html'
})
export class AlertesComponent implements OnInit {
  private alerteService = inject(AlerteService);
  private tempsReel = inject(TempsReelService);
  private auth = inject(AuthService);
  private destroyRef = inject(DestroyRef);

  readonly initiales = initiales;
  readonly niveaux = NIVEAU_RISQUE_LABELS;
  readonly sources = SOURCE_ALERTE_LABELS;
  readonly estTherapeute = computed(() => this.auth.utilisateur()?.role === 'THERAPEUTE');

  readonly alertes = signal<Alerte[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly enCours = signal<number | null>(null);
  readonly aTraiter = computed(() => this.alertes().filter((a) => !a.traitee).length);

  ngOnInit() {
    this.charger();
    this.tempsReel
      .ecouter<Alerte>('ALERTE')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((a) => this.alertes.update((liste) => [a, ...liste.filter((x) => x.id !== a.id)]));
  }

  charger() {
    this.chargement.set(true);
    this.erreur.set('');
    this.alerteService.lister().subscribe({
      next: (liste) => {
        this.alertes.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger les alertes.'));
        this.chargement.set(false);
      }
    });
  }

  traiter(a: Alerte) {
    this.enCours.set(a.id);
    this.alerteService.traiter(a.id).subscribe({
      next: (maj) => {
        this.alertes.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
        this.enCours.set(null);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.enCours.set(null);
      }
    });
  }
}

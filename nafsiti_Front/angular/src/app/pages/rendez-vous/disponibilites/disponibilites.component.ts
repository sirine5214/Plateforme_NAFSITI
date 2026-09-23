import { Component, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import { Disponibilite, grouperParJour } from 'src/app/core/models/rendez-vous.model';
import { RendezVousService } from 'src/app/core/services/rendez-vous.service';
import { messageErreur } from 'src/app/core/services/api-error';

@Component({
  selector: 'app-disponibilites',
  imports: [DatePipe, ReactiveFormsModule, RouterLink],
  templateUrl: './disponibilites.component.html'
})
export class DisponibilitesComponent implements OnInit {
  private rdvService = inject(RendezVousService);
  private modalService = inject(NgbModal);
  private fb = inject(FormBuilder);

  readonly durees = [30, 45, 60, 90, 120];
  readonly aujourdHui = this.formatDate(new Date());

  readonly creneaux = signal<Disponibilite[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly erreurForm = signal('');
  readonly envoi = signal(false);
  readonly enCours = signal<number | null>(null);
  readonly notification = signal<{ type: 'success' | 'danger'; texte: string } | null>(null);

  readonly jours = computed(() => grouperParJour(this.creneaux()));
  readonly nbLibres = computed(() => this.creneaux().filter((c) => !c.reserve).length);
  readonly nbReserves = computed(() => this.creneaux().filter((c) => c.reserve).length);

  readonly form = this.fb.nonNullable.group({
    date: [this.aujourdHui, Validators.required],
    heure: ['09:00', Validators.required],
    dureeMinutes: [60, Validators.required]
  });

  private timerNotification?: ReturnType<typeof setTimeout>;

  ngOnInit() {
    this.charger();
  }

  charger() {
    this.chargement.set(true);
    this.erreur.set('');
    this.rdvService.mesDisponibilites().subscribe({
      next: (liste) => {
        this.creneaux.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger vos disponibilités.'));
        this.chargement.set(false);
      }
    });
  }

  ajouter() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { date, heure, dureeMinutes } = this.form.getRawValue();
    const debut = `${date}T${heure}:00`;
    if (new Date(debut).getTime() <= Date.now()) {
      this.erreurForm.set('Le créneau doit être dans le futur.');
      return;
    }
    this.envoi.set(true);
    this.erreurForm.set('');
    this.rdvService.ajouterDisponibilite({ debut, dureeMinutes: Number(dureeMinutes) }).subscribe({
      next: (c) => {
        this.creneaux.update((liste) => [...liste, c].sort((a, b) => a.debut.localeCompare(b.debut)));
        this.envoi.set(false);
        this.notifier('success', 'Créneau ajouté.');
        // propose directement le créneau suivant
        this.form.patchValue({ heure: c.fin.substring(11, 16) });
      },
      error: (err) => {
        this.erreurForm.set(messageErreur(err, "Impossible d'ajouter ce créneau."));
        this.envoi.set(false);
      }
    });
  }

  supprimer(c: Disponibilite, modele: TemplateRef<unknown>) {
    this.modalService.open(modele, { centered: true }).closed.subscribe(() => {
      this.enCours.set(c.id);
      this.rdvService.supprimerDisponibilite(c.id).subscribe({
        next: () => {
          this.creneaux.update((liste) => liste.filter((x) => x.id !== c.id));
          this.enCours.set(null);
          this.notifier('success', 'Créneau supprimé.');
        },
        error: (err) => {
          this.enCours.set(null);
          this.notifier('danger', messageErreur(err));
          this.charger();
        }
      });
    });
  }

  libelleDuree(minutes: number): string {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h === 0) return `${m} min`;
    return m ? `${h} h ${m}` : `${h} h`;
  }

  private formatDate(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private notifier(type: 'success' | 'danger', texte: string) {
    clearTimeout(this.timerNotification);
    this.notification.set({ type, texte });
    this.timerNotification = setTimeout(() => this.notification.set(null), 4000);
  }
}

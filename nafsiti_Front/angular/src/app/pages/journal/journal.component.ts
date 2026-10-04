import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApexOptions, NgApexchartsModule } from 'ng-apexcharts';

import {
  EntreeJournal,
  JournalCreation,
  RECOMMANDATIONS,
  STATUT_TENDANCE_LABELS,
  TAGS_EMOTION,
  Tendance
} from 'src/app/core/models/journal.model';
import { JournalService } from 'src/app/core/services/journal.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { UrgenceComponent } from 'src/app/theme/shared/components/urgence/urgence.component';

/** Module 3 — journal quotidien, analyse IA (valence, risque) et tendance de l'humeur. */
@Component({
  selector: 'app-journal',
  imports: [DatePipe, DecimalPipe, ReactiveFormsModule, RouterLink, NgApexchartsModule, UrgenceComponent],
  templateUrl: './journal.component.html'
})
export class JournalComponent implements OnInit {
  private journalService = inject(JournalService);
  private fb = inject(FormBuilder);

  readonly tagsDisponibles = TAGS_EMOTION;
  readonly recommandationsInfo = RECOMMANDATIONS;
  readonly statutLabels = STATUT_TENDANCE_LABELS;

  readonly entrees = signal<EntreeJournal[]>([]);
  readonly tendance = signal<Tendance | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly envoi = signal(false);
  readonly erreurForm = signal('');
  readonly resultat = signal<JournalCreation | null>(null);
  readonly tags = signal<string[]>([]);

  readonly form = this.fb.nonNullable.group({
    humeur: [6, [Validators.required, Validators.min(1), Validators.max(10)]],
    texte: ['', Validators.maxLength(5000)]
  });

  readonly graphique = computed<Partial<ApexOptions> | null>(() => {
    const points = this.tendance()?.points ?? [];
    if (points.length < 2) return null;
    return {
      chart: { type: 'line', height: 260, toolbar: { show: false }, fontFamily: 'Poppins, sans-serif', zoom: { enabled: false } },
      series: [
        { name: 'Humeur (/10)', data: points.map((p) => p.humeur) },
        // valence 1-5 ramenée sur 10 pour partager l'axe
        { name: 'Ton du texte (/10)', data: points.map((p) => (p.valence == null ? null : Math.round(p.valence * 20) / 10)) }
      ],
      xaxis: { categories: points.map((p) => new Date(p.date).toLocaleDateString('fr-FR', { day: '2-digit', month: '2-digit' })) },
      yaxis: { min: 0, max: 10, tickAmount: 5 },
      colors: ['#45735D', '#AABFB3'],
      stroke: { curve: 'smooth', width: [3, 2], dashArray: [0, 5] },
      markers: { size: 3 },
      dataLabels: { enabled: false },
      legend: { position: 'top' },
      grid: { borderColor: '#e1e8e4' },
      tooltip: { y: { formatter: (v: number) => (v == null ? '—' : `${v}/10`) } }
    };
  });

  ngOnInit() {
    this.charger();
  }

  charger() {
    this.chargement.set(true);
    this.erreur.set('');
    this.journalService.lister().subscribe({
      next: (liste) => {
        this.entrees.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger votre journal.'));
        this.chargement.set(false);
      }
    });
    this.journalService.tendance().subscribe({ next: (t) => this.tendance.set(t) });
  }

  basculerTag(tag: string) {
    this.tags.update((t) => (t.includes(tag) ? t.filter((x) => x !== tag) : [...t, tag]));
  }

  enregistrer() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { humeur, texte } = this.form.getRawValue();
    this.envoi.set(true);
    this.erreurForm.set('');
    this.resultat.set(null);
    this.journalService.creer({ humeur: Number(humeur), texte: texte.trim() || undefined, tags: this.tags() }).subscribe({
      next: (r) => {
        this.resultat.set(r);
        this.entrees.update((liste) => [r.entree, ...liste]);
        this.form.reset({ humeur: 6, texte: '' });
        this.tags.set([]);
        this.envoi.set(false);
        this.journalService.tendance().subscribe({ next: (t) => this.tendance.set(t) });
      },
      error: (err) => {
        this.erreurForm.set(messageErreur(err, "Impossible d'enregistrer cette entrée."));
        this.envoi.set(false);
      }
    });
  }

  supprimer(e: EntreeJournal) {
    this.journalService.supprimer(e.id).subscribe({
      next: () => this.entrees.update((liste) => liste.filter((x) => x.id !== e.id)),
      error: (err) => this.erreur.set(messageErreur(err))
    });
  }

  emoji(humeur: number): string {
    if (humeur <= 2) return '😢';
    if (humeur <= 4) return '😟';
    if (humeur <= 6) return '😐';
    if (humeur <= 8) return '🙂';
    return '😄';
  }

  /** Les liens internes passent par le routeur, les numéros d'urgence par tel:. */
  estLienInterne(lien: string): boolean {
    return lien.startsWith('/');
  }
}

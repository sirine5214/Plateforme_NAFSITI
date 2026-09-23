import { Component, DestroyRef, OnInit, computed, inject, signal } from '@angular/core';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ApexOptions, NgApexchartsModule } from 'ng-apexcharts';

import { Disponibilite, RendezVous, STATUT_RDV_LABELS, estPasse } from 'src/app/core/models/rendez-vous.model';
import { Utilisateur, UtilisateurStats, initiales } from 'src/app/core/models/utilisateur.model';
import { AuthService } from 'src/app/core/services/auth.service';
import { RendezVousService } from 'src/app/core/services/rendez-vous.service';
import { UtilisateurService } from 'src/app/core/services/utilisateur.service';
import { messageErreur } from 'src/app/core/services/api-error';

const VERT = '#45735D';
const SAUGE = '#6D8C7E';
const MENTHE = '#AABFB3';
const AMBRE = '#D9A441';
const ROUGE = '#B54848';

type PhaseRespiration = 'inspire' | 'expire';

@Component({
  selector: 'app-tableau-de-bord',
  imports: [DatePipe, LowerCasePipe, RouterLink, NgApexchartsModule],
  templateUrl: './tableau-de-bord.component.html'
})
export class TableauDeBordComponent implements OnInit {
  private auth = inject(AuthService);
  private rdvService = inject(RendezVousService);
  private utilisateurService = inject(UtilisateurService);
  private destroyRef = inject(DestroyRef);

  readonly statutLabels = STATUT_RDV_LABELS;
  readonly initiales = initiales;
  readonly maintenant = new Date();

  readonly utilisateur = this.auth.utilisateur;
  readonly role = computed(() => this.utilisateur()?.role);

  readonly rendezVous = signal<RendezVous[]>([]);
  readonly disponibilites = signal<Disponibilite[]>([]);
  readonly statsUtilisateurs = signal<UtilisateurStats | null>(null);
  readonly utilisateurs = signal<Utilisateur[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly enCours = signal<number | null>(null);

  // ===== Salutation =====

  readonly salutation = computed(() => (this.maintenant.getHours() < 18 ? 'Bonjour' : 'Bonsoir'));

  readonly accroche = computed(() => {
    switch (this.role()) {
      case 'PATIENT':
        return 'Prenez un moment pour vous. Chaque petit pas compte.';
      case 'THERAPEUTE':
        return 'Voici votre journée et les demandes de vos patients.';
      default:
        return "Vue d'ensemble de l'activité de la plateforme Nafsiti.";
    }
  });

  // ===== Rendez-vous dérivés =====

  /** Rendez-vous non annulés à venir, du plus proche au plus lointain. */
  readonly aVenir = computed(() =>
    this.rendezVous()
      .filter((r) => r.statut !== 'ANNULE' && !estPasse(r.dateHeure))
      .sort((a, b) => a.dateHeure.localeCompare(b.dateHeure))
  );

  readonly prochain = computed(() => this.aVenir()[0] ?? null);
  readonly enAttente = computed(() => this.aVenir().filter((r) => r.statut === 'EN_ATTENTE'));
  readonly confirmesAVenir = computed(() => this.aVenir().filter((r) => r.statut === 'CONFIRME'));
  readonly seancesPassees = computed(
    () => this.rendezVous().filter((r) => r.statut === 'CONFIRME' && estPasse(r.dateHeure)).length
  );

  readonly aujourdhui = computed(() => {
    const jour = this.cleJour(new Date());
    return this.aVenir().filter((r) => r.dateHeure.startsWith(jour));
  });

  readonly creneauxLibres = computed(() => this.disponibilites().filter((d) => !d.reserve).length);

  // ===== Graphiques =====

  /** Thérapeute : séances prévues sur les 7 prochains jours. */
  readonly graphiqueSemaine = computed<Partial<ApexOptions>>(() => {
    const jours = Array.from({ length: 7 }, (_, i) => {
      const d = new Date();
      d.setDate(d.getDate() + i);
      return d;
    });
    const compter = (statut: 'CONFIRME' | 'EN_ATTENTE') =>
      jours.map((d) => this.aVenir().filter((r) => r.statut === statut && r.dateHeure.startsWith(this.cleJour(d))).length);
    return {
      ...this.baseBarres(),
      series: [
        { name: 'Confirmés', data: compter('CONFIRME') },
        { name: 'En attente', data: compter('EN_ATTENTE') }
      ],
      colors: [VERT, AMBRE],
      xaxis: {
        categories: jours.map((d) => d.toLocaleDateString('fr-FR', { weekday: 'short', day: 'numeric' })),
        axisBorder: { show: false },
        axisTicks: { show: false }
      }
    };
  });

  /** Admin : répartition des rendez-vous par statut. */
  readonly graphiqueStatuts = computed<Partial<ApexOptions>>(() => {
    const liste = this.rendezVous();
    const n = (s: string) => liste.filter((r) => r.statut === s).length;
    return {
      chart: { type: 'donut', height: 280, fontFamily: 'Poppins, sans-serif' },
      series: [n('CONFIRME'), n('EN_ATTENTE'), n('ANNULE')],
      labels: ['Confirmés', 'En attente', 'Annulés'],
      colors: [VERT, AMBRE, ROUGE],
      legend: { position: 'bottom' },
      dataLabels: { enabled: false },
      stroke: { width: 3, colors: ['#fff'] },
      plotOptions: {
        pie: {
          donut: {
            size: '72%',
            labels: { show: true, total: { show: true, label: 'Rendez-vous', color: SAUGE } }
          }
        }
      }
    };
  });

  /** Admin : inscriptions des 6 derniers mois, par rôle. */
  readonly graphiqueInscriptions = computed<Partial<ApexOptions>>(() => {
    const mois = Array.from({ length: 6 }, (_, i) => {
      const d = new Date();
      d.setDate(1);
      d.setMonth(d.getMonth() - (5 - i));
      return d;
    });
    const compter = (role: string) =>
      mois.map(
        (m) =>
          this.utilisateurs().filter((u) => u.role === role && u.dateCreation?.startsWith(this.cleJour(m).substring(0, 7))).length
      );
    return {
      ...this.baseBarres(),
      series: [
        { name: 'Patients', data: compter('PATIENT') },
        { name: 'Thérapeutes', data: compter('THERAPEUTE') }
      ],
      colors: [VERT, MENTHE],
      xaxis: {
        categories: mois.map((m) => m.toLocaleDateString('fr-FR', { month: 'short' })),
        axisBorder: { show: false },
        axisTicks: { show: false }
      }
    };
  });

  // ===== Exercice de respiration (cohérence cardiaque : 5 s / 5 s) =====

  readonly respirationActive = signal(false);
  readonly phase = signal<PhaseRespiration>('inspire');
  readonly cycles = signal(0);
  private timerRespiration?: ReturnType<typeof setInterval>;

  ngOnInit() {
    this.charger();
    this.destroyRef.onDestroy(() => clearInterval(this.timerRespiration));
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
        this.erreur.set(messageErreur(err, 'Impossible de charger vos rendez-vous.'));
        this.chargement.set(false);
      }
    });
    if (this.role() === 'THERAPEUTE') {
      this.rdvService.mesDisponibilites().subscribe({ next: (d) => this.disponibilites.set(d) });
    }
    if (this.role() === 'ADMINISTRATEUR') {
      this.utilisateurService.statistiques().subscribe({ next: (s) => this.statsUtilisateurs.set(s) });
      this.utilisateurService.lister().subscribe({ next: (u) => this.utilisateurs.set(u) });
    }
  }

  confirmer(r: RendezVous) {
    this.enCours.set(r.id);
    this.rdvService.confirmer(r.id).subscribe({
      next: (maj) => {
        this.rendezVous.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
        this.enCours.set(null);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.enCours.set(null);
      }
    });
  }

  basculerRespiration() {
    if (this.respirationActive()) {
      clearInterval(this.timerRespiration);
      this.respirationActive.set(false);
      return;
    }
    this.phase.set('inspire');
    this.cycles.set(0);
    this.respirationActive.set(true);
    this.timerRespiration = setInterval(() => {
      if (this.phase() === 'inspire') {
        this.phase.set('expire');
      } else {
        this.phase.set('inspire');
        this.cycles.update((c) => c + 1);
      }
    }, 5000);
  }

  /** Nom de la personne en face dans un rendez-vous. */
  interlocuteur(r: RendezVous) {
    return this.role() === 'PATIENT' ? r.therapeute : r.patient;
  }

  private baseBarres(): Partial<ApexOptions> {
    return {
      chart: { type: 'bar', height: 260, stacked: true, toolbar: { show: false }, fontFamily: 'Poppins, sans-serif' },
      plotOptions: { bar: { borderRadius: 6, columnWidth: '42%' } },
      dataLabels: { enabled: false },
      legend: { position: 'top', horizontalAlign: 'right' },
      grid: { borderColor: '#e1e8e4', strokeDashArray: 4 },
      yaxis: { labels: { formatter: (v: number) => String(Math.round(v)) } },
      tooltip: { y: { formatter: (v: number) => String(v) } }
    };
  }

  private cleJour(d: Date): string {
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }
}

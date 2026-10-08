import { Injectable, computed, effect, inject, signal } from '@angular/core';
import { Observable, Subscription, catchError, forkJoin, of } from 'rxjs';

import { AuthService } from './auth.service';
import { AlerteService } from './alerte.service';
import { JournalService } from './journal.service';
import { MessagerieService } from './messagerie.service';
import { RendezVousService } from './rendez-vous.service';
import { RessourceService } from './ressource.service';
import { TempsReelService } from './temps-reel.service';
import { Alerte } from '../models/alerte.model';
import { EntreeJournal, Tendance } from '../models/journal.model';
import { Contact, MessageModeration } from '../models/messagerie.model';
import { RendezVous } from '../models/rendez-vous.model';
import { Recommandations } from '../models/ressource.model';

export interface Rappel {
  id: string;
  icone: string;
  titre: string;
  texte: string;
  lien: string;
  type: 'info' | 'attention' | 'bravo';
}

const CLE_IGNORES = 'nafsiti_rappels_ignores';
const CLE_NOTIFIES = 'nafsiti_rappels_notifies';
const INTERVALLE_MS = 10 * 60 * 1000;

/**
 * Notifications intelligentes : rappel de journal à l'heure où l'utilisateur ouvre habituellement l'application
 * (calculée par l'IA), rappels de séance, avis à donner, demandes à confirmer, alertes et encouragements.
 * Affichées dans la cloche de l'en-tête et, si l'utilisateur l'a autorisé, en notification du navigateur.
 */
@Injectable({ providedIn: 'root' })
export class RappelsService {
  private auth = inject(AuthService);
  private journal = inject(JournalService);
  private rdv = inject(RendezVousService);
  private ressources = inject(RessourceService);
  private messagerie = inject(MessagerieService);
  private alertes = inject(AlerteService);
  private tempsReel = inject(TempsReelService);

  private readonly tous = signal<Rappel[]>([]);
  private readonly ignores = signal<string[]>(this.lire(CLE_IGNORES));
  private timer?: ReturnType<typeof setInterval>;
  private abonnements: Subscription[] = [];
  private demarre = false;

  readonly rappels = computed(() => this.tous().filter((r) => !this.ignores().includes(r.id)));
  readonly nombre = computed(() => this.rappels().length);
  readonly notificationsNavigateur = signal(typeof Notification !== 'undefined' ? Notification.permission : 'denied');

  constructor() {
    effect(() => {
      if (!this.auth.estConnecte()) this.arreter();
    });
  }

  demarrer() {
    if (this.demarre) return;
    this.demarre = true;
    this.actualiser();
    this.timer = setInterval(() => this.actualiser(), INTERVALLE_MS);
    this.abonnements = [
      this.tempsReel.ecouter('MESSAGE').subscribe(() => this.actualiser()),
      this.tempsReel.ecouter('ALERTE').subscribe(() => this.actualiser())
    ];
  }

  ignorer(id: string) {
    this.ignores.update((liste) => [...liste, id]);
    this.ecrire(CLE_IGNORES, this.ignores());
  }

  toutIgnorer() {
    this.ignores.update((liste) => [...new Set([...liste, ...this.tous().map((r) => r.id)])]);
    this.ecrire(CLE_IGNORES, this.ignores());
  }

  async activerNotificationsNavigateur() {
    if (typeof Notification === 'undefined') return;
    this.notificationsNavigateur.set(await Notification.requestPermission());
  }

  actualiser() {
    const role = this.auth.utilisateur()?.role;
    if (!role || !this.auth.estConnecte()) return;
    const calcul: Observable<Rappel[]> =
      role === 'PATIENT' ? this.rappelsPatient() : role === 'THERAPEUTE' ? this.rappelsTherapeute() : this.rappelsAdmin();
    calcul.subscribe((rappels) => {
      this.tous.set(rappels);
      this.notifierNavigateur(rappels);
    });
  }

  private rappelsPatient(): Observable<Rappel[]> {
    return new Observable<Rappel[]>((obs) => {
      forkJoin({
        entrees: this.journal.lister().pipe(catchError(() => of([] as EntreeJournal[]))),
        rdv: this.rdv.lister().pipe(catchError(() => of([] as RendezVous[]))),
        reco: this.ressources.recommandations().pipe(catchError(() => of(null as Recommandations | null))),
        tendance: this.journal.tendance().pipe(catchError(() => of(null as Tendance | null))),
        contacts: this.messagerie.contacts().pipe(catchError(() => of([] as Contact[])))
      }).subscribe(({ entrees, rdv, reco, tendance, contacts }) => {
        const r: Rappel[] = [];
        const jour = this.jour();
        const heureRappel = reco?.heureRappel ?? 20;
        const ecritAujourdHui = entrees.some((e) => e.dateCreation.startsWith(jour));
        if (!ecritAujourdHui && new Date().getHours() >= heureRappel) {
          r.push({
            id: `journal-${jour}`, icone: 'icon-edit-3', type: 'info', lien: '/journal',
            titre: 'Comment allez-vous aujourd’hui ?', texte: 'Prenez une minute pour noter votre humeur dans votre journal.'
          });
        }
        r.push(...this.seancesProchaines(rdv, (x) => `${x.therapeute.prenom} ${x.therapeute.nom}`));
        rdv
          .filter((x) => x.statut === 'CONFIRME' && new Date(x.dateFin).getTime() < Date.now() && x.noteAvis == null)
          .slice(0, 2)
          .forEach((x) =>
            r.push({
              id: `avis-${x.id}`, icone: 'icon-star', type: 'info', lien: '/rendez-vous',
              titre: 'Votre avis compte', texte: `Comment s’est passée votre séance avec ${x.therapeute.prenom} ${x.therapeute.nom} ?`
            })
          );
        r.push(...this.messagesNonLus(contacts));
        if (tendance?.statut === 'amelioration') {
          r.push({
            id: `bravo-${jour}`, icone: 'icon-sun', type: 'bravo', lien: '/journal',
            titre: 'Bravo !', texte: 'Votre humeur s’améliore ces derniers jours. Continuez à prendre soin de vous.'
          });
        } else if (tendance?.baisseSignificative) {
          r.push({
            id: `soutien-${jour}`, icone: 'icon-heart', type: 'attention', lien: '/ressources',
            titre: 'Prenez soin de vous', texte: 'Votre humeur semble plus basse : quelques exercices pourraient vous aider.'
          });
        }
        obs.next(r);
        obs.complete();
      });
    });
  }

  private rappelsTherapeute(): Observable<Rappel[]> {
    return new Observable<Rappel[]>((obs) => {
      forkJoin({
        rdv: this.rdv.lister().pipe(catchError(() => of([] as RendezVous[]))),
        contacts: this.messagerie.contacts().pipe(catchError(() => of([] as Contact[]))),
        alertes: this.alertes.lister().pipe(catchError(() => of([] as Alerte[])))
      }).subscribe(({ rdv, contacts, alertes }) => {
        const r: Rappel[] = [];
        const enAttente = rdv.filter((x) => x.statut === 'EN_ATTENTE' && new Date(x.dateHeure).getTime() > Date.now()).length;
        if (enAttente > 0) {
          r.push({
            id: `attente-${enAttente}-${this.jour()}`, icone: 'icon-clock', type: 'info', lien: '/rendez-vous',
            titre: `${enAttente} demande${enAttente > 1 ? 's' : ''} à confirmer`, texte: 'Des patients attendent votre confirmation.'
          });
        }
        r.push(...this.alertesATraiter(alertes));
        r.push(...this.seancesProchaines(rdv, (x) => `${x.patient.prenom} ${x.patient.nom}`));
        r.push(...this.messagesNonLus(contacts));
        obs.next(r);
        obs.complete();
      });
    });
  }

  private rappelsAdmin(): Observable<Rappel[]> {
    return new Observable<Rappel[]>((obs) => {
      forkJoin({
        alertes: this.alertes.lister().pipe(catchError(() => of([] as Alerte[]))),
        moderation: this.messagerie.enAttente().pipe(catchError(() => of([] as MessageModeration[])))
      }).subscribe(({ alertes, moderation }) => {
        const r = this.alertesATraiter(alertes);
        if (moderation.length > 0) {
          r.push({
            id: `moderation-${moderation.length}-${this.jour()}`, icone: 'icon-shield', type: 'info', lien: '/moderation',
            titre: `${moderation.length} message${moderation.length > 1 ? 's' : ''} à relire`, texte: 'Messages masqués par la modération automatique.'
          });
        }
        obs.next(r);
        obs.complete();
      });
    });
  }

  private seancesProchaines(rdv: RendezVous[], avec: (x: RendezVous) => string): Rappel[] {
    const maintenant = Date.now();
    return rdv
      .filter((x) => x.statut === 'CONFIRME')
      .filter((x) => {
        const t = new Date(x.dateHeure).getTime();
        return t > maintenant && t - maintenant < 24 * 3600 * 1000;
      })
      .map((x) => ({
        id: `seance-${x.id}`, icone: 'icon-calendar', type: 'info' as const, lien: '/rendez-vous',
        titre: 'Séance bientôt',
        texte: `${new Date(x.dateHeure).toLocaleString('fr-FR', { weekday: 'long', hour: '2-digit', minute: '2-digit' })} avec ${avec(x)}`
      }));
  }

  private messagesNonLus(contacts: Contact[]): Rappel[] {
    const n = contacts.reduce((total, c) => total + c.nonLus, 0);
    return n > 0
      ? [{
          id: `messages-${n}-${this.jour()}`, icone: 'icon-message-circle', type: 'info', lien: '/messagerie',
          titre: `${n} message${n > 1 ? 's' : ''} non lu${n > 1 ? 's' : ''}`, texte: 'Ouvrez la messagerie pour les lire.'
        }]
      : [];
  }

  private alertesATraiter(alertes: Alerte[]): Rappel[] {
    const n = alertes.filter((a) => !a.traitee).length;
    return n > 0
      ? [{
          id: `alertes-${n}-${this.jour()}`, icone: 'icon-alert-triangle', type: 'attention', lien: '/alertes',
          titre: `${n} alerte${n > 1 ? 's' : ''} de détresse`, texte: 'Des patients ont peut-être besoin d’être contactés.'
        }]
      : [];
  }

  /** Notification du navigateur, une seule fois par rappel. */
  private notifierNavigateur(rappels: Rappel[]) {
    if (typeof Notification === 'undefined' || Notification.permission !== 'granted') return;
    const deja = this.lire(CLE_NOTIFIES);
    const nouveaux = rappels.filter((r) => !deja.includes(r.id) && !this.ignores().includes(r.id));
    nouveaux.forEach((r) => new Notification(r.titre, { body: r.texte, icon: 'favicon.png', tag: r.id }));
    if (nouveaux.length) this.ecrire(CLE_NOTIFIES, [...deja, ...nouveaux.map((r) => r.id)].slice(-200));
  }

  private arreter() {
    clearInterval(this.timer);
    this.abonnements.forEach((a) => a.unsubscribe());
    this.abonnements = [];
    this.demarre = false;
    this.tous.set([]);
  }

  private jour(): string {
    const d = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  }

  private lire(cle: string): string[] {
    try {
      return JSON.parse(localStorage.getItem(cle) ?? '[]') as string[];
    } catch {
      return [];
    }
  }

  private ecrire(cle: string, valeur: string[]) {
    try {
      localStorage.setItem(cle, JSON.stringify(valeur.slice(-200)));
    } catch {
      // stockage indisponible : les rappels réapparaîtront simplement
    }
  }
}

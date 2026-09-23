import { Component, DestroyRef, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { DatePipe, LowerCasePipe } from '@angular/common';
import { NgbModal, NgbPaginationModule } from '@ng-bootstrap/ng-bootstrap';
import { debounceTime } from 'rxjs';

import { ROLE_LABELS, ROLES, Role, Utilisateur, UtilisateurStats, initiales } from 'src/app/core/models/utilisateur.model';
import { UtilisateurService } from 'src/app/core/services/utilisateur.service';
import { AuthService } from 'src/app/core/services/auth.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { UtilisateurFormComponent } from '../utilisateur-form/utilisateur-form.component';

@Component({
  selector: 'app-utilisateur-list',
  imports: [ReactiveFormsModule, DatePipe, LowerCasePipe, NgbPaginationModule],
  templateUrl: './utilisateur-list.component.html'
})
export class UtilisateurListComponent implements OnInit {
  private utilisateurService = inject(UtilisateurService);
  private auth = inject(AuthService);
  private modalService = inject(NgbModal);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  readonly roles = ROLES;
  readonly roleLabels = ROLE_LABELS;
  readonly initiales = initiales;
  readonly taillePage = 8;

  readonly utilisateurs = signal<Utilisateur[]>([]);
  readonly stats = signal<UtilisateurStats | null>(null);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly notification = signal<{ type: 'success' | 'danger'; texte: string } | null>(null);
  readonly page = signal(1);
  readonly enCours = signal<number | null>(null);

  readonly utilisateursPage = computed(() => {
    const debut = (this.page() - 1) * this.taillePage;
    return this.utilisateurs().slice(debut, debut + this.taillePage);
  });

  readonly idConnecte = computed(() => this.auth.utilisateur()?.id ?? null);

  readonly filtres = this.fb.nonNullable.group({
    recherche: [''],
    role: this.fb.nonNullable.control<Role | ''>(''),
    actif: ['']
  });

  private timerNotification?: ReturnType<typeof setTimeout>;

  ngOnInit() {
    this.charger();
    this.filtres.valueChanges.pipe(debounceTime(300), takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      this.page.set(1);
      this.charger(false);
    });
  }

  charger(avecStats = true) {
    const { recherche, role, actif } = this.filtres.getRawValue();
    this.chargement.set(true);
    this.erreur.set('');
    this.utilisateurService.lister({ recherche, role, actif: actif === '' ? '' : actif === 'true' }).subscribe({
      next: (liste) => {
        this.utilisateurs.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger les utilisateurs.'));
        this.chargement.set(false);
      }
    });
    if (avecStats) {
      this.chargerStats();
    }
  }

  reinitialiserFiltres() {
    this.filtres.reset();
  }

  ouvrirFormulaire(utilisateur: Utilisateur | null = null) {
    const ref = this.modalService.open(UtilisateurFormComponent, { centered: true, size: 'lg', backdrop: 'static' });
    const form = ref.componentInstance as UtilisateurFormComponent;
    form.utilisateur = utilisateur;
    form.utilisateurConnecteId = this.idConnecte();
    ref.closed.subscribe((u: Utilisateur) => {
      this.notifier('success', utilisateur ? `${u.prenom} ${u.nom} a été modifié(e).` : `${u.prenom} ${u.nom} a été ajouté(e).`);
      if (u.id === this.idConnecte()) {
        this.auth.majUtilisateur(u);
      }
      this.charger();
    });
  }

  basculerStatut(u: Utilisateur) {
    this.enCours.set(u.id);
    this.utilisateurService.changerStatut(u.id, !u.actif).subscribe({
      next: (maj) => {
        this.utilisateurs.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
        this.notifier('success', `Compte de ${maj.prenom} ${maj.nom} ${maj.actif ? 'activé' : 'désactivé'}.`);
        this.enCours.set(null);
        this.chargerStats();
      },
      error: (err) => {
        this.notifier('danger', messageErreur(err));
        this.enCours.set(null);
      }
    });
  }

  confirmerSuppression(u: Utilisateur, modele: TemplateRef<unknown>) {
    this.modalService.open(modele, { centered: true }).closed.subscribe(() => {
      this.enCours.set(u.id);
      this.utilisateurService.supprimer(u.id).subscribe({
        next: () => {
          this.notifier('success', `${u.prenom} ${u.nom} a été supprimé(e).`);
          this.enCours.set(null);
          this.charger();
        },
        error: (err) => {
          this.notifier('danger', messageErreur(err));
          this.enCours.set(null);
        }
      });
    });
  }

  private chargerStats() {
    this.utilisateurService.statistiques().subscribe({ next: (s) => this.stats.set(s) });
  }

  private notifier(type: 'success' | 'danger', texte: string) {
    clearTimeout(this.timerNotification);
    this.notification.set({ type, texte });
    this.timerNotification = setTimeout(() => this.notification.set(null), 4000);
  }
}

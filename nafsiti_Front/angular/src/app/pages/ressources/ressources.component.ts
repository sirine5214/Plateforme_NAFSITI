import { Component, OnInit, TemplateRef, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgbModal } from '@ng-bootstrap/ng-bootstrap';

import {
  Recommandations,
  Ressource,
  TYPES_RESSOURCE,
  TYPE_RESSOURCE_LABELS,
  TypeRessource
} from 'src/app/core/models/ressource.model';
import { AuthService } from 'src/app/core/services/auth.service';
import { RessourceService } from 'src/app/core/services/ressource.service';
import { messageErreur } from 'src/app/core/services/api-error';

/** Module 4 — bibliothèque (respiration, méditation, articles) et recommandations IA. */
@Component({
  selector: 'app-ressources',
  imports: [ReactiveFormsModule],
  templateUrl: './ressources.component.html'
})
export class RessourcesComponent implements OnInit {
  private ressourceService = inject(RessourceService);
  private auth = inject(AuthService);
  private modalService = inject(NgbModal);
  private fb = inject(FormBuilder);

  readonly types = TYPES_RESSOURCE;
  readonly typeLabels = TYPE_RESSOURCE_LABELS;
  readonly estAdmin = this.auth.estAdmin;

  readonly ressources = signal<Ressource[]>([]);
  readonly recommandations = signal<Recommandations | null>(null);
  readonly filtre = signal<TypeRessource | ''>('');
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly ouverte = signal<Ressource | null>(null);

  readonly visibles = computed(() => {
    const f = this.filtre();
    return f ? this.ressources().filter((r) => r.type === f) : this.ressources();
  });

  // Administration
  readonly enEdition = signal<Ressource | null>(null);
  readonly erreurForm = signal('');
  readonly form = this.fb.nonNullable.group({
    titre: ['', [Validators.required, Validators.maxLength(150)]],
    description: ['', [Validators.required, Validators.maxLength(500)]],
    type: this.fb.nonNullable.control<TypeRessource>('RESPIRATION'),
    contenu: [''],
    dureeMinutes: this.fb.control<number | null>(5),
    url: ['', Validators.pattern(/^$|^https?:\/\/.+/)],
    valideParPro: [true]
  });

  ngOnInit() {
    this.charger();
  }

  charger() {
    this.chargement.set(true);
    this.erreur.set('');
    this.ressourceService.lister().subscribe({
      next: (liste) => {
        this.ressources.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger la bibliothèque.'));
        this.chargement.set(false);
      }
    });
    this.ressourceService.recommandations().subscribe({ next: (r) => this.recommandations.set(r) });
  }

  ouvrir(r: Ressource, modele: TemplateRef<unknown>) {
    this.ouverte.set(r);
    this.modalService.open(modele, { centered: true, size: 'lg', scrollable: true });
    this.ressourceService.consulter(r.id).subscribe({ next: (maj) => this.remplacer(maj) });
  }

  aimer(r: Ressource) {
    this.ressourceService.aimer(r.id, !r.aime).subscribe({ next: (maj) => this.remplacer(maj) });
  }

  libelleHeure(h: number): string {
    return `${String(h).padStart(2, '0')} h`;
  }

  // ===== Administration =====

  editer(r: Ressource | null, modele: TemplateRef<unknown>) {
    this.enEdition.set(r);
    this.erreurForm.set('');
    this.form.reset(
      r
        ? {
            titre: r.titre,
            description: r.description,
            type: r.type,
            contenu: r.contenu ?? '',
            dureeMinutes: r.dureeMinutes,
            url: r.url ?? '',
            valideParPro: r.valideParPro
          }
        : { titre: '', description: '', type: 'RESPIRATION', contenu: '', dureeMinutes: 5, url: '', valideParPro: true }
    );
    this.modalService.open(modele, { centered: true, size: 'lg' });
  }

  enregistrer(modal: { close: () => void }) {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const req = { ...v, dureeMinutes: v.dureeMinutes ? Number(v.dureeMinutes) : null };
    const r = this.enEdition();
    const appel = r ? this.ressourceService.modifier(r.id, req) : this.ressourceService.creer(req);
    appel.subscribe({
      next: (maj) => {
        if (r) {
          this.remplacer(maj);
        } else {
          this.ressources.update((liste) => [...liste, maj]);
        }
        modal.close();
      },
      error: (err) => this.erreurForm.set(messageErreur(err))
    });
  }

  supprimer(r: Ressource) {
    if (!confirm(`Supprimer « ${r.titre} » ?`)) return;
    this.ressourceService.supprimer(r.id).subscribe({
      next: () => this.ressources.update((liste) => liste.filter((x) => x.id !== r.id)),
      error: (err) => this.erreur.set(messageErreur(err))
    });
  }

  private remplacer(maj: Ressource) {
    this.ressources.update((liste) => liste.map((x) => (x.id === maj.id ? maj : x)));
    this.recommandations.update((reco) =>
      reco ? { ...reco, ressources: reco.ressources.map((x) => (x.id === maj.id ? maj : x)) } : reco
    );
    if (this.ouverte()?.id === maj.id) this.ouverte.set(maj);
  }
}

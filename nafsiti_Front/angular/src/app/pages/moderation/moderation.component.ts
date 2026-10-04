import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';

import { MessageModeration } from 'src/app/core/models/messagerie.model';
import { MessagerieService } from 'src/app/core/services/messagerie.service';
import { messageErreur } from 'src/app/core/services/api-error';

/** Module 5 — revue humaine des messages masqués par la modération automatique (administrateur). */
@Component({
  selector: 'app-moderation',
  imports: [DatePipe, PercentPipe],
  template: `
    <div class="nf-page">
      <div class="nf-page-header">
        <div>
          <h4 class="nf-page-title">Modération des messages</h4>
          <p class="nf-page-subtitle">Messages jugés ambigus par l'IA, en attente d'une décision humaine.</p>
        </div>
      </div>

      @if (erreur()) {
        <div class="alert alert-danger nf-alert" role="alert"><i class="feather icon-alert-circle"></i><span>{{ erreur() }}</span></div>
      }

      @if (chargement()) {
        <div class="text-muted"><span class="spinner-border spinner-border-sm me-2" aria-hidden="true"></span>Chargement…</div>
      } @else {
        @for (m of messages(); track m.id) {
          <div class="card nf-card mb-3">
            <div class="nf-card__body">
              <div class="d-flex flex-wrap gap-2 align-items-center mb-2 text-muted small">
                <strong class="text-body">{{ m.expediteur.prenom }} {{ m.expediteur.nom }}</strong>
                <i class="feather icon-arrow-right"></i>
                <span>{{ m.destinataire.prenom }} {{ m.destinataire.nom }}</span>
                <span>· {{ m.dateEnvoi | date: 'dd/MM/yyyy HH:mm' }}</span>
                @if (m.toxicite != null) {
                  <span class="nf-risque nf-risque--2 ms-auto">Toxicité {{ m.toxicite | percent }}</span>
                }
              </div>
              <blockquote class="nf-bulle mb-3">
                <p>{{ m.contenu }}</p>
              </blockquote>
              <div class="d-flex justify-content-end gap-2">
                <button type="button" class="btn btn-outline-danger" [disabled]="enCours() === m.id" (click)="decider(m, false)">Rejeter</button>
                <button type="button" class="btn btn-primary" [disabled]="enCours() === m.id" (click)="decider(m, true)">Publier</button>
              </div>
            </div>
          </div>
        } @empty {
          <div class="card nf-card">
            <div class="nf-placeholder"><i class="feather icon-check-circle"></i><p>Aucun message en attente de revue.</p></div>
          </div>
        }
      }
    </div>
  `
})
export class ModerationComponent implements OnInit {
  private messagerie = inject(MessagerieService);

  readonly messages = signal<MessageModeration[]>([]);
  readonly chargement = signal(true);
  readonly erreur = signal('');
  readonly enCours = signal<number | null>(null);

  ngOnInit() {
    this.messagerie.enAttente().subscribe({
      next: (liste) => {
        this.messages.set(liste);
        this.chargement.set(false);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.chargement.set(false);
      }
    });
  }

  decider(m: MessageModeration, publier: boolean) {
    this.enCours.set(m.id);
    this.messagerie.decider(m.id, publier).subscribe({
      next: () => {
        this.messages.update((liste) => liste.filter((x) => x.id !== m.id));
        this.enCours.set(null);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.enCours.set(null);
      }
    });
  }
}

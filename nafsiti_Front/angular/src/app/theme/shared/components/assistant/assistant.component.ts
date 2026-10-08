import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

import { ActionChatbot, ChatbotService } from 'src/app/core/services/chatbot.service';
import { messageErreur } from 'src/app/core/services/api-error';

interface Bulle {
  auteur: 'moi' | 'assistant';
  texte: string;
  actions?: ActionChatbot[];
  urgence?: boolean;
}

/** Chatbot d'orientation flottant : oriente vers les fonctionnalités, ne pose jamais de diagnostic. */
@Component({
  selector: 'app-assistant',
  imports: [FormsModule, RouterLink],
  template: `
    @if (ouvert()) {
      <section class="nf-assistant" role="dialog" aria-label="Assistant Nafsiti">
        <header class="nf-assistant__header">
          <span><i class="feather icon-message-square me-2"></i>Assistant Nafsiti</span>
          <button type="button" class="btn-close btn-close-white" aria-label="Fermer l'assistant" (click)="ouvert.set(false)"></button>
        </header>
        <div class="nf-assistant__fil" #fil aria-live="polite">
          @for (b of bulles(); track $index) {
            <div class="nf-bulle" [class.nf-bulle--moi]="b.auteur === 'moi'" [class.nf-bulle--urgence]="b.urgence">
              <p>{{ b.texte }}</p>
              @if (b.actions?.length) {
                <div class="nf-assistant__actions">
                  @for (a of b.actions; track a.lien) {
                    @if (a.lien.startsWith('/')) {
                      <a class="nf-assistant__action" [routerLink]="a.lien" (click)="ouvert.set(false)">{{ a.libelle }}</a>
                    } @else {
                      <a class="nf-assistant__action nf-assistant__action--urgence" [href]="a.lien">{{ a.libelle }}</a>
                    }
                  }
                </div>
              }
            </div>
          }
          @if (envoi()) {
            <div class="nf-bulle"><span class="spinner-border spinner-border-sm" aria-hidden="true"></span><span class="visually-hidden">L'assistant écrit…</span></div>
          }
        </div>
        <form class="nf-assistant__saisie" (ngSubmit)="envoyer()">
          <label for="assistant-message" class="visually-hidden">Votre question</label>
          <input id="assistant-message" class="form-control" name="message" [(ngModel)]="message" maxlength="1000"
            placeholder="Posez votre question…" autocomplete="off" />
          <button type="submit" class="btn btn-primary" [disabled]="envoi() || !message.trim()" aria-label="Envoyer">
            <i class="feather icon-arrow-right"></i>
          </button>
        </form>
        <p class="nf-assistant__mention">Assistant d'orientation, pas un professionnel de santé. Urgence : 3114 · 15 · 112</p>
      </section>
    }
    <button type="button" class="nf-assistant__bouton" (click)="basculer()" [attr.aria-expanded]="ouvert()"
      [attr.aria-label]="ouvert() ? 'Fermer l\\'assistant' : 'Ouvrir l\\'assistant'">
      <i class="feather" [class.icon-message-circle]="!ouvert()" [class.icon-x]="ouvert()"></i>
    </button>
  `
})
export class AssistantComponent {
  private chatbot = inject(ChatbotService);
  private readonly fil = viewChild<ElementRef<HTMLElement>>('fil');

  readonly ouvert = signal(false);
  readonly envoi = signal(false);
  readonly bulles = signal<Bulle[]>([
    {
      auteur: 'assistant',
      texte: 'Bonjour ! Je peux vous orienter : prendre rendez-vous, trouver un exercice de respiration, '
        + 'comprendre comment fonctionne Nafsiti… Que puis-je faire pour vous ?'
    }
  ]);
  message = '';

  basculer() {
    this.ouvert.update((o) => !o);
    this.defiler();
  }

  envoyer() {
    const texte = this.message.trim();
    if (!texte || this.envoi()) return;
    this.message = '';
    this.bulles.update((b) => [...b, { auteur: 'moi', texte }]);
    this.envoi.set(true);
    this.defiler();
    this.chatbot.envoyer(texte).subscribe({
      next: (r) => {
        this.bulles.update((b) => [...b, { auteur: 'assistant', texte: r.reponse, actions: r.actions, urgence: r.niveauRisque >= 2 }]);
        this.envoi.set(false);
        this.defiler();
      },
      error: (err) => {
        this.bulles.update((b) => [
          ...b,
          { auteur: 'assistant', texte: messageErreur(err, 'Je ne peux pas répondre pour le moment. En cas d’urgence, appelez le 3114.') }
        ]);
        this.envoi.set(false);
        this.defiler();
      }
    });
  }

  private defiler() {
    setTimeout(() => {
      const el = this.fil()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}

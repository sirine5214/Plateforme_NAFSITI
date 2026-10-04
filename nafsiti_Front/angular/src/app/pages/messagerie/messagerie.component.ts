import { Component, DestroyRef, ElementRef, OnInit, inject, signal, viewChild } from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';

import { Contact, Message } from 'src/app/core/models/messagerie.model';
import { initiales } from 'src/app/core/models/utilisateur.model';
import { AuthService } from 'src/app/core/services/auth.service';
import { MessagerieService } from 'src/app/core/services/messagerie.service';
import { TempsReelService } from 'src/app/core/services/temps-reel.service';
import { messageErreur } from 'src/app/core/services/api-error';
import { UrgenceComponent } from 'src/app/theme/shared/components/urgence/urgence.component';

/** Module 5 — messagerie sécurisée patient ↔ thérapeute, temps réel et modérée par l'IA. */
@Component({
  selector: 'app-messagerie',
  imports: [DatePipe, ReactiveFormsModule, UrgenceComponent],
  templateUrl: './messagerie.component.html'
})
export class MessagerieComponent implements OnInit {
  private messagerie = inject(MessagerieService);
  private tempsReel = inject(TempsReelService);
  private auth = inject(AuthService);
  private route = inject(ActivatedRoute);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  private readonly fil = viewChild<ElementRef<HTMLElement>>('fil');

  readonly initiales = initiales;
  readonly moi = this.auth.utilisateur;
  readonly enLigne = this.tempsReel.connecte;

  readonly contacts = signal<Contact[]>([]);
  readonly contact = signal<Contact | null>(null);
  readonly messages = signal<Message[]>([]);
  readonly chargement = signal(true);
  readonly chargementFil = signal(false);
  readonly envoi = signal(false);
  readonly erreur = signal('');
  readonly info = signal('');
  readonly urgence = signal(false);

  readonly form = this.fb.nonNullable.group({
    contenu: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  ngOnInit() {
    this.messagerie.contacts().subscribe({
      next: (liste) => {
        this.contacts.set(liste);
        this.chargement.set(false);
        const id = Number(this.route.snapshot.queryParamMap.get('contact'));
        const initial = liste.find((c) => c.personne.id === id) ?? (liste.length === 1 ? liste[0] : null);
        if (initial) this.ouvrir(initial);
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, 'Impossible de charger vos conversations.'));
        this.chargement.set(false);
      }
    });

    this.tempsReel
      .ecouter<Message>('MESSAGE')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((m) => this.recevoir(m));

    // Décision d'un modérateur sur l'un de mes messages masqués
    this.tempsReel
      .ecouter<Message>('MODERATION')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((m) => this.messages.update((liste) => liste.map((x) => (x.id === m.id ? m : x))));
  }

  ouvrir(c: Contact) {
    this.contact.set(c);
    this.erreur.set('');
    this.info.set('');
    this.urgence.set(false);
    this.chargementFil.set(true);
    this.messagerie.conversation(c.personne.id).subscribe({
      next: (liste) => {
        this.messages.set(liste);
        this.chargementFil.set(false);
        this.contacts.update((cs) => cs.map((x) => (x.personne.id === c.personne.id ? { ...x, nonLus: 0 } : x)));
        this.defiler();
      },
      error: (err) => {
        this.erreur.set(messageErreur(err));
        this.chargementFil.set(false);
      }
    });
  }

  envoyer() {
    const c = this.contact();
    const contenu = this.form.getRawValue().contenu.trim();
    if (!c || !contenu) return;
    this.envoi.set(true);
    this.erreur.set('');
    this.info.set('');
    this.messagerie.envoyer(c.personne.id, contenu).subscribe({
      next: (r) => {
        this.messages.update((liste) => [...liste, r.message]);
        this.form.reset();
        this.envoi.set(false);
        this.urgence.set(r.afficherUrgence);
        if (r.decision === 'MASQUER_EN_ATTENTE_REVUE') {
          this.info.set('Votre message sera transmis après relecture par un modérateur.');
        }
        this.defiler();
      },
      error: (err) => {
        this.erreur.set(messageErreur(err, "Le message n'a pas pu être envoyé."));
        this.envoi.set(false);
      }
    });
  }

  /** Entrée envoie, Maj+Entrée va à la ligne. */
  touche(e: KeyboardEvent) {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      this.envoyer();
    }
  }

  estAMoi(m: Message): boolean {
    return m.expediteurId === this.moi()?.id;
  }

  private recevoir(m: Message) {
    const c = this.contact();
    if (c && m.expediteurId === c.personne.id) {
      this.messages.update((liste) => [...liste, m]);
      this.defiler();
      // le fil est ouvert : on le recharge pour marquer comme lu côté serveur
      this.messagerie.conversation(c.personne.id).subscribe();
    } else {
      this.contacts.update((cs) => cs.map((x) => (x.personne.id === m.expediteurId ? { ...x, nonLus: x.nonLus + 1 } : x)));
    }
  }

  private defiler() {
    setTimeout(() => {
      const el = this.fil()?.nativeElement;
      if (el) el.scrollTop = el.scrollHeight;
    });
  }
}

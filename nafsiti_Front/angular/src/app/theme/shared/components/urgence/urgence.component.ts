import { Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Ressources d'urgence affichées dès qu'un signal de détresse est détecté.
 * Nafsiti ne prend pas en charge les urgences vitales : renvoi systématique vers les numéros d'urgence.
 */
@Component({
  selector: 'app-urgence',
  imports: [RouterLink],
  template: `
    <div class="nf-urgence" [class.nf-urgence--critique]="critique()" role="alert" aria-live="assertive">
      <div class="nf-urgence__icon"><i class="feather icon-heart"></i></div>
      <div class="nf-urgence__body">
        <h5>{{ critique() ? "Vous n'êtes pas seul·e. Parlez-en maintenant." : 'Prenez soin de vous' }}</h5>
        <p>
          {{
            critique()
              ? 'Ce que vous traversez semble très difficile. Des professionnels peuvent vous écouter, 24 h/24, gratuitement.'
              : 'Votre message laisse penser que vous traversez un moment difficile. Vous pouvez être aidé·e.'
          }}
        </p>
        <div class="nf-urgence__numeros">
          <a href="tel:3114" class="nf-urgence__numero"><strong>3114</strong> Prévention du suicide</a>
          <a href="tel:15" class="nf-urgence__numero"><strong>15</strong> SAMU</a>
          <a href="tel:112" class="nf-urgence__numero"><strong>112</strong> Urgences</a>
        </div>
        <div class="nf-urgence__actions">
          <a routerLink="/messagerie" class="btn btn-sm btn-light">Écrire à mon thérapeute</a>
          <a routerLink="/rendez-vous/prendre" class="btn btn-sm btn-light">Prendre rendez-vous</a>
        </div>
      </div>
    </div>
  `
})
export class UrgenceComponent {
  /** Niveau 3 : message plus direct. */
  readonly critique = input(false);
}

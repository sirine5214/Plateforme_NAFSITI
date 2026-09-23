import { Component } from '@angular/core';

/** Panneau de marque (logo + promesse) partagé par la connexion et l'inscription. */
@Component({
  selector: 'app-auth-brand',
  template: `
    <aside class="nf-auth-brand">
      <img src="assets/images/nafsiti-logo.png" alt="Nafsiti — Votre bien-être, toujours vivant" class="nf-auth-brand__logo" />
      <ul class="nf-auth-brand__points">
        <li>
          <span class="nf-auth-brand__icon"><i class="feather icon-book-open"></i></span>
          <div>
            <strong>Journal d'humeur</strong>
            <p>Notez vos émotions au quotidien et suivez leur évolution.</p>
          </div>
        </li>
        <li>
          <span class="nf-auth-brand__icon"><i class="feather icon-calendar"></i></span>
          <div>
            <strong>Rendez-vous</strong>
            <p>Planifiez vos séances avec un thérapeute en quelques clics.</p>
          </div>
        </li>
        <li>
          <span class="nf-auth-brand__icon"><i class="feather icon-heart"></i></span>
          <div>
            <strong>Ressources bien-être</strong>
            <p>Méditation, respiration et articles pour prendre soin de vous.</p>
          </div>
        </li>
      </ul>
    </aside>
  `
})
export class AuthBrandComponent {}

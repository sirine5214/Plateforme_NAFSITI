// angular import
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';

// bootstrap import
import { NgbDropdownConfig } from '@ng-bootstrap/ng-bootstrap';

// project import
import { SharedModule } from 'src/app/theme/shared/shared.module';
import { AuthService } from 'src/app/core/services/auth.service';
import { RappelsService } from 'src/app/core/services/rappels.service';
import { ROLE_LABELS, initiales } from 'src/app/core/models/utilisateur.model';

@Component({
  selector: 'app-nav-right',
  imports: [SharedModule, RouterModule],
  templateUrl: './nav-right.component.html',
  styleUrls: ['./nav-right.component.scss'],
  providers: [NgbDropdownConfig]
})
export class NavRightComponent {
  private auth = inject(AuthService);
  readonly rappels = inject(RappelsService);

  readonly utilisateur = this.auth.utilisateur;
  readonly estAdmin = this.auth.estAdmin;
  readonly roleLabels = ROLE_LABELS;
  readonly initiales = initiales;
  readonly nombreRappels = this.rappels.nombre;

  constructor() {
    this.rappels.demarrer();
  }

  deconnexion() {
    this.auth.logout();
  }
}

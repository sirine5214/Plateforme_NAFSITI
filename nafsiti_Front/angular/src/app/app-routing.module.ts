// Angular Import
import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

// project import
import { AdminComponent } from './theme/layout/admin/admin.component';
import { GuestComponent } from './theme/layout/guest/guest.component';
import { authGuard, guestGuard, roleGuard } from './core/guards/auth.guards';

const routes: Routes = [
  {
    path: '',
    component: AdminComponent,
    canActivate: [authGuard],
    canActivateChild: [roleGuard],
    children: [
      {
        path: '',
        redirectTo: '/analytics',
        pathMatch: 'full'
      },
      {
        path: 'analytics',
        loadComponent: () => import('./pages/tableau-de-bord/tableau-de-bord.component').then((c) => c.TableauDeBordComponent)
      },
      {
        path: 'utilisateurs',
        data: { roles: ['ADMINISTRATEUR'] },
        loadComponent: () =>
          import('./pages/utilisateurs/utilisateur-list/utilisateur-list.component').then((c) => c.UtilisateurListComponent)
      },
      {
        path: 'rendez-vous',
        loadComponent: () =>
          import('./pages/rendez-vous/mes-rendez-vous/mes-rendez-vous.component').then((c) => c.MesRendezVousComponent)
      },
      {
        path: 'rendez-vous/prendre',
        data: { roles: ['PATIENT'] },
        loadComponent: () =>
          import('./pages/rendez-vous/prendre-rendez-vous/prendre-rendez-vous.component').then((c) => c.PrendreRendezVousComponent)
      },
      {
        path: 'disponibilites',
        data: { roles: ['THERAPEUTE'] },
        loadComponent: () => import('./pages/rendez-vous/disponibilites/disponibilites.component').then((c) => c.DisponibilitesComponent)
      },
      {
        path: 'profil',
        loadComponent: () => import('./pages/profil/profil.component').then((c) => c.ProfilComponent)
      },
      {
        path: 'component',
        loadChildren: () => import('./demo/ui-element/ui-basic.module').then((m) => m.UiBasicModule)
      },
      {
        path: 'chart',
        loadComponent: () => import('./demo/chart-maps/core-apex.component').then((c) => c.CoreApexComponent)
      },
      {
        path: 'forms',
        loadComponent: () => import('./demo/forms/form-elements/form-elements.component').then((c) => c.FormElementsComponent)
      },
      {
        path: 'tables',
        loadComponent: () => import('./demo/tables/tbl-bootstrap/tbl-bootstrap.component').then((c) => c.TblBootstrapComponent)
      },
      {
        path: 'sample-page',
        loadComponent: () => import('./demo/other/sample-page/sample-page.component').then((c) => c.SamplePageComponent)
      }
    ]
  },
  {
    path: '',
    component: GuestComponent,
    canActivate: [guestGuard],
    children: [
      {
        path: 'register',
        loadComponent: () => import('./pages/auth/register/register.component').then((c) => c.RegisterComponent)
      },
      {
        path: 'login',
        loadComponent: () => import('./pages/auth/login/login.component').then((c) => c.LoginComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule {}

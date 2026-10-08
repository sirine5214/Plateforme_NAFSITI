import { Role } from 'src/app/core/models/utilisateur.model';

export interface NavigationItem {
  id: string;
  title: string;
  type: 'item' | 'collapse' | 'group';
  translate?: string;
  icon?: string;
  hidden?: boolean;
  url?: string;
  classes?: string;
  exactMatch?: boolean;
  external?: boolean;
  target?: boolean;
  breadcrumbs?: boolean;
  badge?: {
    title?: string;
    type?: string;
  };
  children?: NavigationItem[];
  /** Rôles autorisés à voir l'entrée (tous si absent). */
  roles?: Role[];
}

export const NavigationItems: NavigationItem[] = [
  {
    id: 'navigation',
    title: 'Navigation',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'dashboard',
        title: 'Tableau de bord',
        type: 'item',
        url: '/analytics',
        icon: 'feather icon-home'
      }
    ]
  },
  {
    id: 'bien-etre',
    title: 'Bien-être',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'journal',
        title: 'Mon journal',
        type: 'item',
        url: '/journal',
        classes: 'nav-item',
        icon: 'feather icon-edit-2',
        roles: ['PATIENT']
      },
      {
        id: 'ressources',
        title: 'Ressources',
        type: 'item',
        url: '/ressources',
        classes: 'nav-item',
        icon: 'feather icon-wind'
      },
      {
        id: 'messagerie',
        title: 'Messagerie',
        type: 'item',
        url: '/messagerie',
        classes: 'nav-item',
        icon: 'feather icon-message-circle',
        roles: ['PATIENT', 'THERAPEUTE']
      },
      {
        id: 'alertes',
        title: 'Alertes de détresse',
        type: 'item',
        url: '/alertes',
        classes: 'nav-item',
        icon: 'feather icon-alert-triangle',
        roles: ['THERAPEUTE', 'ADMINISTRATEUR']
      }
    ]
  },
  {
    id: 'rendez-vous-groupe',
    title: 'Rendez-vous',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'prendre-rendez-vous',
        title: 'Prendre un rendez-vous',
        type: 'item',
        url: '/rendez-vous/prendre',
        classes: 'nav-item',
        icon: 'feather icon-plus-circle',
        roles: ['PATIENT']
      },
      {
        id: 'mes-rendez-vous',
        title: 'Mes rendez-vous',
        type: 'item',
        url: '/rendez-vous',
        classes: 'nav-item',
        icon: 'feather icon-calendar',
        exactMatch: true,
        roles: ['PATIENT', 'THERAPEUTE']
      },
      {
        id: 'disponibilites',
        title: 'Mes disponibilités',
        type: 'item',
        url: '/disponibilites',
        classes: 'nav-item',
        icon: 'feather icon-clock',
        roles: ['THERAPEUTE']
      },
      {
        id: 'tous-rendez-vous',
        title: 'Tous les rendez-vous',
        type: 'item',
        url: '/rendez-vous',
        classes: 'nav-item',
        icon: 'feather icon-calendar',
        exactMatch: true,
        roles: ['ADMINISTRATEUR']
      }
    ]
  },
  {
    id: 'administration',
    title: 'Administration',
    type: 'group',
    icon: 'icon-group',
    roles: ['ADMINISTRATEUR'],
    children: [
      {
        id: 'utilisateurs',
        title: 'Gestion des utilisateurs',
        type: 'item',
        url: '/utilisateurs',
        classes: 'nav-item',
        icon: 'feather icon-users'
      },
      {
        id: 'moderation',
        title: 'Modération des messages',
        type: 'item',
        url: '/moderation',
        classes: 'nav-item',
        icon: 'feather icon-shield'
      }
    ]
  },
  {
    id: 'compte',
    title: 'Mon compte',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'profil',
        title: 'Mon profil',
        type: 'item',
        url: '/profil',
        classes: 'nav-item',
        icon: 'feather icon-user'
      }
    ]
  }
];

/** Retire les entrées non autorisées pour le rôle donné (récursif). */
export function filtrerNavigation(items: NavigationItem[], role: Role | undefined): NavigationItem[] {
  return items
    .filter((item) => !item.roles || (!!role && item.roles.includes(role)))
    .map((item) => (item.children ? { ...item, children: filtrerNavigation(item.children, role) } : item));
}

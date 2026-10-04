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
        icon: 'feather icon-edit-3',
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
  },
  {
    id: 'ui-component',
    title: 'Ui Component',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'basic',
        title: 'Component',
        type: 'collapse',
        icon: 'feather icon-box',
        children: [
          {
            id: 'button',
            title: 'Button',
            type: 'item',
            url: '/component/button'
          },
          {
            id: 'badges',
            title: 'Badges',
            type: 'item',
            url: '/component/badges'
          },
          {
            id: 'breadcrumb-pagination',
            title: 'Breadcrumb & Pagination',
            type: 'item',
            url: '/component/breadcrumb-paging'
          },
          {
            id: 'collapse',
            title: 'Collapse',
            type: 'item',
            url: '/component/collapse'
          },
          {
            id: 'tabs-pills',
            title: 'Tabs & Pills',
            type: 'item',
            url: '/component/tabs-pills'
          },
          {
            id: 'typography',
            title: 'Typography',
            type: 'item',
            url: '/component/typography'
          }
        ]
      }
    ]
  },
  {
    id: 'chart',
    title: 'Chart',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'apexchart',
        title: 'ApexChart',
        type: 'item',
        url: '/chart',
        classes: 'nav-item',
        icon: 'feather icon-pie-chart'
      }
    ]
  },
  {
    id: 'forms & tables',
    title: 'Forms & Tables',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'forms',
        title: 'Basic Forms',
        type: 'item',
        url: '/forms',
        classes: 'nav-item',
        icon: 'feather icon-file-text'
      },
      {
        id: 'tables',
        title: 'Tables',
        type: 'item',
        url: '/tables',
        classes: 'nav-item',
        icon: 'feather icon-server'
      }
    ]
  },
  {
    id: 'other',
    title: 'Other',
    type: 'group',
    icon: 'icon-group',
    children: [
      {
        id: 'sample-page',
        title: 'Sample Page',
        type: 'item',
        url: '/sample-page',
        classes: 'nav-item',
        icon: 'feather icon-sidebar'
      },
      {
        id: 'menu-level',
        title: 'Menu Levels',
        type: 'collapse',
        icon: 'feather icon-menu',
        children: [
          {
            id: 'menu-level-2.1',
            title: 'Menu Level 2.1',
            type: 'item',
            url: 'javascript:void(0)',
            external: true
          },
          {
            id: 'menu-level-2.2',
            title: 'Menu Level 2.2',
            type: 'collapse',
            children: [
              {
                id: 'menu-level-2.2.1',
                title: 'Menu Level 2.2.1',
                type: 'item',
                url: 'javascript:void(0)',
                external: true
              },
              {
                id: 'menu-level-2.2.2',
                title: 'Menu Level 2.2.2',
                type: 'item',
                url: 'javascript:void(0)',
                external: true
              }
            ]
          }
        ]
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

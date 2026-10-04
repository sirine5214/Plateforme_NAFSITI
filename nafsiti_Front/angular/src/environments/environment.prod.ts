import packageInfo from '../../package.json';

export const environment = {
  appVersion: packageInfo.version,
  production: true,
  apiUrl: 'http://localhost:8080/api',
  /** Canal temps réel (messagerie, alertes) */
  wsUrl: 'ws://localhost:8080/ws'
};

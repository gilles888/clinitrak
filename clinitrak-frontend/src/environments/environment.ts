/**
 * Configuration de l'environnement de developpement.
 *
 * Le proxy Angular (proxy.conf.json) redirige /api → http://localhost:8080 (gateway).
 */
export const environment = {
  production: false,
  apiBaseUrl: '/api',
  notificationStreamUrl: 'http://localhost:8090/api/v1/notifications/stream',
  tenantId: 'saintluc',
  appVersion: '1.0.0-dev',
  features: {
    darkMode: true,
    exportExcel: true,
    sseNotifications: true,
  },
};

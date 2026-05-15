/**
 * Configuration de l'environnement de production.
 */
export const environment = {
  production: true,
  apiBaseUrl: '/api',
  notificationStreamUrl: '/api/v1/notifications/stream',
  tenantId: '',
  appVersion: '1.0.0',
  features: {
    darkMode: true,
    exportExcel: true,
    sseNotifications: true,
  },
};

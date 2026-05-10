/**
 * Configuration de l'environnement de développement.
 *
 * Le proxy Angular (proxy.conf.json) redirige /api → http://localhost:8080 (gateway).
 */
export const environment = {
  production: false,
  apiBaseUrl: '/api',
  tenantId: 'saintluc',  // Tenant de dev par défaut
  appVersion: '1.0.0-dev',
};

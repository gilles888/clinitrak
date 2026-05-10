import { ApplicationConfig, inject, provideAppInitializer } from '@angular/core';
import { provideRouter, withRouterConfig } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { authInterceptor } from './core/interceptors/auth.interceptor';
import { tenantInterceptor } from './core/interceptors/tenant.interceptor';
import { AuthService } from './core/services/auth.service';

/**
 * Configuration principale de l'application Angular 20.
 *
 * <p>Utilise la nouvelle API {@code ApplicationConfig} (standalone, sans NgModule).
 * Les intercepteurs sont déclarés comme fonctions (functional interceptors).
 */
export const appConfig: ApplicationConfig = {
  providers: [
    // Router avec title strategy activée
    provideRouter(
      routes,
      withRouterConfig({ paramsInheritanceStrategy: 'always' })
    ),

    // HttpClient avec intercepteurs fonctionnels (ordre important : tenant → auth)
    provideHttpClient(
      withInterceptors([tenantInterceptor, authInterceptor])
    ),

    // PrimeNG animations
    provideAnimationsAsync(),

    // Initialisation : restauration de la session depuis localStorage
    provideAppInitializer(() => {
      const authService = inject(AuthService);
      authService.initFromStorage();
    }),
  ],
};

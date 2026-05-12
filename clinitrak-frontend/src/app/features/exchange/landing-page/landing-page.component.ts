import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { CardModule } from 'primeng/card';
import { ButtonModule } from 'primeng/button';

/**
 * Page d'accueil publique du portail Exchange.
 *
 * <p>Présente les avantages du portail de soumission clinique et invite
 * les utilisateurs externes à s'inscrire ou se connecter.
 * Layout autonome (pas de sidebar ni topbar interne).
 */
@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [RouterLink, CardModule, ButtonModule],
  template: `
    <div class="tw-min-h-screen tw-bg-gradient-to-b tw-from-blue-50 tw-to-white">

      <!-- Header -->
      <header class="tw-bg-white tw-shadow-sm tw-px-8 tw-py-4 tw-flex tw-justify-between tw-items-center">
        <h1 class="tw-text-2xl tw-font-bold tw-text-blue-700">CliniTrak Exchange</h1>
        <div class="tw-flex tw-gap-3">
          <a routerLink="login" pButton label="Connexion" severity="secondary"></a>
          <a routerLink="register" pButton label="S'inscrire"></a>
        </div>
      </header>

      <!-- Hero -->
      <main class="tw-max-w-4xl tw-mx-auto tw-px-8 tw-py-16 tw-text-center">
        <h2 class="tw-text-4xl tw-font-bold tw-text-gray-900 tw-mb-6">
          Portail de soumission clinique
        </h2>
        <p class="tw-text-xl tw-text-gray-600 tw-mb-12">
          Déposez vos demandes d'étude clinique en toute sécurité.
        </p>

        <!-- Cartes avantages -->
        <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-3 tw-gap-8 tw-text-left">
          <p-card header="Soumission en ligne" styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-600">
              Déposez vos dossiers directement depuis votre navigateur, sans installation logicielle.
            </p>
          </p-card>

          <p-card header="Suivi en temps réel" styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-600">
              Suivez l'état de votre demande à chaque étape du processus d'examen.
            </p>
          </p-card>

          <p-card header="Communication sécurisée" styleClass="tw-border tw-border-gray-100">
            <p class="tw-text-sm tw-text-gray-600">
              Échangez directement avec les équipes du CTC et du Comité d'Éthique.
            </p>
          </p-card>
        </div>

        <!-- CTA -->
        <div class="tw-mt-12 tw-flex tw-flex-col tw-items-center tw-gap-4">
          <a
            routerLink="register"
            pButton
            label="Soumettre une demande"
            icon="pi pi-arrow-right"
            iconPos="right"
            class="tw-text-lg tw-px-8 tw-py-3"
          ></a>
          <p class="tw-text-sm tw-text-gray-500">
            Déjà inscrit ?
            <a routerLink="login" class="tw-text-blue-600 hover:tw-underline">Se connecter</a>
          </p>
        </div>
      </main>

      <!-- Footer -->
      <footer class="tw-bg-gray-50 tw-border-t tw-border-gray-200 tw-py-6 tw-text-center">
        <p class="tw-text-xs tw-text-gray-400">
          CliniTrak Exchange — Cliniques Universitaires Saint-Luc · Bruxelles
        </p>
      </footer>
    </div>
  `,
})
export class LandingPageComponent {}

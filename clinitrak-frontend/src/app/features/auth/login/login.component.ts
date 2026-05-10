import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessageModule } from 'primeng/message';
import { AuthService } from '../../../core/services/auth.service';
import { authError, isLoading } from '../../../core/store/auth.store';

/**
 * Page de login CliniTrak.
 *
 * <p>Utilise PrimeNG 17 pour les champs de formulaire et les messages d'erreur.
 * L'état de chargement est géré via les signals du store d'auth.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessageModule,
  ],
  template: `
    <div class="tw-min-h-screen tw-bg-gradient-to-br tw-from-slate-900 tw-to-blue-900
                tw-flex tw-items-center tw-justify-center tw-p-4">

      <div class="tw-w-full tw-max-w-md">

        <!-- Card login -->
        <div class="tw-bg-white tw-rounded-2xl tw-shadow-2xl tw-p-8">

          <!-- Logo + Titre -->
          <div class="tw-text-center tw-mb-8">
            <div class="tw-text-5xl tw-mb-3">🏥</div>
            <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900">CliniTrak</h1>
            <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
              Gestion de la recherche clinique
            </p>
          </div>

          <!-- Erreur globale -->
          @if (authError()) {
            <p-message
              severity="error"
              [text]="authError()!"
              styleClass="tw-w-full tw-mb-4"
            />
          }

          <!-- Formulaire -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="tw-space-y-5">

            <!-- Email -->
            <div class="tw-flex tw-flex-col tw-gap-1.5">
              <label for="email" class="tw-text-sm tw-font-medium tw-text-gray-700">
                Adresse email
              </label>
              <input
                pInputText
                id="email"
                type="email"
                formControlName="email"
                placeholder="vous@institution.be"
                [class.ng-dirty]="loginForm.controls.email.dirty"
                class="tw-w-full"
                autocomplete="email"
              />
              @if (loginForm.controls.email.dirty && loginForm.controls.email.errors?.['required']) {
                <small class="tw-text-red-500">L'email est obligatoire</small>
              }
              @if (loginForm.controls.email.dirty && loginForm.controls.email.errors?.['email']) {
                <small class="tw-text-red-500">Format d'email invalide</small>
              }
            </div>

            <!-- Mot de passe -->
            <div class="tw-flex tw-flex-col tw-gap-1.5">
              <label for="password" class="tw-text-sm tw-font-medium tw-text-gray-700">
                Mot de passe
              </label>
              <p-password
                inputId="password"
                formControlName="password"
                [feedback]="false"
                [toggleMask]="true"
                placeholder="••••••••"
                styleClass="tw-w-full"
                inputStyleClass="tw-w-full"
                autocomplete="current-password"
              />
              @if (loginForm.controls.password.dirty && loginForm.controls.password.errors?.['required']) {
                <small class="tw-text-red-500">Le mot de passe est obligatoire</small>
              }
            </div>

            <!-- Lien mot de passe oublié -->
            <div class="tw-flex tw-justify-end">
              <a
                href="#"
                class="tw-text-sm tw-text-blue-600 hover:tw-text-blue-800 hover:tw-underline"
              >
                Mot de passe oublié ?
              </a>
            </div>

            <!-- Bouton submit -->
            <p-button
              type="submit"
              label="Se connecter"
              icon="pi pi-sign-in"
              styleClass="tw-w-full"
              [loading]="isLoading()"
              [disabled]="loginForm.invalid || isLoading()"
            />
          </form>

          <!-- Footer -->
          <p class="tw-text-center tw-text-xs tw-text-gray-400 tw-mt-6">
            CliniTrak v1.0.0 — Cliniques Universitaires Saint-Luc
          </p>
        </div>
      </div>
    </div>
  `,
})
export class LoginComponent {

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly isLoading = isLoading;
  protected readonly authError = authError;

  protected readonly loginForm = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  /** Soumet le formulaire de login. */
  protected onSubmit(): void {
    if (this.loginForm.invalid) return;

    const { email, password } = this.loginForm.value;

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        const returnUrl = new URLSearchParams(window.location.search).get('returnUrl') ?? '/dashboard';
        this.router.navigateByUrl(returnUrl);
      },
    });
  }
}

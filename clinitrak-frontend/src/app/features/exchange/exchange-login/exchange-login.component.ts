import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { Message } from 'primeng/api';
import { ExchangeService } from '../../../core/services/exchange.service';

/**
 * Page de connexion spécifique aux utilisateurs externes du portail Exchange.
 *
 * <p>Distinct du login interne CliniTrak. Stocke le token Exchange dans
 * localStorage sous {@code exchange_access_token} et redirige vers le suivi.
 */
@Component({
  selector: 'app-exchange-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    MessagesModule,
  ],
  template: `
    <div class="tw-min-h-screen tw-bg-gradient-to-b tw-from-blue-50 tw-to-white tw-flex tw-items-center tw-justify-center tw-p-4">
      <div class="tw-w-full tw-max-w-md">

        <!-- En-tête -->
        <div class="tw-text-center tw-mb-8">
          <h1 class="tw-text-3xl tw-font-bold tw-text-blue-700">CliniTrak Exchange</h1>
          <p class="tw-text-gray-500 tw-mt-1">Connexion au portail externe</p>
        </div>

        <div class="tw-bg-white tw-rounded-2xl tw-shadow-lg tw-p-8">

          <!-- Messages d'erreur -->
          @if (messages().length > 0) {
            <p-messages [value]="messages()" styleClass="tw-mb-4" />
          }

          <!-- Formulaire -->
          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="tw-space-y-5">

            <!-- Email -->
            <div class="tw-flex tw-flex-col tw-gap-1.5">
              <label for="email" class="tw-text-sm tw-font-medium tw-text-gray-700">Email *</label>
              <input
                pInputText
                id="email"
                type="email"
                formControlName="email"
                placeholder="vous@organisation.com"
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
              <label for="password" class="tw-text-sm tw-font-medium tw-text-gray-700">Mot de passe *</label>
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

            <!-- Submit -->
            <p-button
              type="submit"
              label="Se connecter"
              icon="pi pi-sign-in"
              styleClass="tw-w-full"
              [loading]="isLoading()"
              [disabled]="loginForm.invalid || isLoading()"
            />
          </form>

          <!-- Liens annexes -->
          <div class="tw-mt-4 tw-text-center tw-text-sm tw-text-gray-500 tw-space-y-1">
            <p>
              Pas encore de compte ?
              <a routerLink="../register" class="tw-text-blue-600 hover:tw-underline">S'inscrire</a>
            </p>
            <p>
              <a routerLink="../" class="tw-text-gray-400 hover:tw-underline">Retour à l'accueil</a>
            </p>
          </div>
        </div>
      </div>
    </div>
  `,
})
export class ExchangeLoginComponent {

  private readonly exchangeService = inject(ExchangeService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  /** Indique si la requête de login est en cours. */
  protected readonly isLoading = signal(false);

  /** Messages d'erreur. */
  protected readonly messages = signal<Message[]>([]);

  protected readonly loginForm = this.fb.group({
    email:    ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  /**
   * Soumet le formulaire de connexion Exchange.
   * En cas de succès, navigue vers le suivi des demandes.
   */
  protected onSubmit(): void {
    if (this.loginForm.invalid) return;

    this.isLoading.set(true);
    this.messages.set([]);

    const { email, password } = this.loginForm.value;

    this.exchangeService.login(email!, password!).subscribe({
      next: () => {
        this.isLoading.set(false);
        this.router.navigate(['/exchange/tracking']);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.messages.set([{
          severity: 'error',
          summary: 'Erreur de connexion',
          detail: err.error?.detail ?? 'Identifiants incorrects. Veuillez réessayer.',
        }]);
      },
    });
  }
}

import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { MessagesModule } from 'primeng/messages';
import { Message } from 'primeng/api';
import { ExchangeService } from '../../../core/services/exchange.service';
import { EXTERNAL_ROLE_OPTIONS } from '../../../core/models/exchange.model';

/**
 * Formulaire d'inscription pour les utilisateurs externes du portail Exchange.
 *
 * <p>Collecte les informations d'identification et le rôle de l'utilisateur.
 * Après succès, affiche un message demandant la vérification de l'email.
 */
@Component({
  selector: 'app-external-registration',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    InputTextModule,
    PasswordModule,
    DropdownModule,
    ButtonModule,
    MessagesModule,
  ],
  template: `
    <div class="tw-min-h-screen tw-bg-gradient-to-b tw-from-blue-50 tw-to-white tw-flex tw-items-center tw-justify-center tw-p-4">
      <div class="tw-w-full tw-max-w-lg">

        <!-- En-tête -->
        <div class="tw-text-center tw-mb-8">
          <h1 class="tw-text-3xl tw-font-bold tw-text-blue-700">CliniTrak Exchange</h1>
          <p class="tw-text-gray-500 tw-mt-1">Créer votre compte externe</p>
        </div>

        <div class="tw-bg-white tw-rounded-2xl tw-shadow-lg tw-p-8">

          <!-- Messages -->
          @if (messages().length > 0) {
            <p-messages [value]="messages()" styleClass="tw-mb-4" />
          }

          <!-- Confirmation post-inscription -->
          @if (submitted()) {
            <div class="tw-text-center tw-py-8">
              <div class="tw-text-5xl tw-mb-4">📧</div>
              <h2 class="tw-text-xl tw-font-semibold tw-text-gray-800 tw-mb-2">
                Vérifiez votre email
              </h2>
              <p class="tw-text-gray-600 tw-mb-6">
                Un lien de confirmation a été envoyé à votre adresse email.
                Cliquez sur ce lien pour activer votre compte.
              </p>
              <a routerLink="../login" pButton label="Retour à la connexion" severity="secondary"></a>
            </div>
          } @else {

            <!-- Formulaire -->
            <form [formGroup]="registerForm" (ngSubmit)="onSubmit()" class="tw-space-y-5">

              <!-- Prénom + Nom -->
              <div class="tw-grid tw-grid-cols-2 tw-gap-4">
                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label for="firstName" class="tw-text-sm tw-font-medium tw-text-gray-700">Prénom *</label>
                  <input pInputText id="firstName" formControlName="firstName" placeholder="Prénom" class="tw-w-full" />
                  @if (registerForm.controls.firstName.dirty && registerForm.controls.firstName.errors?.['required']) {
                    <small class="tw-text-red-500">Obligatoire</small>
                  }
                </div>
                <div class="tw-flex tw-flex-col tw-gap-1.5">
                  <label for="lastName" class="tw-text-sm tw-font-medium tw-text-gray-700">Nom *</label>
                  <input pInputText id="lastName" formControlName="lastName" placeholder="Nom de famille" class="tw-w-full" />
                  @if (registerForm.controls.lastName.dirty && registerForm.controls.lastName.errors?.['required']) {
                    <small class="tw-text-red-500">Obligatoire</small>
                  }
                </div>
              </div>

              <!-- Email -->
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="email" class="tw-text-sm tw-font-medium tw-text-gray-700">Email *</label>
                <input pInputText id="email" type="email" formControlName="email" placeholder="vous@organisation.com" class="tw-w-full" />
                @if (registerForm.controls.email.dirty && registerForm.controls.email.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
                @if (registerForm.controls.email.dirty && registerForm.controls.email.errors?.['email']) {
                  <small class="tw-text-red-500">Format invalide</small>
                }
              </div>

              <!-- Organisation -->
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="organization" class="tw-text-sm tw-font-medium tw-text-gray-700">Organisation</label>
                <input pInputText id="organization" formControlName="organization" placeholder="Firme, institution, hôpital..." class="tw-w-full" />
              </div>

              <!-- Rôle -->
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="role" class="tw-text-sm tw-font-medium tw-text-gray-700">Rôle *</label>
                <p-dropdown
                  inputId="role"
                  formControlName="role"
                  [options]="roleOptions"
                  optionLabel="label"
                  optionValue="value"
                  placeholder="Sélectionner votre rôle"
                  styleClass="tw-w-full"
                />
                @if (registerForm.controls.role.dirty && registerForm.controls.role.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
              </div>

              <!-- Mot de passe -->
              <div class="tw-flex tw-flex-col tw-gap-1.5">
                <label for="password" class="tw-text-sm tw-font-medium tw-text-gray-700">Mot de passe *</label>
                <p-password
                  inputId="password"
                  formControlName="password"
                  [toggleMask]="true"
                  placeholder="Minimum 8 caractères"
                  styleClass="tw-w-full"
                  inputStyleClass="tw-w-full"
                />
                @if (registerForm.controls.password.dirty && registerForm.controls.password.errors?.['required']) {
                  <small class="tw-text-red-500">Obligatoire</small>
                }
                @if (registerForm.controls.password.dirty && registerForm.controls.password.errors?.['minlength']) {
                  <small class="tw-text-red-500">Minimum 8 caractères</small>
                }
              </div>

              <!-- Submit -->
              <p-button
                type="submit"
                label="Créer mon compte"
                icon="pi pi-user-plus"
                styleClass="tw-w-full"
                [loading]="isLoading()"
                [disabled]="registerForm.invalid || isLoading()"
              />
            </form>

            <!-- Lien login -->
            <p class="tw-text-center tw-text-sm tw-text-gray-500 tw-mt-4">
              Déjà inscrit ?
              <a routerLink="../login" class="tw-text-blue-600 hover:tw-underline">Se connecter</a>
            </p>
          }
        </div>
      </div>
    </div>
  `,
})
export class ExternalRegistrationComponent {

  private readonly exchangeService = inject(ExchangeService);
  private readonly fb = inject(FormBuilder);

  /** Indique si la soumission est en cours. */
  protected readonly isLoading = signal(false);

  /** Indique si l'inscription a réussi (affiche le message de vérification). */
  protected readonly submitted = signal(false);

  /** Messages d'erreur ou d'information à afficher. */
  protected readonly messages = signal<Message[]>([]);

  /** Options de rôle pour le dropdown. */
  protected readonly roleOptions = EXTERNAL_ROLE_OPTIONS;

  protected readonly registerForm = this.fb.group({
    firstName:    ['', Validators.required],
    lastName:     ['', Validators.required],
    email:        ['', [Validators.required, Validators.email]],
    organization: [''],
    role:         ['', Validators.required],
    password:     ['', [Validators.required, Validators.minLength(8)]],
  });

  /**
   * Soumet le formulaire d'inscription.
   * En cas de succès, affiche le message de vérification email.
   */
  protected onSubmit(): void {
    if (this.registerForm.invalid) return;

    this.isLoading.set(true);
    this.messages.set([]);

    const { firstName, lastName, email, organization, role, password } = this.registerForm.value;

    this.exchangeService.register({ firstName, lastName, email, organization, role, password }).subscribe({
      next: () => {
        this.submitted.set(true);
        this.isLoading.set(false);
      },
      error: (err) => {
        this.isLoading.set(false);
        this.messages.set([{
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Erreur lors de l\'inscription. Veuillez réessayer.',
        }]);
      },
    });
  }
}

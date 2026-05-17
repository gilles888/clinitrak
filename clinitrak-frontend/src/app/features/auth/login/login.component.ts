import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { InputTextModule } from 'primeng/inputtext';
import { PasswordModule } from 'primeng/password';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { RippleModule } from 'primeng/ripple';
import { AuthService } from '../../../core/services/auth.service';
import { authError, isLoading } from '../../../core/store/auth.store';
import { loginAnimations } from './login.animations';

/**
 * Page de connexion CliniTrak.
 *
 * <p>Design split-screen avec panneau de branding à gauche et formulaire à droite.
 * Gère les états loading, erreur serveur, validation temps réel et succès animé.
 */
@Component({
  selector: 'app-login',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    InputTextModule,
    PasswordModule,
    ButtonModule,
    CheckboxModule,
    RippleModule,
  ],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
  animations: loginAnimations,
})
export class LoginComponent {

  private readonly authService = inject(AuthService);
  private readonly router      = inject(Router);
  private readonly fb          = inject(FormBuilder);

  protected readonly isLoading    = isLoading;
  protected readonly authError    = authError;
  protected readonly loginSuccess = signal(false);
  protected readonly shakeState   = signal<'idle' | 'active'>('idle');

  protected readonly loginForm = this.fb.group({
    email:      ['', [Validators.required, Validators.email]],
    password:   ['', [Validators.required, Validators.minLength(8)]],
    rememberMe: [false],
  });

  protected get emailCtrl()    { return this.loginForm.controls.email; }
  protected get passwordCtrl() { return this.loginForm.controls.password; }

  /** Classe CSS dynamique pour l'input mot de passe (validation colorée). */
  protected get passwordInputClass(): string {
    const base = 'tw-w-full';
    if (this.passwordCtrl.valid && this.passwordCtrl.dirty)       return `${base} ct-input-valid`;
    if (this.passwordCtrl.invalid && this.passwordCtrl.touched)   return `${base} ct-input-error`;
    return base;
  }

  /** Déclenche l'animation shake sur le conteneur du formulaire. */
  private triggerShake(): void {
    this.shakeState.set('active');
    setTimeout(() => this.shakeState.set('idle'), 600);
  }

  /** Soumet le formulaire de connexion. */
  protected onSubmit(): void {
    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      this.triggerShake();
      return;
    }

    const { email, password } = this.loginForm.value;

    const tenantSlug = email!.split('@')[1]?.split('.')[0]?.toLowerCase() ?? '';
    if (tenantSlug) {
      localStorage.setItem('ct_tenant_override', tenantSlug);
    }

    this.authService.login({ email: email!, password: password! }).subscribe({
      next: () => {
        this.loginSuccess.set(true);
        setTimeout(() => {
          const returnUrl = new URLSearchParams(window.location.search).get('returnUrl') ?? '/dashboard';
          this.router.navigateByUrl(returnUrl);
        }, 900);
      },
      error: () => this.triggerShake(),
    });
  }
}

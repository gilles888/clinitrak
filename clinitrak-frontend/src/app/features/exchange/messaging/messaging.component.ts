import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { SlicePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { ExchangeService } from '../../../core/services/exchange.service';
import { ExchangeMessage, SenderType } from '../../../core/models/exchange.model';

/**
 * Interface de messagerie entre l'utilisateur externe et les équipes internes.
 *
 * <p>Affiche les messages sous forme de bulles chat :
 * - Messages internes : bulle à gauche (fond bleu clair)
 * - Messages externes : bulle à droite (fond vert clair)
 */
@Component({
  selector: 'app-messaging',
  standalone: true,
  imports: [FormsModule, SlicePipe, RouterLink, ButtonModule, InputTextareaModule, CardModule, TagModule],
  template: `
    <div class="tw-min-h-screen tw-bg-gray-50 tw-p-6">
      <div class="tw-max-w-3xl tw-mx-auto">

        <!-- En-tête -->
        <div class="tw-mb-6">
          <a routerLink="/exchange/tracking" class="tw-text-blue-600 hover:tw-underline tw-text-sm">
            &larr; Retour au suivi
          </a>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900 tw-mt-1">
            Messagerie — Demande #{{ requestId() | slice:0:8 }}
          </h1>
        </div>

        <!-- Zone de messages -->
        <div class="tw-bg-white tw-rounded-xl tw-shadow tw-flex tw-flex-col" style="height: 70vh;">

          <!-- Liste des messages -->
          <div class="tw-flex-1 tw-overflow-y-auto tw-p-6 tw-space-y-4" #scrollContainer>

            @if (isLoading()) {
              <div class="tw-text-center tw-py-8 tw-text-gray-400">
                <i class="pi pi-spin pi-spinner tw-text-2xl"></i>
              </div>
            } @else if (messages().length === 0) {
              <div class="tw-text-center tw-py-12 tw-text-gray-400">
                <i class="pi pi-comments tw-text-5xl tw-block tw-mb-4"></i>
                <p>Aucun message pour le moment.</p>
                <p class="tw-text-sm">Démarrez la conversation ci-dessous.</p>
              </div>
            } @else {
              @for (msg of messages(); track msg.id) {
                @if (msg.senderType === SenderType.INTERNAL) {
                  <!-- Message interne (gauche, fond bleu) -->
                  <div class="tw-flex tw-flex-col tw-items-start tw-mr-8">
                    <div class="tw-bg-blue-50 tw-border tw-border-blue-100 tw-rounded-xl tw-rounded-tl-none tw-px-4 tw-py-3 tw-max-w-lg">
                      <div class="tw-flex tw-items-center tw-gap-2 tw-mb-1">
                        <p-tag value="Équipe interne" severity="info" styleClass="tw-text-xs" />
                      </div>
                      <p class="tw-text-sm tw-text-gray-800 tw-whitespace-pre-wrap">{{ msg.content }}</p>
                    </div>
                    <span class="tw-text-xs tw-text-gray-400 tw-mt-1 tw-ml-1">
                      {{ msg.sentAt | slice:0:16 | slice:0:10 }} à {{ msg.sentAt | slice:11:16 }}
                    </span>
                  </div>
                } @else {
                  <!-- Message externe (droite, fond vert) -->
                  <div class="tw-flex tw-flex-col tw-items-end tw-ml-8">
                    <div class="tw-bg-green-50 tw-border tw-border-green-100 tw-rounded-xl tw-rounded-tr-none tw-px-4 tw-py-3 tw-max-w-lg">
                      <div class="tw-flex tw-items-center tw-gap-2 tw-mb-1 tw-justify-end">
                        <p-tag value="Vous" severity="success" styleClass="tw-text-xs" />
                      </div>
                      <p class="tw-text-sm tw-text-gray-800 tw-whitespace-pre-wrap">{{ msg.content }}</p>
                    </div>
                    <span class="tw-text-xs tw-text-gray-400 tw-mt-1 tw-mr-1">
                      {{ msg.sentAt | slice:0:10 }} à {{ msg.sentAt | slice:11:16 }}
                    </span>
                  </div>
                }
              }
            }
          </div>

          <!-- Zone de saisie -->
          <div class="tw-border-t tw-border-gray-200 tw-p-4">
            <div class="tw-flex tw-gap-3 tw-items-end">
              <textarea
                pInputTextarea
                [(ngModel)]="newMessage"
                placeholder="Écrivez votre message..."
                rows="3"
                class="tw-flex-1 tw-resize-none"
                (keydown.ctrl.enter)="sendMessage()"
              ></textarea>
              <p-button
                icon="pi pi-send"
                label="Envoyer"
                (onClick)="sendMessage()"
                [loading]="isSending()"
                [disabled]="!newMessage.trim() || isSending()"
              />
            </div>
            <p class="tw-text-xs tw-text-gray-400 tw-mt-1">Ctrl+Entrée pour envoyer rapidement</p>
          </div>
        </div>

      </div>
    </div>
  `,
})
export class MessagingComponent implements OnInit {

  private readonly exchangeService = inject(ExchangeService);

  /** Identifiant de la demande fourni par la route. */
  readonly requestId = input.required<string>();

  /** Messages du fil de discussion. */
  protected readonly messages = signal<ExchangeMessage[]>([]);

  /** Indique si les messages sont en cours de chargement. */
  protected readonly isLoading = signal(false);

  /** Indique si un envoi est en cours. */
  protected readonly isSending = signal(false);

  /** Contenu du message en cours de saisie. */
  protected newMessage = '';

  /** Référence à l'enum pour le template. */
  protected readonly SenderType = SenderType;

  /** Charge les messages au chargement du composant. */
  ngOnInit(): void {
    this.loadMessages(this.requestId());
  }

  /**
   * Charge les messages pour la demande courante.
   *
   * @param requestId Identifiant de la demande
   */
  private loadMessages(requestId: string): void {
    this.isLoading.set(true);
    this.exchangeService.getMessages(requestId).subscribe({
      next: (msgs) => {
        this.messages.set(msgs);
        this.isLoading.set(false);
      },
      error: () => this.isLoading.set(false),
    });
  }

  /**
   * Envoie le message saisi et l'ajoute à la liste.
   */
  protected sendMessage(): void {
    const content = this.newMessage.trim();
    if (!content) return;

    this.isSending.set(true);
    this.exchangeService.sendMessage(this.requestId(), content).subscribe({
      next: (msg) => {
        this.messages.update(msgs => [...msgs, msg]);
        this.newMessage = '';
        this.isSending.set(false);
      },
      error: () => this.isSending.set(false),
    });
  }
}

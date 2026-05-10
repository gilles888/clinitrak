import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { SplitterModule } from 'primeng/splitter';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  CorrespondenceGenerateRequest,
  CorrespondenceResponse,
  TemplateResponse,
} from '../../../core/models/ethics.model';

/**
 * Composant de génération et d'envoi de correspondances CE.
 *
 * <p>Interface en deux colonnes :
 * - Gauche : formulaire de génération d'une lettre à partir d'un modèle.
 * - Droite : liste des correspondances récentes pour une étude donnée,
 *   avec possibilité d'envoyer par email ou de télécharger le PDF.
 *
 * <p>La génération fait appel à {@link EthicsService#generateCorrespondence},
 * l'envoi à {@link EthicsService#sendCorrespondence},
 * et le téléchargement PDF à {@link EthicsService#downloadPdf}.
 */
@Component({
  selector: 'app-correspondence-generator',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    ButtonModule,
    CardModule,
    DropdownModule,
    InputTextModule,
    SplitterModule,
    TableModule,
    TagModule,
    ToastModule,
    TooltipModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-4">

      <!-- En-tête -->
      <div>
        <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Correspondances</h2>
        <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Générez et envoyez des lettres CE depuis les modèles disponibles</p>
      </div>

      <!-- Layout 2 colonnes via splitter -->
      <p-splitter [panelSizes]="[40, 60]" [minSizes]="[30, 30]" styleClass="tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">

        <!-- Colonne gauche : Génération -->
        <ng-template pTemplate>
          <div class="tw-p-5 tw-space-y-4 tw-overflow-y-auto tw-h-full">
            <h3 class="tw-font-semibold tw-text-gray-800">Générer une correspondance</h3>

            <!-- Modèle -->
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                Modèle de lettre <span class="tw-text-red-500">*</span>
              </label>
              <p-dropdown
                [(ngModel)]="selectedTemplateCode"
                [options]="templateOptions()"
                optionLabel="label"
                optionValue="value"
                placeholder="Sélectionner un modèle"
                styleClass="tw-w-full"
                [loading]="isLoadingTemplates()"
              />
            </div>

            <!-- ID Étude -->
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                ID Étude <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                [(ngModel)]="studyIdInput"
                placeholder="UUID de l'étude"
                class="tw-w-full tw-font-mono"
              />
            </div>

            <!-- Email destinataire -->
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                Email destinataire <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                [(ngModel)]="recipientEmail"
                type="email"
                placeholder="contact@exemple.com"
                class="tw-w-full"
              />
            </div>

            <!-- Nom destinataire -->
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">
                Nom destinataire <span class="tw-text-red-500">*</span>
              </label>
              <input
                pInputText
                [(ngModel)]="recipientName"
                placeholder="Pr. Jean Dupont"
                class="tw-w-full"
              />
            </div>

            <!-- ID Avis CE (optionnel) -->
            <div class="tw-flex tw-flex-col tw-gap-1">
              <label class="tw-text-sm tw-font-medium tw-text-gray-700">ID Avis CE (optionnel)</label>
              <input
                pInputText
                [(ngModel)]="reviewIdInput"
                placeholder="UUID de l'avis CE"
                class="tw-w-full tw-font-mono"
              />
            </div>

            <!-- Bouton générer -->
            <p-button
              label="Générer le document"
              icon="pi pi-file-edit"
              [loading]="isGenerating()"
              [disabled]="!isFormValid()"
              (onClick)="generateCorrespondence()"
              styleClass="tw-w-full"
            />

            <!-- Info sujet du modèle sélectionné -->
            @if (selectedTemplate()) {
              <div class="tw-bg-blue-50 tw-border tw-border-blue-200 tw-rounded-lg tw-p-3">
                <p class="tw-text-xs tw-font-medium tw-text-blue-700">Sujet : {{ selectedTemplate()!.subject }}</p>
                <p class="tw-text-xs tw-text-blue-600 tw-mt-0.5">Langue : {{ selectedTemplate()!.language }}</p>
              </div>
            }

          </div>
        </ng-template>

        <!-- Colonne droite : Correspondances récentes -->
        <ng-template pTemplate>
          <div class="tw-p-5 tw-space-y-4 tw-overflow-y-auto tw-h-full">
            <div class="tw-flex tw-items-center tw-justify-between">
              <h3 class="tw-font-semibold tw-text-gray-800">Correspondances récentes</h3>
              <div class="tw-flex tw-items-center tw-gap-2">
                <input
                  pInputText
                  [(ngModel)]="studyIdFilter"
                  placeholder="ID Étude pour filtrer..."
                  class="tw-text-sm tw-font-mono"
                />
                <p-button
                  icon="pi pi-search"
                  size="small"
                  severity="secondary"
                  pTooltip="Rechercher"
                  (onClick)="loadCorrespondences()"
                />
              </div>
            </div>

            @if (isLoadingCorrespondences()) {
              <div class="tw-flex tw-justify-center tw-py-10">
                <i class="pi pi-spin pi-spinner tw-text-2xl tw-text-blue-500"></i>
              </div>
            }

            @if (!isLoadingCorrespondences() && correspondences().length === 0) {
              <div class="tw-text-center tw-py-10 tw-text-gray-400">
                <i class="pi pi-envelope tw-text-3xl tw-block tw-mb-2"></i>
                <p class="tw-text-sm">
                  @if (studyIdFilter) {
                    Aucune correspondance pour cette étude.
                  } @else {
                    Entrez un ID étude pour voir ses correspondances.
                  }
                </p>
              </div>
            }

            @if (!isLoadingCorrespondences() && correspondences().length > 0) {
              <p-table
                [value]="correspondences()"
                [paginator]="true"
                [rows]="10"
                styleClass="tw-text-sm"
                [rowHover]="true"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Sujet</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Destinataire</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Date</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2 tw-text-center">Envoyé</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2 tw-text-center">Actions</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-c>
                  <tr>
                    <td class="tw-px-3 tw-py-2 tw-max-w-[150px] tw-truncate tw-text-sm" [title]="c.subject">
                      {{ c.subject }}
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600">{{ c.recipientName }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">
                      {{ c.generatedDate | date:'dd/MM/yyyy' }}
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-center">
                      @if (c.sent) {
                        <p-tag value="Envoyé" severity="success" />
                      } @else {
                        <p-tag value="Non envoyé" severity="secondary" />
                      }
                    </td>
                    <td class="tw-px-3 tw-py-2">
                      <div class="tw-flex tw-gap-1 tw-justify-center">
                        @if (!c.sent) {
                          <p-button
                            icon="pi pi-send"
                            severity="info"
                            size="small"
                            [rounded]="true"
                            [text]="true"
                            pTooltip="Envoyer par email"
                            (onClick)="sendCorrespondence(c)"
                          />
                        }
                        <p-button
                          icon="pi pi-download"
                          severity="secondary"
                          size="small"
                          [rounded]="true"
                          [text]="true"
                          pTooltip="Télécharger le PDF"
                          (onClick)="downloadPdf(c.id)"
                        />
                      </div>
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }

          </div>
        </ng-template>

      </p-splitter>

    </div>
  `,
})
export class CorrespondenceGeneratorComponent implements OnInit {

  private readonly ethicsService = inject(EthicsService);
  private readonly messageService = inject(MessageService);

  // ─── État réactif ──────────────────────────────────────────
  /** Liste des modèles disponibles. */
  protected readonly templates = signal<TemplateResponse[]>([]);
  /** Options du dropdown modèles (dérivé de templates). */
  protected readonly templateOptions = signal<{ label: string; value: string }[]>([]);
  /** Modèle sélectionné complet. */
  protected readonly selectedTemplate = signal<TemplateResponse | null>(null);
  /** Liste des correspondances affichées. */
  protected readonly correspondences = signal<CorrespondenceResponse[]>([]);
  /** Indicateur de chargement des modèles. */
  protected readonly isLoadingTemplates = signal(false);
  /** Indicateur de génération en cours. */
  protected readonly isGenerating = signal(false);
  /** Indicateur de chargement des correspondances. */
  protected readonly isLoadingCorrespondences = signal(false);

  // ─── Champs du formulaire de génération ──────────────────
  /** Code du modèle sélectionné. */
  protected selectedTemplateCode: string | null = null;
  /** ID de l'étude pour la génération. */
  protected studyIdInput = '';
  /** Email du destinataire. */
  protected recipientEmail = '';
  /** Nom du destinataire. */
  protected recipientName = '';
  /** ID de l'avis CE (optionnel). */
  protected reviewIdInput = '';
  /** Filtre ID étude pour la liste de droite. */
  protected studyIdFilter = '';

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadTemplates();
  }

  /** Charge les modèles de correspondance disponibles. */
  private loadTemplates(): void {
    this.isLoadingTemplates.set(true);
    this.ethicsService.getTemplates().subscribe({
      next: (list) => {
        this.templates.set(list);
        this.templateOptions.set(
          list.filter(t => t.isActive).map(t => ({
            label: `${t.templateName} (${t.language})`,
            value: t.templateCode,
          })),
        );
        this.isLoadingTemplates.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement modèles', err);
        this.isLoadingTemplates.set(false);
      },
    });
  }

  /**
   * Met à jour le modèle sélectionné lorsque le dropdown change.
   * Appelé par ngModelChange sur le dropdown.
   */
  protected onTemplateChange(): void {
    const found = this.templates().find(t => t.templateCode === this.selectedTemplateCode) ?? null;
    this.selectedTemplate.set(found);
  }

  /**
   * Charge les correspondances pour l'ID étude saisi dans le filtre de droite.
   */
  protected loadCorrespondences(): void {
    if (!this.studyIdFilter.trim()) {
      this.correspondences.set([]);
      return;
    }
    this.isLoadingCorrespondences.set(true);
    this.ethicsService.getCorrespondenceByStudy(this.studyIdFilter.trim()).subscribe({
      next: (page) => {
        this.correspondences.set(page.content);
        this.isLoadingCorrespondences.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement correspondances', err);
        this.isLoadingCorrespondences.set(false);
      },
    });
  }

  /**
   * Génère une correspondance depuis le formulaire de gauche.
   * Recharge automatiquement la liste droite si l'étude correspond au filtre.
   */
  protected generateCorrespondence(): void {
    if (!this.isFormValid()) return;

    const request: CorrespondenceGenerateRequest = {
      studyId:        this.studyIdInput.trim(),
      templateCode:   this.selectedTemplateCode!,
      recipientEmail: this.recipientEmail.trim(),
      recipientName:  this.recipientName.trim(),
      reviewId:       this.reviewIdInput.trim() || undefined,
    };

    this.isGenerating.set(true);
    this.ethicsService.generateCorrespondence(request).subscribe({
      next: (correspondence) => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Correspondance générée.' });
        this.isGenerating.set(false);
        // Mettre à jour la liste droite si même étude
        if (this.studyIdFilter.trim() === this.studyIdInput.trim()) {
          this.correspondences.update(list => [correspondence, ...list]);
        } else {
          this.studyIdFilter = this.studyIdInput.trim();
          this.loadCorrespondences();
        }
      },
      error: (err) => {
        console.error('Erreur génération correspondance', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de générer la correspondance.' });
        this.isGenerating.set(false);
      },
    });
  }

  /**
   * Envoie une correspondance déjà générée par email.
   *
   * @param correspondence correspondance à envoyer
   */
  protected sendCorrespondence(correspondence: CorrespondenceResponse): void {
    this.ethicsService.sendCorrespondence(correspondence.id).subscribe({
      next: (updated) => {
        this.correspondences.update(list =>
          list.map(c => c.id === updated.id ? updated : c),
        );
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Email envoyé à ' + updated.recipientEmail });
      },
      error: (err) => {
        console.error('Erreur envoi correspondance', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'envoyer l\'email.' });
      },
    });
  }

  /**
   * Télécharge le PDF d'une correspondance et déclenche le téléchargement navigateur.
   *
   * @param id identifiant UUID de la correspondance
   */
  protected downloadPdf(id: string): void {
    this.ethicsService.downloadPdf(id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `courrier-${id}.pdf`;
        a.click();
        URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Erreur téléchargement PDF', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de télécharger le PDF.' });
      },
    });
  }

  /**
   * Vérifie que les champs obligatoires du formulaire de génération sont remplis.
   *
   * @returns vrai si le formulaire est valide
   */
  protected isFormValid(): boolean {
    return !!(
      this.selectedTemplateCode &&
      this.studyIdInput.trim() &&
      this.recipientEmail.trim() &&
      this.recipientName.trim()
    );
  }
}

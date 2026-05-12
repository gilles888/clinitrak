import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { InputTextareaModule } from 'primeng/inputtextarea';
import { TimelineModule } from 'primeng/timeline';
import { ToastModule } from 'primeng/toast';
import { TooltipModule } from 'primeng/tooltip';
import { MessageService } from 'primeng/api';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  DECISION_SEVERITY,
  MEETING_STATUS_SEVERITY,
  AgendaItemResponse,
  MeetingResponse,
  MeetingStatus,
} from '../../../core/models/ethics.model';

/**
 * Composant de détail d'une réunion CE.
 *
 * <p>Affiche les informations complètes d'une réunion (date, lieu, statut, notes),
 * l'ordre du jour avec les items et leurs décisions éventuelles, ainsi que les
 * boutons d'action pour faire évoluer le statut de la réunion.
 *
 * <p>Les données sont chargées via {@link EthicsService} au moment de l'initialisation.
 * L'identifiant de la réunion est extrait de la route {@code /ethics/meetings/:id}.
 */
@Component({
  selector: 'app-meeting-detail',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    CardModule,
    DialogModule,
    DropdownModule,
    InputTextModule,
    TableModule,
    TagModule,
    InputTextareaModule,
    TimelineModule,
    ToastModule,
    TooltipModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-6">

      <!-- En-tête navigation -->
      <div class="tw-flex tw-items-center tw-gap-3">
        <p-button
          icon="pi pi-arrow-left"
          severity="secondary"
          [text]="true"
          [rounded]="true"
          (onClick)="router.navigate(['/ethics/meetings'])"
          pTooltip="Retour au calendrier"
        />
        <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Détail de la réunion</h2>
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-16">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      @if (!isLoading() && meeting()) {

        <!-- Section 1 : Informations de la réunion -->
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
          <div class="tw-flex tw-items-center tw-justify-between tw-px-6 tw-py-4 tw-border-b tw-border-gray-100">
            <h3 class="tw-font-semibold tw-text-gray-800 tw-text-lg">Informations</h3>
            <div class="tw-flex tw-gap-2">
              <p-tag
                [value]="meeting()!.meetingTypeLabel"
                severity="info"
              />
              <p-tag
                [value]="meeting()!.statusLabel"
                [severity]="getMeetingStatusSeverity(meeting()!.status)"
              />
            </div>
          </div>

          <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 lg:tw-grid-cols-4 tw-gap-4 tw-p-6">
            <div>
              <p class="tw-text-xs tw-font-medium tw-text-gray-500 tw-uppercase tw-tracking-wide">Date</p>
              <p class="tw-mt-1 tw-font-semibold tw-text-gray-800">
                {{ meeting()!.meetingDate | date:'EEEE d MMMM yyyy':'':'fr' }}
              </p>
            </div>
            <div>
              <p class="tw-text-xs tw-font-medium tw-text-gray-500 tw-uppercase tw-tracking-wide">Heure</p>
              <p class="tw-mt-1 tw-font-semibold tw-text-gray-800">
                {{ meeting()!.meetingTime ?? '—' }}
              </p>
            </div>
            <div>
              <p class="tw-text-xs tw-font-medium tw-text-gray-500 tw-uppercase tw-tracking-wide">Lieu</p>
              <p class="tw-mt-1 tw-font-semibold tw-text-gray-800">{{ meeting()!.location }}</p>
            </div>
            <div>
              <p class="tw-text-xs tw-font-medium tw-text-gray-500 tw-uppercase tw-tracking-wide">Points à l'ordre du jour</p>
              <p class="tw-mt-1 tw-font-semibold tw-text-gray-800">{{ meeting()!.agendaItemCount }}</p>
            </div>
          </div>

          @if (meeting()!.notes) {
            <div class="tw-px-6 tw-pb-4">
              <p class="tw-text-xs tw-font-medium tw-text-gray-500 tw-uppercase tw-tracking-wide tw-mb-1">Notes</p>
              <p class="tw-text-sm tw-text-gray-700 tw-bg-gray-50 tw-rounded-lg tw-p-3">{{ meeting()!.notes }}</p>
            </div>
          }

          <!-- Boutons d'action sur le statut -->
          <div class="tw-flex tw-gap-2 tw-px-6 tw-pb-5 tw-flex-wrap">
            @if (meeting()!.status === MeetingStatus.PLANNED) {
              <p-button
                label="Marquer En cours"
                icon="pi pi-play"
                severity="warning"
                size="small"
                [loading]="isUpdatingStatus()"
                (onClick)="updateStatus('IN_PROGRESS')"
              />
              <p-button
                label="Annuler la réunion"
                icon="pi pi-times"
                severity="danger"
                size="small"
                [loading]="isUpdatingStatus()"
                (onClick)="updateStatus('CANCELLED')"
              />
            }
            @if (meeting()!.status === MeetingStatus.IN_PROGRESS) {
              <p-button
                label="Clôturer la réunion"
                icon="pi pi-check-circle"
                severity="success"
                size="small"
                [loading]="isUpdatingStatus()"
                (onClick)="updateStatus('COMPLETED')"
              />
            }
          </div>
        </div>

        <!-- Section 2 : Ordre du jour -->
        <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm">
          <div class="tw-flex tw-items-center tw-justify-between tw-px-6 tw-py-4 tw-border-b tw-border-gray-100">
            <h3 class="tw-font-semibold tw-text-gray-800 tw-text-lg">Ordre du jour</h3>
            <p-button
              label="Ajouter un item"
              icon="pi pi-plus"
              size="small"
              severity="secondary"
              (onClick)="openAddAgendaDialog()"
            />
          </div>

          @if (agendaItems().length === 0) {
            <div class="tw-py-10 tw-text-center tw-text-gray-400">
              <i class="pi pi-list tw-text-3xl tw-block tw-mb-2"></i>
              <p class="tw-text-sm">Aucun item dans l'ordre du jour.</p>
            </div>
          } @else {
            <p-table
              [value]="agendaItems()"
              styleClass="tw-text-sm"
              [scrollable]="true"
            >
              <ng-template pTemplate="header">
                <tr class="tw-bg-gray-50">
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2 tw-w-12">#</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2">Étude</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2">Type</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2 tw-w-24">Durée (min)</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2">Décision</th>
                  <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-4 tw-py-2">Commentaires</th>
                </tr>
              </ng-template>
              <ng-template pTemplate="body" let-item>
                <tr class="hover:tw-bg-gray-50">
                  <td class="tw-px-4 tw-py-2 tw-text-gray-500 tw-font-mono">{{ item.itemOrder }}</td>
                  <td class="tw-px-4 tw-py-2 tw-font-mono tw-text-blue-600 tw-text-xs">
                    @if (item.studyId) {
                      {{ item.studyId | slice:0:8 }}...
                    } @else {
                      <span class="tw-text-gray-400">—</span>
                    }
                  </td>
                  <td class="tw-px-4 tw-py-2 tw-text-gray-700">{{ item.itemType ?? '—' }}</td>
                  <td class="tw-px-4 tw-py-2 tw-text-center tw-font-semibold">{{ item.durationMinutes }}</td>
                  <td class="tw-px-4 tw-py-2">
                    @if (item.decision) {
                      <p-tag
                        [value]="item.decisionLabel ?? item.decision"
                        [severity]="getDecisionSeverity(item.decision)"
                      />
                    } @else {
                      <span class="tw-text-gray-400 tw-text-xs">—</span>
                    }
                  </td>
                  <td class="tw-px-4 tw-py-2 tw-text-gray-600 tw-text-xs tw-max-w-xs tw-truncate">
                    {{ item.comments ?? '—' }}
                  </td>
                </tr>
              </ng-template>
            </p-table>
          }
        </div>

      }

      @if (!isLoading() && !meeting()) {
        <div class="tw-text-center tw-py-20 tw-text-gray-400">
          <i class="pi pi-exclamation-circle tw-text-4xl tw-block tw-mb-3"></i>
          <p class="tw-text-lg tw-font-medium">Réunion introuvable</p>
          <p class="tw-text-sm tw-mt-1">L'identifiant de réunion n'existe pas ou vous n'y avez pas accès.</p>
          <p-button
            label="Retour au calendrier"
            severity="secondary"
            styleClass="tw-mt-4"
            (onClick)="router.navigate(['/ethics/meetings'])"
          />
        </div>
      }

    </div>

    <!-- Dialog ajout item ordre du jour -->
    <p-dialog
      header="Ajouter un item à l'ordre du jour"
      [(visible)]="showAgendaDialog"
      [modal]="true"
      [style]="{ width: '480px' }"
      [closable]="true"
      [draggable]="false"
    >
      <div class="tw-space-y-4 tw-pt-2">

        <!-- Étude (optionnel) -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">ID Étude (optionnel)</label>
          <input
            pInputText
            [(ngModel)]="newAgendaItem.studyId"
            placeholder="UUID de l'étude"
            class="tw-w-full tw-font-mono"
          />
        </div>

        <!-- Type d'item -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Type</label>
          <input
            pInputText
            [(ngModel)]="newAgendaItem.itemType"
            placeholder="Ex: Présentation initiale, Amendement..."
            class="tw-w-full"
          />
        </div>

        <!-- Durée -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">
            Durée (minutes) <span class="tw-text-red-500">*</span>
          </label>
          <input
            pInputText
            type="number"
            [(ngModel)]="newAgendaItem.durationMinutes"
            placeholder="15"
            min="1"
            class="tw-w-full"
          />
        </div>

        <!-- Commentaires -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Commentaires</label>
          <textarea
            pTextarea
            [(ngModel)]="newAgendaItem.comments"
            rows="3"
            placeholder="Informations sur cet item..."
            class="tw-w-full"
          ></textarea>
        </div>

      </div>

      <ng-template pTemplate="footer">
        <div class="tw-flex tw-gap-2 tw-justify-end">
          <p-button
            label="Annuler"
            severity="secondary"
            (onClick)="showAgendaDialog = false"
          />
          <p-button
            label="Ajouter"
            icon="pi pi-plus"
            [loading]="isSavingAgenda()"
            [disabled]="!newAgendaItem.durationMinutes"
            (onClick)="addAgendaItem()"
          />
        </div>
      </ng-template>
    </p-dialog>
  `,
})
export class MeetingDetailComponent implements OnInit {

  protected readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly ethicsService = inject(EthicsService);
  private readonly messageService = inject(MessageService);

  /** Expose l'enum MeetingStatus au template. */
  protected readonly MeetingStatus = MeetingStatus;

  // ─── État réactif ──────────────────────────────────────────
  /** Réunion chargée, null si non trouvée ou en chargement. */
  protected readonly meeting = signal<MeetingResponse | null>(null);
  /** Items de l'ordre du jour. */
  protected readonly agendaItems = signal<AgendaItemResponse[]>([]);
  /** Indicateur de chargement initial. */
  protected readonly isLoading = signal(false);
  /** Indicateur de mise à jour du statut. */
  protected readonly isUpdatingStatus = signal(false);
  /** Indicateur de sauvegarde d'un item. */
  protected readonly isSavingAgenda = signal(false);

  /** Visibilité du dialog d'ajout d'item. */
  protected showAgendaDialog = false;
  /** Données du nouvel item de l'ordre du jour. */
  protected newAgendaItem: {
    studyId?: string;
    itemType?: string;
    durationMinutes: number | null;
    comments?: string;
  } = { durationMinutes: null };

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadMeetingData(id);
    }
  }

  /**
   * Charge les données de la réunion et son ordre du jour depuis l'API.
   *
   * <p>Utilise {@link EthicsService#getMeetings} puis filtre par id car
   * l'endpoint {@code GET /meetings/{id}} n'est pas encore disponible.
   *
   * @param id identifiant UUID de la réunion
   */
  private loadMeetingData(id: string): void {
    this.isLoading.set(true);
    this.ethicsService.getMeetings().subscribe({
      next: (meetings) => {
        const found = meetings.find(m => m.id === id) ?? null;
        this.meeting.set(found);
        this.isLoading.set(false);
        if (found) {
          this.loadAgendaItems(id);
        }
      },
      error: (err) => {
        console.error('Erreur chargement réunions', err);
        this.isLoading.set(false);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger la réunion.' });
      },
    });
  }

  /**
   * Charge les items de l'ordre du jour d'une réunion.
   *
   * @param meetingId identifiant UUID de la réunion
   */
  private loadAgendaItems(meetingId: string): void {
    this.ethicsService.getAgendaItems(meetingId).subscribe({
      next: (items) => this.agendaItems.set(items),
      error: (err) => console.error('Erreur chargement ordre du jour', err),
    });
  }

  /**
   * Met à jour le statut de la réunion courante.
   *
   * @param status nouveau statut (PLANNED, IN_PROGRESS, COMPLETED, CANCELLED)
   */
  protected updateStatus(status: string): void {
    const m = this.meeting();
    if (!m) return;

    this.isUpdatingStatus.set(true);
    this.ethicsService.updateMeetingStatus(m.id, status).subscribe({
      next: () => {
        const statusLabel = this.getStatusLabel(status);
        this.meeting.update(curr => curr ? { ...curr, status: status as MeetingStatus, statusLabel } : null);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: `Statut mis à jour : ${statusLabel}` });
        this.isUpdatingStatus.set(false);
      },
      error: (err) => {
        console.error('Erreur mise à jour statut', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de mettre à jour le statut.' });
        this.isUpdatingStatus.set(false);
      },
    });
  }

  /** Ouvre le dialog d'ajout d'item. */
  protected openAddAgendaDialog(): void {
    this.newAgendaItem = { durationMinutes: null };
    this.showAgendaDialog = true;
  }

  /**
   * Ajoute un item à l'ordre du jour de la réunion courante.
   */
  protected addAgendaItem(): void {
    const m = this.meeting();
    if (!m || !this.newAgendaItem.durationMinutes) return;

    this.isSavingAgenda.set(true);
    this.ethicsService.addAgendaItem(m.id, {
      studyId:         this.newAgendaItem.studyId || undefined,
      itemType:        this.newAgendaItem.itemType || undefined,
      durationMinutes: this.newAgendaItem.durationMinutes,
      comments:        this.newAgendaItem.comments || undefined,
    }).subscribe({
      next: (item) => {
        this.agendaItems.update(list => [...list, item]);
        this.meeting.update(curr => curr ? { ...curr, agendaItemCount: curr.agendaItemCount + 1 } : null);
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Item ajouté à l\'ordre du jour.' });
        this.isSavingAgenda.set(false);
        this.showAgendaDialog = false;
      },
      error: (err) => {
        console.error('Erreur ajout item', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible d\'ajouter l\'item.' });
        this.isSavingAgenda.set(false);
      },
    });
  }

  /**
   * Retourne la severité PrimeNG Tag pour un statut de réunion donné.
   *
   * @param status clé du statut
   * @returns severité PrimeNG
   */
  protected getMeetingStatusSeverity(status: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return MEETING_STATUS_SEVERITY[status] ?? 'info';
  }

  /**
   * Retourne la severité PrimeNG Tag pour une décision CE donnée.
   *
   * @param decision clé de la décision
   * @returns severité PrimeNG
   */
  protected getDecisionSeverity(decision: string): 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined {
    return DECISION_SEVERITY[decision] ?? 'info';
  }

  /**
   * Retourne le libellé d'un statut de réunion.
   *
   * @param status clé du statut
   * @returns libellé affiché
   */
  private getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      PLANNED:     'Planifiée',
      IN_PROGRESS: 'En cours',
      COMPLETED:   'Terminée',
      CANCELLED:   'Annulée',
    };
    return labels[status] ?? status;
  }
}

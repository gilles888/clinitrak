import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CalendarModule } from 'primeng/calendar';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { TextareaModule } from 'primeng/textarea';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { FullCalendarModule } from '@fullcalendar/angular';
import { CalendarOptions } from '@fullcalendar/core';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import interactionPlugin from '@fullcalendar/interaction';
import listPlugin from '@fullcalendar/list';
import { EthicsService } from '../../../core/services/ethics.service';
import {
  MEETING_TYPE_OPTIONS,
  MeetingCreateRequest,
  MeetingResponse,
  MeetingStatus,
} from '../../../core/models/ethics.model';

/**
 * Composant calendrier des réunions du Comité d'Éthique.
 *
 * <p>Affiche les réunions CE sous forme de calendrier interactif (FullCalendar).
 * Permet de créer de nouvelles réunions via un dialog.
 *
 * <p>Les plugins FullCalendar requis sont :
 * {@code @fullcalendar/daygrid}, {@code @fullcalendar/timegrid},
 * {@code @fullcalendar/interaction}, {@code @fullcalendar/list}.
 *
 * @remarks
 * Les packages FullCalendar doivent être ajoutés au {@code package.json} :
 * {@code @fullcalendar/angular}, {@code @fullcalendar/core},
 * {@code @fullcalendar/daygrid}, {@code @fullcalendar/timegrid},
 * {@code @fullcalendar/interaction}, {@code @fullcalendar/list}.
 */
@Component({
  selector: 'app-meeting-calendar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ButtonModule,
    CalendarModule,
    DialogModule,
    DropdownModule,
    FullCalendarModule,
    InputTextModule,
    TextareaModule,
    ToastModule,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-4">

      <!-- Barre d'actions -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Calendrier des réunions CE</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">{{ meetings().length }} réunion(s) chargée(s)</p>
        </div>
        <p-button
          label="Nouvelle réunion"
          icon="pi pi-plus"
          (onClick)="openNewMeetingDialog()"
          severity="primary"
        />
      </div>

      <!-- Légende -->
      <div class="tw-flex tw-gap-4 tw-text-xs tw-text-gray-600">
        <span class="tw-flex tw-items-center tw-gap-1.5">
          <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-blue-500 tw-inline-block"></span> Planifiée
        </span>
        <span class="tw-flex tw-items-center tw-gap-1.5">
          <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-yellow-500 tw-inline-block"></span> En cours
        </span>
        <span class="tw-flex tw-items-center tw-gap-1.5">
          <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-green-500 tw-inline-block"></span> Terminée
        </span>
        <span class="tw-flex tw-items-center tw-gap-1.5">
          <span class="tw-w-3 tw-h-3 tw-rounded-full tw-bg-gray-400 tw-inline-block"></span> Annulée
        </span>
      </div>

      <!-- FullCalendar -->
      <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-shadow-sm tw-p-4">
        @if (isLoading()) {
          <div class="tw-flex tw-justify-center tw-py-16">
            <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
          </div>
        } @else {
          <full-calendar [options]="calendarOptions()" />
        }
      </div>

    </div>

    <!-- Dialog Nouvelle réunion -->
    <p-dialog
      header="Nouvelle réunion CE"
      [(visible)]="showNewMeetingDialog"
      [modal]="true"
      [style]="{ width: '520px' }"
      [closable]="true"
      [draggable]="false"
    >
      <div class="tw-space-y-4 tw-pt-2">

        <!-- Date de la réunion -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">
            Date <span class="tw-text-red-500">*</span>
          </label>
          <p-calendar
            [(ngModel)]="newMeeting.meetingDate"
            dateFormat="dd/mm/yy"
            [showIcon]="true"
            styleClass="tw-w-full"
          />
        </div>

        <!-- Heure -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Heure (ex: 14:00)</label>
          <input
            pInputText
            [(ngModel)]="newMeeting.meetingTime"
            placeholder="HH:MM"
            class="tw-w-full"
          />
        </div>

        <!-- Type de réunion -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">
            Type <span class="tw-text-red-500">*</span>
          </label>
          <p-dropdown
            [(ngModel)]="newMeeting.meetingType"
            [options]="meetingTypeOptions"
            optionLabel="label"
            optionValue="value"
            placeholder="Sélectionner le type"
            styleClass="tw-w-full"
          />
        </div>

        <!-- Lieu -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">
            Lieu <span class="tw-text-red-500">*</span>
          </label>
          <input
            pInputText
            [(ngModel)]="newMeeting.location"
            placeholder="Salle de conférence, adresse..."
            class="tw-w-full"
          />
        </div>

        <!-- Notes -->
        <div class="tw-flex tw-flex-col tw-gap-1">
          <label class="tw-text-sm tw-font-medium tw-text-gray-700">Notes</label>
          <textarea
            pTextarea
            [(ngModel)]="newMeeting.notes"
            rows="3"
            placeholder="Informations complémentaires sur la réunion..."
            class="tw-w-full"
          ></textarea>
        </div>

      </div>

      <ng-template pTemplate="footer">
        <div class="tw-flex tw-gap-2 tw-justify-end">
          <p-button
            label="Annuler"
            severity="secondary"
            (onClick)="closeNewMeetingDialog()"
          />
          <p-button
            label="Créer la réunion"
            icon="pi pi-check"
            [loading]="isSaving()"
            [disabled]="!isNewMeetingValid()"
            (onClick)="createMeeting()"
          />
        </div>
      </ng-template>
    </p-dialog>
  `,
})
export class MeetingCalendarComponent implements OnInit {

  private readonly ethicsService = inject(EthicsService);
  private readonly router = inject(Router);
  private readonly messageService = inject(MessageService);

  /** Options du dropdown type de réunion. */
  protected readonly meetingTypeOptions = MEETING_TYPE_OPTIONS;

  // ─── État réactif ──────────────────────────────────────────
  /** Liste des réunions chargées. */
  protected readonly meetings = signal<MeetingResponse[]>([]);
  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);
  /** Indicateur de création en cours. */
  protected readonly isSaving = signal(false);
  /** Visibilité du dialog de création. */
  protected showNewMeetingDialog = false;
  /** Date pré-remplie lorsqu'on clique sur un jour du calendrier. */
  protected readonly newMeetingDate = signal<string | null>(null);

  /** Données du formulaire de création de réunion. */
  protected newMeeting: Partial<MeetingCreateRequest> & { meetingDate: Date | null } = {
    meetingDate: null,
    meetingTime: '',
    meetingType: undefined,
    location: '',
    notes: '',
  };

  /**
   * Options FullCalendar, recalculées dès que {@link meetings} change.
   *
   * <p>La liste des événements est dérivée de {@link meetings} via {@link computed}.
   */
  protected readonly calendarOptions = computed<CalendarOptions>(() => ({
    plugins: [dayGridPlugin, timeGridPlugin, interactionPlugin, listPlugin],
    initialView: 'dayGridMonth',
    locale: 'fr',
    firstDay: 1,
    headerToolbar: {
      left:   'prev,next today',
      center: 'title',
      right:  'dayGridMonth,listMonth',
    },
    buttonText: {
      today:     'Aujourd\'hui',
      month:     'Mois',
      list:      'Liste',
    },
    events: this.meetings().map(m => ({
      id:              m.id,
      title:           `${m.meetingTypeLabel} — ${m.location}`,
      date:            m.meetingDate,
      backgroundColor: this.getMeetingColor(m.status),
      borderColor:     this.getMeetingColor(m.status),
      extendedProps:   { meeting: m },
    })),
    eventClick: (info) => {
      this.router.navigate(['/ethics/meetings', info.event.id]);
    },
    dateClick: (info) => {
      this.newMeetingDate.set(info.dateStr);
      const parts = info.dateStr.split('-');
      this.newMeeting.meetingDate = new Date(
        Number(parts[0]),
        Number(parts[1]) - 1,
        Number(parts[2]),
      );
      this.showNewMeetingDialog = true;
    },
  }));

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadMeetings();
  }

  /**
   * Charge les réunions CE pour la plage : 1 mois passé → 3 mois à venir.
   */
  protected loadMeetings(): void {
    this.isLoading.set(true);
    const now = new Date();
    const from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
    const to   = new Date(now.getFullYear(), now.getMonth() + 4, 0);

    this.ethicsService.getMeetings(
      this.isoDate(from),
      this.isoDate(to),
    ).subscribe({
      next: (list) => {
        this.meetings.set(list);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement réunions CE', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de charger les réunions.' });
        this.isLoading.set(false);
      },
    });
  }

  /** Ouvre le dialog de création de réunion. */
  protected openNewMeetingDialog(): void {
    this.resetNewMeetingForm();
    this.showNewMeetingDialog = true;
  }

  /** Ferme le dialog de création et réinitialise le formulaire. */
  protected closeNewMeetingDialog(): void {
    this.showNewMeetingDialog = false;
    this.resetNewMeetingForm();
  }

  /**
   * Soumet la création d'une nouvelle réunion CE.
   * Recharge le calendrier en cas de succès.
   */
  protected createMeeting(): void {
    if (!this.isNewMeetingValid()) return;

    const request: MeetingCreateRequest = {
      meetingDate:  this.isoDate(this.newMeeting.meetingDate as Date),
      meetingTime:  this.newMeeting.meetingTime || undefined,
      meetingType:  this.newMeeting.meetingType!,
      location:     this.newMeeting.location!,
      notes:        this.newMeeting.notes || undefined,
    };

    this.isSaving.set(true);
    this.ethicsService.createMeeting(request).subscribe({
      next: (meeting) => {
        this.messageService.add({ severity: 'success', summary: 'Succès', detail: 'Réunion créée.' });
        this.isSaving.set(false);
        this.closeNewMeetingDialog();
        this.meetings.update(list => [...list, meeting]);
      },
      error: (err) => {
        console.error('Erreur création réunion', err);
        this.messageService.add({ severity: 'error', summary: 'Erreur', detail: 'Impossible de créer la réunion.' });
        this.isSaving.set(false);
      },
    });
  }

  /**
   * Vérifie que les champs obligatoires du formulaire de création sont remplis.
   *
   * @returns vrai si le formulaire est valide
   */
  protected isNewMeetingValid(): boolean {
    return !!(
      this.newMeeting.meetingDate &&
      this.newMeeting.meetingType &&
      this.newMeeting.location?.trim()
    );
  }

  /** Réinitialise le formulaire de création de réunion. */
  private resetNewMeetingForm(): void {
    this.newMeeting = {
      meetingDate: null,
      meetingTime: '',
      meetingType: undefined,
      location: '',
      notes: '',
    };
  }

  /**
   * Retourne la couleur FullCalendar selon le statut de la réunion.
   *
   * @param status clé du statut MeetingStatus
   * @returns code couleur hexadécimal
   */
  private getMeetingColor(status: MeetingStatus | string): string {
    switch (status) {
      case MeetingStatus.PLANNED:     return '#3b82f6';
      case MeetingStatus.IN_PROGRESS: return '#f59e0b';
      case MeetingStatus.COMPLETED:   return '#22c55e';
      case MeetingStatus.CANCELLED:   return '#6b7280';
      default:                        return '#6b7280';
    }
  }

  /**
   * Formate une Date en chaîne ISO 8601 (yyyy-MM-dd).
   *
   * @param date objet Date à formater
   * @returns chaîne ISO 8601
   */
  private isoDate(date: Date): string {
    const y   = date.getFullYear();
    const m   = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${day}`;
  }
}

import { Component, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { CardModule } from 'primeng/card';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { CtcService } from '../../../core/services/ctc.service';
import {
  SEVERITY_COLOR,
  StudyTimeline,
  TimelineEvent,
} from '../../../core/models/ctc.model';

/**
 * Composant affichant la timeline verticale d'une étude clinique.
 *
 * <p>Récupère tous les événements de l'étude (visites, demandes, contrats,
 * événements qualité) via {@link CtcService#getStudyTimeline} et les affiche
 * dans un composant PrimeNG {@code p-timeline} avec icônes et couleurs par type.
 *
 * <p>Si le composant est utilisé en standalone (route directe), il lit le
 * {@code studyId} depuis les paramètres de la route active. Sinon, la valeur
 * peut être passée via l'input signal {@code studyId}.
 */
@Component({
  selector: 'app-study-timeline',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    CardModule,
    TagModule,
    TimelineModule,
  ],
  template: `
    <div class="tw-space-y-4">

      <!-- En-tête -->
      <div>
        <h3 class="tw-text-xl tw-font-bold tw-text-gray-900">Timeline de l'étude</h3>
        <p class="tw-text-sm tw-text-gray-500 tw-mt-0.5 tw-font-mono">{{ effectiveStudyId() }}</p>
      </div>

      <!-- Chargement -->
      @if (isLoading()) {
        <div class="tw-flex tw-justify-center tw-py-12">
          <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
        </div>
      }

      <!-- Timeline vide -->
      @if (!isLoading() && (!timeline() || timeline()!.events.length === 0)) {
        <div class="tw-py-12 tw-text-center tw-text-gray-400">
          <i class="pi pi-calendar tw-text-3xl tw-block tw-mb-2"></i>
          <p class="tw-text-sm">Aucun événement dans la timeline</p>
        </div>
      }

      <!-- Timeline PrimeNG -->
      @if (!isLoading() && timeline() && timeline()!.events.length > 0) {
        <p-timeline [value]="timeline()!.events" layout="vertical" align="left">

          <!-- Marker : icône colorée selon type et sévérité -->
          <ng-template pTemplate="marker" let-event>
            <div class="tw-w-10 tw-h-10 tw-rounded-full tw-flex tw-items-center tw-justify-center tw-border-2 tw-border-white tw-shadow-md"
                 [style.background-color]="getMarkerColor(event)">
              <i [class]="'pi ' + getEventIcon(event) + ' tw-text-white tw-text-sm'"></i>
            </div>
          </ng-template>

          <!-- Contenu : titre, description, date -->
          <ng-template pTemplate="content" let-event>
            <div class="tw-bg-white tw-rounded-xl tw-border tw-border-gray-200 tw-p-4 tw-shadow-sm
                        tw-ml-4 tw-mb-4 tw-min-w-0">
              <div class="tw-flex tw-items-start tw-justify-between tw-gap-2">
                <div class="tw-flex-1 tw-min-w-0">
                  <p class="tw-font-semibold tw-text-gray-800 tw-text-sm">{{ event.title }}</p>
                  @if (event.description) {
                    <p class="tw-text-xs tw-text-gray-500 tw-mt-1 tw-line-clamp-2">{{ event.description }}</p>
                  }
                </div>
                @if (event.severity) {
                  <span class="tw-text-xs tw-font-semibold tw-px-2 tw-py-0.5 tw-rounded-full tw-shrink-0"
                        [style.background-color]="getSeverityBg(event.severity)"
                        [style.color]="getSeverityColor(event.severity)">
                    {{ event.severity }}
                  </span>
                }
              </div>
              <div class="tw-flex tw-items-center tw-gap-1 tw-mt-2 tw-text-xs tw-text-gray-400">
                <i class="pi pi-clock"></i>
                <span>{{ event.eventDate | date:'dd/MM/yyyy' }}</span>
                <span class="tw-mx-1">•</span>
                <span class="tw-uppercase tw-font-medium tw-text-gray-500">{{ event.eventType }}</span>
              </div>
            </div>
          </ng-template>

        </p-timeline>
      }

    </div>
  `,
})
export class StudyTimelineComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly route = inject(ActivatedRoute);

  /**
   * Identifiant de l'étude (optionnel via input, sinon lu depuis la route).
   */
  readonly studyId = input<string>('');

  /** Timeline de l'étude, null pendant le chargement. */
  protected readonly timeline = signal<StudyTimeline | null>(null);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal(false);

  /** Identifiant effectif de l'étude (input ou route). */
  protected effectiveStudyId = signal<string>('');

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const paramId = this.route.snapshot.paramMap.get('studyId');
    const id = this.studyId() || paramId || '';
    this.effectiveStudyId.set(id);
    if (id) {
      this.loadTimeline(id);
    }
  }

  /**
   * Charge la timeline de l'étude depuis l'API.
   *
   * @param studyId identifiant UUID de l'étude
   */
  protected loadTimeline(studyId: string): void {
    this.isLoading.set(true);
    this.ctcService.getStudyTimeline(studyId).subscribe({
      next: (data) => {
        this.timeline.set(data);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement timeline', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Retourne l'icône PrimeNG correspondant au type d'événement.
   *
   * @param event événement de timeline
   * @returns classe icône PrimeNG (sans le préfixe "pi ")
   */
  protected getEventIcon(event: TimelineEvent): string {
    const icons: Record<string, string> = {
      DEVIATION: 'pi-flag',
      SAE:       'pi-exclamation-triangle',
      CAPA:      'pi-check-circle',
      AUDIT:     'pi-search',
      VISIT:     'pi-calendar',
      CONTRACT:  'pi-file',
      DESK:      'pi-inbox',
    };
    return icons[event.eventType] ?? 'pi-circle';
  }

  /**
   * Retourne la couleur de fond du marker selon la sévérité ou un défaut par type.
   *
   * @param event événement de timeline
   * @returns couleur hexadécimale
   */
  protected getMarkerColor(event: TimelineEvent): string {
    if (event.severity && SEVERITY_COLOR[event.severity as keyof typeof SEVERITY_COLOR]) {
      return SEVERITY_COLOR[event.severity as keyof typeof SEVERITY_COLOR];
    }
    const defaults: Record<string, string> = {
      VISIT:    '#3b82f6',
      CONTRACT: '#10b981',
      DESK:     '#6366f1',
    };
    return defaults[event.eventType] ?? '#94a3b8';
  }

  /**
   * Retourne la couleur de texte pour un badge de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale
   */
  protected getSeverityColor(severity: string): string {
    return SEVERITY_COLOR[severity as keyof typeof SEVERITY_COLOR] ?? '#6b7280';
  }

  /**
   * Retourne la couleur de fond (opacité réduite) pour un badge de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale avec opacité
   */
  protected getSeverityBg(severity: string): string {
    const colors: Record<string, string> = {
      LOW:      '#d1fae5',
      MEDIUM:   '#fef3c7',
      HIGH:     '#fee2e2',
      CRITICAL: '#ede9fe',
    };
    return colors[severity] ?? '#f3f4f6';
  }
}

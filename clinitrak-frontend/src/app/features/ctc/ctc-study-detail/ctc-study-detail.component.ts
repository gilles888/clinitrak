import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';
import { TabViewModule } from 'primeng/tabview';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { CtcService } from '../../../core/services/ctc.service';
import {
  CONTRACT_STATUS_SEVERITY,
  EVENT_STATUS_SEVERITY,
  FinancialContract,
  MonitoringVisit,
  QualityEvent,
  SEVERITY_COLOR,
  STATISTICS_STATUS_SEVERITY,
  StatisticsRequest,
  StudyTimeline,
  TimelineEvent,
  VISIT_STATUS_SEVERITY,
} from '../../../core/models/ctc.model';

/**
 * Composant détail d'une étude dans le module CTC.
 *
 * <p>Affiche 5 onglets {@code p-tabView} :
 * <ol>
 *   <li>Timeline — 5 derniers événements de l'étude</li>
 *   <li>Visites monitoring — visites filtrées par studyId</li>
 *   <li>Contrats financiers — contrats filtrés par studyId</li>
 *   <li>Qualité — événements qualité filtrés par studyId</li>
 *   <li>Statistiques — demandes filtrées par studyId</li>
 * </ol>
 *
 * <p>L'identifiant de l'étude est lu depuis les paramètres de la route active,
 * ou peut être passé via l'input signal {@link studyId}.
 */
@Component({
  selector: 'app-ctc-study-detail',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    ButtonModule,
    TableModule,
    TabViewModule,
    TagModule,
    TimelineModule,
  ],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tête -->
      <div class="tw-flex tw-items-center tw-gap-3">
        <p-button
          icon="pi pi-arrow-left"
          severity="secondary"
          [text]="true"
          [rounded]="true"
          routerLink="/ctc"
          pTooltip="Retour au CTC"
        />
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Détail de l'étude</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-font-mono tw-mt-0.5">{{ effectiveStudyId() }}</p>
        </div>
        <div class="tw-ml-auto">
          <p-button
            label="Timeline complète"
            icon="pi pi-calendar"
            severity="secondary"
            [outlined]="true"
            size="small"
            [routerLink]="['/ctc/study', effectiveStudyId(), 'timeline']"
          />
        </div>
      </div>

      <!-- Tabs -->
      <p-tabView>

        <!-- Onglet 1 : Timeline -->
        <p-tabPanel header="Timeline">
          <div class="tw-py-4">
            @if (isLoadingTimeline()) {
              <div class="tw-flex tw-justify-center tw-py-8">
                <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
              </div>
            } @else if (!timeline() || recentTimelineEvents().length === 0) {
              <div class="tw-py-8 tw-text-center tw-text-gray-400">
                <i class="pi pi-calendar tw-text-2xl tw-block tw-mb-2"></i>
                <p class="tw-text-sm">Aucun événement dans la timeline</p>
              </div>
            } @else {
              <p-timeline [value]="recentTimelineEvents()" layout="vertical" align="left">
                <ng-template pTemplate="marker" let-event>
                  <div class="tw-w-8 tw-h-8 tw-rounded-full tw-flex tw-items-center tw-justify-center
                               tw-border-2 tw-border-white tw-shadow"
                       [style.background-color]="getTimelineMarkerColor(event)">
                    <i [class]="'pi ' + getTimelineIcon(event) + ' tw-text-white tw-text-xs'"></i>
                  </div>
                </ng-template>
                <ng-template pTemplate="content" let-event>
                  <div class="tw-bg-white tw-rounded-lg tw-border tw-border-gray-200 tw-p-3 tw-ml-3 tw-mb-3 tw-shadow-sm">
                    <p class="tw-font-semibold tw-text-gray-800 tw-text-sm">{{ event.title }}</p>
                    @if (event.description) {
                      <p class="tw-text-xs tw-text-gray-500 tw-mt-0.5">{{ event.description }}</p>
                    }
                    <p class="tw-text-xs tw-text-gray-400 tw-mt-1">
                      {{ event.eventDate | date:'dd/MM/yyyy' }} — {{ event.eventType }}
                    </p>
                  </div>
                </ng-template>
              </p-timeline>
            }
          </div>
        </p-tabPanel>

        <!-- Onglet 2 : Visites monitoring -->
        <p-tabPanel header="Visites monitoring">
          <div class="tw-py-4">
            @if (isLoadingVisits()) {
              <div class="tw-flex tw-justify-center tw-py-8">
                <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
              </div>
            } @else {
              <p-table
                [value]="studyVisits()"
                emptyMessage="Aucune visite de monitoring pour cette étude"
                styleClass="tw-text-sm"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Date</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">CRA</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Statut</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-visit>
                  <tr class="hover:tw-bg-gray-50">
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ visit.visitDate | date:'dd/MM/yyyy' }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ visit.visitTypeLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ visit.monitorName }}</td>
                    <td class="tw-px-3 tw-py-2">
                      <p-tag [value]="visit.statusLabel" [severity]="getVisitSeverity(visit)" />
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }
          </div>
        </p-tabPanel>

        <!-- Onglet 3 : Contrats financiers -->
        <p-tabPanel header="Contrats financiers">
          <div class="tw-py-4">
            @if (isLoadingContracts()) {
              <div class="tw-flex tw-justify-center tw-py-8">
                <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
              </div>
            } @else {
              <p-table
                [value]="studyContracts()"
                emptyMessage="Aucun contrat financier pour cette étude"
                styleClass="tw-text-sm"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Date</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Montant</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Statut</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-contract>
                  <tr class="hover:tw-bg-gray-50">
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ contract.contractTypeLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ contract.contractDate | date:'dd/MM/yyyy' }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-font-semibold">
                      {{ contract.amount | number:'1.2-2' }} {{ contract.currencyLabel }}
                    </td>
                    <td class="tw-px-3 tw-py-2">
                      <p-tag [value]="contract.statusLabel" [severity]="getContractSeverity(contract)" />
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }
          </div>
        </p-tabPanel>

        <!-- Onglet 4 : Qualité -->
        <p-tabPanel header="Qualité">
          <div class="tw-py-4">
            @if (isLoadingQuality()) {
              <div class="tw-flex tw-justify-center tw-py-8">
                <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
              </div>
            } @else {
              <p-table
                [value]="studyQualityEvents()"
                emptyMessage="Aucun événement qualité pour cette étude"
                styleClass="tw-text-sm"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Date</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Sévérité</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Statut</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Description</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-event>
                  <tr class="hover:tw-bg-gray-50">
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ event.eventTypeLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ event.eventDate | date:'dd/MM/yyyy' }}</td>
                    <td class="tw-px-3 tw-py-2">
                      <span class="tw-text-xs tw-font-semibold tw-px-2 tw-py-0.5 tw-rounded-full"
                            [style.background-color]="getSeverityBg(event.severity)"
                            [style.color]="getSeverityColor(event.severity)">
                        {{ event.severityLabel }}
                      </span>
                    </td>
                    <td class="tw-px-3 tw-py-2">
                      <p-tag [value]="event.statusLabel" [severity]="getEventStatusSeverity(event)" />
                    </td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-600 tw-max-w-[200px] tw-truncate">
                      {{ event.description }}
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }
          </div>
        </p-tabPanel>

        <!-- Onglet 5 : Statistiques -->
        <p-tabPanel header="Statistiques">
          <div class="tw-py-4">
            @if (isLoadingStats()) {
              <div class="tw-flex tw-justify-center tw-py-8">
                <i class="pi pi-spin pi-spinner tw-text-3xl tw-text-blue-500"></i>
              </div>
            } @else {
              <p-table
                [value]="studyStatsRequests()"
                emptyMessage="Aucune demande statistique pour cette étude"
                styleClass="tw-text-sm"
              >
                <ng-template pTemplate="header">
                  <tr class="tw-bg-gray-50">
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Demandeur</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Type analyse</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Format</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Deadline</th>
                    <th class="tw-font-semibold tw-text-xs tw-text-gray-600 tw-px-3 tw-py-2">Statut</th>
                  </tr>
                </ng-template>
                <ng-template pTemplate="body" let-req>
                  <tr class="hover:tw-bg-gray-50">
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ req.requestorName }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ req.analysisTypeLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs tw-text-gray-500">{{ req.dataFormatLabel }}</td>
                    <td class="tw-px-3 tw-py-2 tw-text-xs">{{ req.deadline | date:'dd/MM/yyyy' }}</td>
                    <td class="tw-px-3 tw-py-2">
                      <p-tag [value]="req.statusLabel" [severity]="getStatsSeverity(req)" />
                    </td>
                  </tr>
                </ng-template>
              </p-table>
            }
          </div>
        </p-tabPanel>

      </p-tabView>

    </div>
  `,
})
export class CTCStudyDetailComponent implements OnInit {

  private readonly ctcService = inject(CtcService);
  private readonly route = inject(ActivatedRoute);

  /**
   * Identifiant de l'étude (optionnel via input signal, sinon lu depuis la route).
   */
  readonly studyId = input<string>('');

  /** Identifiant effectif de l'étude. */
  protected readonly effectiveStudyId = signal<string>('');

  // Données de chaque onglet
  protected readonly timeline          = signal<StudyTimeline | null>(null);
  protected readonly studyVisits       = signal<MonitoringVisit[]>([]);
  protected readonly studyContracts    = signal<FinancialContract[]>([]);
  protected readonly studyQualityEvents= signal<QualityEvent[]>([]);
  protected readonly studyStatsRequests= signal<StatisticsRequest[]>([]);

  // Indicateurs de chargement par onglet
  protected readonly isLoadingTimeline  = signal(false);
  protected readonly isLoadingVisits    = signal(false);
  protected readonly isLoadingContracts = signal(false);
  protected readonly isLoadingQuality   = signal(false);
  protected readonly isLoadingStats     = signal(false);

  /** Les 5 derniers événements de la timeline. */
  protected readonly recentTimelineEvents = computed(() =>
    (this.timeline()?.events ?? []).slice(0, 5),
  );

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    const paramId = this.route.snapshot.paramMap.get('studyId');
    const id = this.studyId() || paramId || '';
    this.effectiveStudyId.set(id);
    if (id) {
      this.loadAllData(id);
    }
  }

  /**
   * Lance le chargement parallèle de toutes les données de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadAllData(studyId: string): void {
    this.loadTimeline(studyId);
    this.loadVisits(studyId);
    this.loadContracts(studyId);
    this.loadQualityEvents(studyId);
    this.loadStatsRequests(studyId);
  }

  /**
   * Charge la timeline de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadTimeline(studyId: string): void {
    this.isLoadingTimeline.set(true);
    this.ctcService.getStudyTimeline(studyId).subscribe({
      next: (data) => { this.timeline.set(data); this.isLoadingTimeline.set(false); },
      error: (err) => { console.error('Erreur timeline', err); this.isLoadingTimeline.set(false); },
    });
  }

  /**
   * Charge et filtre les visites de monitoring de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadVisits(studyId: string): void {
    this.isLoadingVisits.set(true);
    this.ctcService.getMonitoringVisits().subscribe({
      next: (data) => {
        this.studyVisits.set(data.filter(v => v.studyId === studyId));
        this.isLoadingVisits.set(false);
      },
      error: (err) => { console.error('Erreur visites', err); this.isLoadingVisits.set(false); },
    });
  }

  /**
   * Charge et filtre les contrats financiers de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadContracts(studyId: string): void {
    this.isLoadingContracts.set(true);
    this.ctcService.getFinancialContracts().subscribe({
      next: (data) => {
        this.studyContracts.set(data.filter(c => c.studyId === studyId));
        this.isLoadingContracts.set(false);
      },
      error: (err) => { console.error('Erreur contrats', err); this.isLoadingContracts.set(false); },
    });
  }

  /**
   * Charge et filtre les événements qualité de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadQualityEvents(studyId: string): void {
    this.isLoadingQuality.set(true);
    this.ctcService.getQualityEvents().subscribe({
      next: (data) => {
        this.studyQualityEvents.set(data.filter(e => e.studyId === studyId));
        this.isLoadingQuality.set(false);
      },
      error: (err) => { console.error('Erreur qualité', err); this.isLoadingQuality.set(false); },
    });
  }

  /**
   * Charge et filtre les demandes statistiques de l'étude.
   *
   * @param studyId identifiant UUID de l'étude
   */
  private loadStatsRequests(studyId: string): void {
    this.isLoadingStats.set(true);
    this.ctcService.getStatisticsRequests().subscribe({
      next: (data) => {
        this.studyStatsRequests.set(data.filter(r => r.studyId === studyId));
        this.isLoadingStats.set(false);
      },
      error: (err) => { console.error('Erreur statistiques', err); this.isLoadingStats.set(false); },
    });
  }

  // ─── Helpers d'affichage ────────────────────────────────────

  /**
   * Retourne l'icône PrimeNG pour un événement de timeline.
   *
   * @param event événement de timeline
   * @returns classe d'icône PrimeNG
   */
  protected getTimelineIcon(event: TimelineEvent): string {
    const icons: Record<string, string> = {
      DEVIATION: 'pi-flag', SAE: 'pi-exclamation-triangle',
      CAPA: 'pi-check-circle', AUDIT: 'pi-search',
      VISIT: 'pi-calendar', CONTRACT: 'pi-file', DESK: 'pi-inbox',
    };
    return icons[event.eventType] ?? 'pi-circle';
  }

  /**
   * Retourne la couleur du marker d'un événement de timeline.
   *
   * @param event événement de timeline
   * @returns couleur hexadécimale
   */
  protected getTimelineMarkerColor(event: TimelineEvent): string {
    if (event.severity && SEVERITY_COLOR[event.severity as keyof typeof SEVERITY_COLOR]) {
      return SEVERITY_COLOR[event.severity as keyof typeof SEVERITY_COLOR];
    }
    const defaults: Record<string, string> = {
      VISIT: '#3b82f6', CONTRACT: '#10b981', DESK: '#6366f1',
    };
    return defaults[event.eventType] ?? '#94a3b8';
  }

  /**
   * Retourne la couleur de texte pour un niveau de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale
   */
  protected getSeverityColor(severity: string): string {
    return SEVERITY_COLOR[severity as keyof typeof SEVERITY_COLOR] ?? '#6b7280';
  }

  /**
   * Retourne la couleur de fond pour un niveau de sévérité.
   *
   * @param severity valeur de sévérité
   * @returns couleur hexadécimale claire
   */
  protected getSeverityBg(severity: string): string {
    const bgs: Record<string, string> = {
      LOW: '#d1fae5', MEDIUM: '#fef3c7', HIGH: '#fee2e2', CRITICAL: '#ede9fe',
    };
    return bgs[severity] ?? '#f3f4f6';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une visite.
   *
   * @param visit visite de monitoring
   * @returns sévérité PrimeNG
   */
  protected getVisitSeverity(visit: MonitoringVisit): string {
    return VISIT_STATUS_SEVERITY[visit.status] ?? 'info';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'un contrat.
   *
   * @param contract contrat financier
   * @returns sévérité PrimeNG
   */
  protected getContractSeverity(contract: FinancialContract): string {
    return CONTRACT_STATUS_SEVERITY[contract.status] ?? 'info';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'un événement qualité.
   *
   * @param event événement qualité
   * @returns sévérité PrimeNG
   */
  protected getEventStatusSeverity(event: QualityEvent): string {
    return EVENT_STATUS_SEVERITY[event.status] ?? 'info';
  }

  /**
   * Retourne la sévérité PrimeNG Tag pour le statut d'une demande statistique.
   *
   * @param req demande statistique
   * @returns sévérité PrimeNG
   */
  protected getStatsSeverity(req: StatisticsRequest): string {
    return STATISTICS_STATUS_SEVERITY[req.status] ?? 'info';
  }
}

import { Component, inject, OnInit, signal } from '@angular/core';
import { SlicePipe } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { TableModule } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { TimelineModule } from 'primeng/timeline';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { ExchangeService } from '../../../core/services/exchange.service';
import {
  EXCHANGE_STATUS_SEVERITY,
  ExchangeRequest,
  ExchangeStatus,
} from '../../../core/models/exchange.model';

/** Entrée pour la timeline du statut d'une demande. */
interface StatusTimelineEntry {
  status: string;
  date: string | undefined;
  icon: string;
  color: string;
}

/**
 * Composant de suivi des demandes Exchange.
 *
 * <p>Affiche la liste des demandes de l'utilisateur connecté avec leur statut.
 * En sélectionnant une demande, l'utilisateur peut voir la timeline de statuts
 * et accéder à la messagerie associée.
 */
@Component({
  selector: 'app-request-tracking',
  standalone: true,
  imports: [SlicePipe, RouterLink, TableModule, TagModule, TimelineModule, ButtonModule, CardModule],
  template: `
    <div class="tw-min-h-screen tw-bg-gray-50 tw-p-6">
      <div class="tw-max-w-6xl tw-mx-auto">

        <!-- En-tête -->
        <div class="tw-flex tw-justify-between tw-items-center tw-mb-6">
          <div>
            <a routerLink="/exchange" class="tw-text-blue-600 hover:tw-underline tw-text-sm">
              &larr; Accueil Exchange
            </a>
            <h1 class="tw-text-2xl tw-font-bold tw-text-gray-900 tw-mt-1">Mes demandes</h1>
          </div>
          <a routerLink="/exchange/submit" pButton label="Nouvelle demande" icon="pi pi-plus"></a>
        </div>

        <div class="tw-grid tw-grid-cols-1 lg:tw-grid-cols-3 tw-gap-6">

          <!-- Liste des demandes -->
          <div class="lg:tw-col-span-2">
            <p-card>
              @if (isLoading()) {
                <div class="tw-text-center tw-py-8 tw-text-gray-400">
                  <i class="pi pi-spin pi-spinner tw-text-2xl"></i>
                  <p class="tw-mt-2">Chargement des demandes...</p>
                </div>
              } @else if (requests().length === 0) {
                <div class="tw-text-center tw-py-12">
                  <i class="pi pi-inbox tw-text-5xl tw-text-gray-300 tw-block tw-mb-4"></i>
                  <p class="tw-text-gray-500 tw-mb-4">Aucune demande pour le moment</p>
                  <a routerLink="/exchange/submit" pButton label="Soumettre une première demande"></a>
                </div>
              } @else {
                <p-table
                  [value]="requests()"
                  [paginator]="true"
                  [rows]="10"
                  styleClass="tw-text-sm"
                  selectionMode="single"
                  [(selection)]="selectedRequestBinding"
                  (onRowSelect)="onRowSelect($event)"
                >
                  <ng-template pTemplate="header">
                    <tr>
                      <th class="tw-text-left">Titre</th>
                      <th class="tw-text-left">Module</th>
                      <th class="tw-text-left">Type</th>
                      <th class="tw-text-left">Statut</th>
                      <th class="tw-text-left">Date</th>
                      <th></th>
                    </tr>
                  </ng-template>
                  <ng-template pTemplate="body" let-req>
                    <tr [class.tw-bg-blue-50]="selectedRequest()?.id === req.id" class="tw-cursor-pointer">
                      <td class="tw-font-medium tw-text-gray-800 tw-max-w-xs tw-truncate">{{ req.title }}</td>
                      <td class="tw-text-gray-600">{{ req.targetModuleLabel }}</td>
                      <td class="tw-text-gray-600">{{ req.requestTypeLabel }}</td>
                      <td>
                        <p-tag
                          [value]="req.statusLabel"
                          [severity]="getStatusSeverity(req.status)"
                        />
                      </td>
                      <td class="tw-text-gray-500 tw-text-xs">{{ req.createdAt | slice:0:10 }}</td>
                      <td>
                        <a
                          [routerLink]="['/exchange/messages', req.id]"
                          pButton
                          icon="pi pi-comments"
                          severity="secondary"
                          size="small"
                          title="Messagerie"
                        ></a>
                      </td>
                    </tr>
                  </ng-template>
                </p-table>
              }
            </p-card>
          </div>

          <!-- Détail / Timeline -->
          <div>
            @if (selectedRequest()) {
              <p-card [header]="'Détail — ' + (selectedRequest()!.title | slice:0:30)">

                <!-- Statut courant -->
                <div class="tw-mb-4">
                  <p class="tw-text-xs tw-text-gray-500 tw-mb-1">Statut actuel</p>
                  <p-tag
                    [value]="selectedRequest()!.statusLabel"
                    [severity]="getStatusSeverity(selectedRequest()!.status)"
                  />
                </div>

                <!-- Infos -->
                <div class="tw-text-sm tw-space-y-2 tw-mb-4">
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-gray-500">Module :</span>
                    <span class="tw-font-medium">{{ selectedRequest()!.targetModuleLabel }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-gray-500">Type :</span>
                    <span class="tw-font-medium">{{ selectedRequest()!.requestTypeLabel }}</span>
                  </div>
                  <div class="tw-flex tw-justify-between">
                    <span class="tw-text-gray-500">Documents :</span>
                    <span class="tw-font-medium">{{ selectedRequest()!.documents.length }}</span>
                  </div>
                </div>

                <!-- Timeline statuts -->
                <p class="tw-text-xs tw-text-gray-500 tw-uppercase tw-font-semibold tw-mb-3">Historique</p>
                <p-timeline [value]="buildTimeline(selectedRequest()!)" layout="vertical" styleClass="tw-text-xs">
                  <ng-template pTemplate="marker" let-entry>
                    <span
                      class="tw-w-6 tw-h-6 tw-rounded-full tw-flex tw-items-center tw-justify-center"
                      [style.background-color]="entry.color"
                    >
                      <i [class]="'pi ' + entry.icon + ' tw-text-white tw-text-xs'"></i>
                    </span>
                  </ng-template>
                  <ng-template pTemplate="content" let-entry>
                    <div class="tw-pb-3">
                      <p class="tw-font-medium tw-text-gray-700">{{ entry.status }}</p>
                      @if (entry.date) {
                        <p class="tw-text-gray-400 tw-text-xs">{{ entry.date | slice:0:10 }}</p>
                      }
                    </div>
                  </ng-template>
                </p-timeline>

                <!-- Lien messagerie -->
                <a
                  [routerLink]="['/exchange/messages', selectedRequest()!.id]"
                  pButton
                  label="Ouvrir la messagerie"
                  icon="pi pi-comments"
                  styleClass="tw-w-full tw-mt-2"
                  severity="secondary"
                ></a>
              </p-card>
            } @else {
              <p-card>
                <div class="tw-text-center tw-py-8 tw-text-gray-400">
                  <i class="pi pi-hand-pointer tw-text-3xl tw-block tw-mb-2"></i>
                  <p class="tw-text-sm">Sélectionnez une demande<br>pour voir son détail</p>
                </div>
              </p-card>
            }
          </div>
        </div>
      </div>
    </div>
  `,
})
export class RequestTrackingComponent implements OnInit {

  private readonly exchangeService = inject(ExchangeService);
  private readonly route = inject(ActivatedRoute);

  /** Liste des demandes de l'utilisateur. */
  protected readonly requests = signal<ExchangeRequest[]>([]);

  /** Demande sélectionnée pour l'affichage du détail. */
  protected readonly selectedRequest = signal<ExchangeRequest | null>(null);

  /** Binding two-way pour la sélection de ligne dans p-table. */
  protected selectedRequestBinding: ExchangeRequest | null = null;

  /** Indique si le chargement est en cours. */
  protected readonly isLoading = signal(false);

  /** Sévérité PrimeNG par statut. */
  protected getStatusSeverity(status: ExchangeStatus): string {
    return EXCHANGE_STATUS_SEVERITY[status] ?? 'secondary';
  }

  /** Charge les demandes et présélectionne si un ID est fourni en route param. */
  ngOnInit(): void {
    this.isLoading.set(true);
    this.exchangeService.getMyRequests().subscribe({
      next: (reqs) => {
        this.requests.set(reqs);
        this.isLoading.set(false);

        // Présélection via param de route
        const id = this.route.snapshot.paramMap.get('id');
        if (id) {
          const found = reqs.find(r => r.id === id);
          if (found) {
            this.selectedRequest.set(found);
            this.selectedRequestBinding = found;
          }
        }
      },
      error: () => this.isLoading.set(false),
    });
  }

  /**
   * Sélectionne une demande au clic sur une ligne de tableau.
   *
   * @param event Événement de sélection PrimeNG
   */
  protected onRowSelect(event: { data: ExchangeRequest }): void {
    this.selectedRequest.set(event.data);
  }

  /**
   * Construit les entrées de la timeline pour une demande.
   *
   * @param req La demande Exchange
   * @returns Tableau d'entrées pour p-timeline
   */
  protected buildTimeline(req: ExchangeRequest): StatusTimelineEntry[] {
    const entries: StatusTimelineEntry[] = [
      { status: 'Créée',          date: req.createdAt,         icon: 'pi-plus',       color: '#64748b' },
      { status: 'Soumise',        date: req.submissionDate,    icon: 'pi-send',       color: '#3b82f6' },
      { status: 'En cours d\'examen', date: undefined,         icon: 'pi-search',     color: '#f59e0b' },
    ];

    if (req.status === ExchangeStatus.ACCEPTED) {
      entries.push({ status: 'Acceptée', date: undefined, icon: 'pi-check', color: '#10b981' });
    } else if (req.status === ExchangeStatus.REJECTED) {
      entries.push({ status: 'Refusée', date: undefined, icon: 'pi-times', color: '#ef4444' });
    } else if (req.status === ExchangeStatus.MORE_INFO) {
      entries.push({ status: 'Informations demandées', date: undefined, icon: 'pi-question', color: '#f59e0b' });
    }

    return entries.filter(e => e.date || req.status !== ExchangeStatus.DRAFT);
  }
}

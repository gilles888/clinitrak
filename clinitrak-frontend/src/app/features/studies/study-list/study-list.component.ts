import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';
import { PanelModule } from 'primeng/panel';
import { TableModule, TableLazyLoadEvent } from 'primeng/table';
import { TagModule } from 'primeng/tag';
import { ToastModule } from 'primeng/toast';
import { MessageService } from 'primeng/api';
import { StudyService } from '../../../core/services/study.service';
import { hasAnyRole } from '../../../core/store/auth.store';
import { SystemRole } from '../../../core/models/user.model';
import {
  STUDY_PHASE_OPTIONS,
  STUDY_STATUS_OPTIONS,
  STUDY_TYPE_OPTIONS,
  SPONSOR_TYPE_OPTIONS,
  StudySearchCriteria,
  StudySummaryResponse,
} from '../../../core/models/study.model';
import { StudyStatusBadgeComponent } from '../../../shared/components/study-status-badge/study-status-badge.component';

/** Événement de pagination PrimeNG Table. */
interface TablePageEvent {
  first: number;
  rows: number;
  page: number;
  pageCount: number;
}

/** Événement de tri PrimeNG Table. */
interface TableSortEvent {
  field: string;
  order: 1 | -1;
}

/**
 * Composant de liste des études cliniques.
 *
 * <p>Affiche un panneau de recherche avancée et une table paginée et triable
 * des études cliniques. Utilise le chargement lazy (server-side) pour gérer
 * de grands volumes de données.
 *
 * <p>Les données sont récupérées via {@link StudyService#searchStudies}.
 * Les erreurs sont affichées via {@link MessageService} (PrimeNG Toast).
 */
@Component({
  selector: 'app-study-list',
  standalone: true,
  imports: [
    CommonModule,
    DatePipe,
    FormsModule,
    RouterLink,
    ButtonModule,
    CardModule,
    CheckboxModule,
    DropdownModule,
    InputTextModule,
    PanelModule,
    TableModule,
    TagModule,
    ToastModule,
    StudyStatusBadgeComponent,
  ],
  providers: [MessageService],
  template: `
    <p-toast />

    <div class="tw-space-y-4">

      <!-- En-tête de page -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Études cliniques</h2>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            {{ totalRecords() }} étude(s) trouvée(s)
          </p>
        </div>
        @if (canCreate()) {
          <p-button
            label="Nouvelle étude"
            icon="pi pi-plus"
            routerLink="/studies/new"
            severity="primary"
          />
        }
      </div>

      <!-- Panneau de recherche -->
      <p-panel header="Recherche avancée" [toggleable]="true" [collapsed]="false"
               styleClass="tw-border tw-border-gray-200">
        <div class="tw-grid tw-grid-cols-1 sm:tw-grid-cols-2 lg:tw-grid-cols-3 xl:tw-grid-cols-4 tw-gap-4">

          <!-- Mot-clé titre -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Titre / Mot-clé</label>
            <input
              pInputText
              [(ngModel)]="searchForm.titleKeyword"
              placeholder="Rechercher dans le titre..."
              class="tw-w-full"
            />
          </div>

          <!-- Acronyme -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Acronyme</label>
            <input
              pInputText
              [(ngModel)]="searchForm.acronym"
              placeholder="Acronyme..."
              class="tw-w-full"
            />
          </div>

          <!-- Numéro éthique -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">N° Éthique</label>
            <input
              pInputText
              [(ngModel)]="searchForm.ethicsNumber"
              placeholder="N° comité d'éthique..."
              class="tw-w-full"
            />
          </div>

          <!-- EudraCT -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">N° EudraCT</label>
            <input
              pInputText
              [(ngModel)]="searchForm.eudractNumber"
              placeholder="EudraCT..."
              class="tw-w-full"
            />
          </div>

          <!-- Type d'étude -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Type d'étude</label>
            <p-dropdown
              [options]="studyTypeOptions"
              [(ngModel)]="searchForm.studyType"
              optionLabel="label"
              optionValue="value"
              placeholder="Tous les types"
              [showClear]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Statut -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Statut</label>
            <p-dropdown
              [options]="studyStatusOptions"
              [(ngModel)]="searchForm.status"
              optionLabel="label"
              optionValue="value"
              placeholder="Tous les statuts"
              [showClear]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Phase -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Phase</label>
            <p-dropdown
              [options]="studyPhaseOptions"
              [(ngModel)]="searchForm.phase"
              optionLabel="label"
              optionValue="value"
              placeholder="Toutes les phases"
              [showClear]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Type sponsor -->
          <div class="tw-flex tw-flex-col tw-gap-1">
            <label class="tw-text-sm tw-font-medium tw-text-gray-700">Type sponsor</label>
            <p-dropdown
              [options]="sponsorTypeOptions"
              [(ngModel)]="searchForm.sponsorType"
              optionLabel="label"
              optionValue="value"
              placeholder="Tous les sponsors"
              [showClear]="true"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Sponsor CUSL -->
          <div class="tw-flex tw-items-center tw-gap-2 tw-pt-5">
            <p-checkbox
              [(ngModel)]="searchForm.isSponsorCusl"
              [binary]="true"
              inputId="isSponsorCusl"
            />
            <label for="isSponsorCusl" class="tw-text-sm tw-font-medium tw-text-gray-700 tw-cursor-pointer">
              Sponsor CUSL uniquement
            </label>
          </div>

        </div>

        <!-- Boutons d'action -->
        <div class="tw-flex tw-gap-2 tw-mt-4 tw-pt-4 tw-border-t tw-border-gray-200">
          <p-button
            label="Rechercher"
            icon="pi pi-search"
            (onClick)="onSearch()"
            [loading]="isLoading()"
          />
          <p-button
            label="Réinitialiser"
            icon="pi pi-refresh"
            severity="secondary"
            (onClick)="onReset()"
          />
        </div>
      </p-panel>

      <!-- Table des études -->
      <p-table
        [value]="studies()"
        [lazy]="true"
        [paginator]="true"
        [rows]="pageSize()"
        [totalRecords]="totalRecords()"
        [loading]="isLoading()"
        [rowsPerPageOptions]="[10, 20, 50]"
        (onLazyLoad)="onLazyLoad($event)"
        styleClass="tw-border tw-border-gray-200 tw-rounded-lg"
        [rowHover]="true"
        dataKey="id"
      >
        <ng-template pTemplate="header">
          <tr class="tw-bg-gray-50">
            <th pSortableColumn="studyNumber" class="tw-font-semibold">
              N° Étude <p-sortIcon field="studyNumber" />
            </th>
            <th pSortableColumn="title" class="tw-font-semibold">
              Titre <p-sortIcon field="title" />
            </th>
            <th class="tw-font-semibold">Acronyme</th>
            <th class="tw-font-semibold">Type</th>
            <th class="tw-font-semibold">Statut</th>
            <th class="tw-font-semibold">Investigateur principal</th>
            <th pSortableColumn="startDate" class="tw-font-semibold">
              Début <p-sortIcon field="startDate" />
            </th>
            <th class="tw-font-semibold tw-text-center">Inclusion</th>
            <th class="tw-font-semibold tw-text-center">CUSL</th>
            <th class="tw-font-semibold tw-text-center">Actions</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-row>
          <tr>
            <td>
              <a
                [routerLink]="['/studies', row.id]"
                class="tw-font-mono tw-text-blue-600 hover:tw-underline tw-text-sm"
              >
                {{ row.studyNumber }}
              </a>
            </td>
            <td class="tw-max-w-xs">
              <span class="tw-line-clamp-2 tw-text-sm" [title]="row.title">
                {{ row.title }}
              </span>
            </td>
            <td>
              <span class="tw-text-sm tw-text-gray-600">{{ row.acronym ?? '—' }}</span>
            </td>
            <td>
              <span class="tw-text-sm">{{ row.studyTypeLabel ?? row.studyType }}</span>
            </td>
            <td>
              <ct-study-status-badge [status]="row.currentStatus" />
            </td>
            <td class="tw-text-sm">{{ row.principalInvestigator }}</td>
            <td class="tw-text-sm">
              {{ row.startDate ? (row.startDate | date:'dd/MM/yyyy') : '—' }}
            </td>
            <td class="tw-text-center tw-text-sm">
              @if (row.targetEnrollment) {
                <span>{{ row.currentEnrollment }}/{{ row.targetEnrollment }}</span>
              } @else {
                <span>{{ row.currentEnrollment }}</span>
              }
            </td>
            <td class="tw-text-center">
              @if (row.isSponsorCusl) {
                <i class="pi pi-check-circle tw-text-green-600"></i>
              } @else {
                <i class="pi pi-minus tw-text-gray-300"></i>
              }
            </td>
            <td>
              <div class="tw-flex tw-gap-1 tw-justify-center">
                <p-button
                  icon="pi pi-eye"
                  [routerLink]="['/studies', row.id]"
                  severity="secondary"
                  size="small"
                  [rounded]="true"
                  [text]="true"
                  pTooltip="Voir le détail"
                />
                @if (canEdit()) {
                  <p-button
                    icon="pi pi-pencil"
                    [routerLink]="['/studies', row.id, 'edit']"
                    severity="info"
                    size="small"
                    [rounded]="true"
                    [text]="true"
                    pTooltip="Modifier"
                  />
                }
              </div>
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="10" class="tw-text-center tw-py-12 tw-text-gray-500">
              <i class="pi pi-search tw-text-4xl tw-mb-4 tw-block tw-text-gray-300"></i>
              Aucune étude trouvée pour ces critères de recherche.
            </td>
          </tr>
        </ng-template>

        <ng-template pTemplate="loadingbody">
          <tr>
            <td colspan="10" class="tw-text-center tw-py-12">
              <i class="pi pi-spin pi-spinner tw-text-4xl tw-text-blue-500"></i>
            </td>
          </tr>
        </ng-template>
      </p-table>

    </div>
  `,
})
export class StudyListComponent implements OnInit {

  private readonly studyService = inject(StudyService);
  private readonly router = inject(Router);

  // ─── Options de dropdowns ───────────────────────────────────
  protected readonly studyTypeOptions    = STUDY_TYPE_OPTIONS;
  protected readonly studyStatusOptions  = STUDY_STATUS_OPTIONS;
  protected readonly studyPhaseOptions   = STUDY_PHASE_OPTIONS;
  protected readonly sponsorTypeOptions  = SPONSOR_TYPE_OPTIONS;

  // ─── Permissions RBAC ───────────────────────────────────────
  /** Vrai si l'utilisateur peut créer une étude. */
  protected readonly canCreate = hasAnyRole(
    SystemRole.CTC_PM, SystemRole.CTC_CRA, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN
  );

  /** Vrai si l'utilisateur peut modifier une étude. */
  protected readonly canEdit = hasAnyRole(
    SystemRole.CTC_PM, SystemRole.CTC_CRA, SystemRole.ADMIN_TENANT, SystemRole.SUPER_ADMIN
  );

  // ─── État réactif (signals) ──────────────────────────────────
  /** Liste des études de la page courante. */
  protected readonly studies       = signal<StudySummaryResponse[]>([]);
  /** Nombre total d'études correspondant aux critères. */
  protected readonly totalRecords  = signal(0);
  /** Indicateur de chargement en cours. */
  protected readonly isLoading     = signal(false);
  /** Critères de recherche courants. */
  protected readonly searchCriteria = signal<StudySearchCriteria>({});
  /** Page courante (0-based). */
  protected readonly currentPage   = signal(0);
  /** Nombre d'éléments par page. */
  protected readonly pageSize      = signal(20);
  /** Tri courant (ex: "studyNumber,asc"). */
  private currentSort = signal<string | undefined>(undefined);

  // ─── Formulaire de recherche (modèle lié via ngModel) ───────
  protected searchForm: Partial<StudySearchCriteria> = {};

  // ─────────────────────────────────────────────────────────────

  ngOnInit(): void {
    this.loadStudies();
  }

  /**
   * Charge la page d'études depuis l'API en appliquant les critères courants.
   */
  loadStudies(): void {
    this.isLoading.set(true);

    const criteria: StudySearchCriteria = {
      ...this.searchCriteria(),
      page: this.currentPage(),
      size: this.pageSize(),
      sort: this.currentSort() ?? undefined,
    };

    this.studyService.searchStudies(criteria).subscribe({
      next: (page) => {
        this.studies.set(page.content);
        this.totalRecords.set(page.totalElements);
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error('Erreur chargement études', err);
        this.isLoading.set(false);
      },
    });
  }

  /**
   * Déclenche une nouvelle recherche depuis le formulaire de filtres.
   * Remet la pagination à la première page.
   */
  protected onSearch(): void {
    const criteria: StudySearchCriteria = {};
    if (this.searchForm.titleKeyword) criteria.titleKeyword = this.searchForm.titleKeyword;
    if (this.searchForm.acronym)      criteria.acronym      = this.searchForm.acronym;
    if (this.searchForm.ethicsNumber) criteria.ethicsNumber = this.searchForm.ethicsNumber;
    if (this.searchForm.eudractNumber) criteria.eudractNumber = this.searchForm.eudractNumber;
    if (this.searchForm.studyType)    criteria.studyType    = this.searchForm.studyType;
    if (this.searchForm.status)       criteria.status       = this.searchForm.status;
    if (this.searchForm.phase)        criteria.phase        = this.searchForm.phase;
    if (this.searchForm.sponsorType)  criteria.sponsorType  = this.searchForm.sponsorType;
    if (this.searchForm.isSponsorCusl != null) criteria.isSponsorCusl = this.searchForm.isSponsorCusl;

    this.searchCriteria.set(criteria);
    this.currentPage.set(0);
    this.loadStudies();
  }

  /**
   * Réinitialise le formulaire de recherche et recharge la liste.
   */
  protected onReset(): void {
    this.searchForm = {};
    this.searchCriteria.set({});
    this.currentPage.set(0);
    this.currentSort.set(undefined);
    this.loadStudies();
  }

  /**
   * Gère les événements lazy load de la table PrimeNG (pagination + tri).
   *
   * @param event événement PrimeNG LazyLoadEvent
   */
  protected onLazyLoad(event: TableLazyLoadEvent): void {
    const first = event.first ?? 0;
    const rows = event.rows ?? this.pageSize();
    const page = Math.floor(first / rows);
    this.currentPage.set(page);
    this.pageSize.set(rows);

    if (event.sortField) {
      const direction = (event.sortOrder ?? 1) > 0 ? 'asc' : 'desc';
      const field = Array.isArray(event.sortField) ? event.sortField[0] : event.sortField;
      this.currentSort.set(`${field},${direction}`);
    }

    this.loadStudies();
  }
}

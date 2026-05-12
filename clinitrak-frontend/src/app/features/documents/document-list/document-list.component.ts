import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule, DatePipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TableModule } from 'primeng/table';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { DropdownModule } from 'primeng/dropdown';
import { FileUploadModule, FileUploadEvent } from 'primeng/fileupload';
import { TagModule } from 'primeng/tag';
import { TooltipModule } from 'primeng/tooltip';
import { CardModule } from 'primeng/card';
import { ProgressSpinnerModule } from 'primeng/progressspinner';
import { ConfirmDialogModule } from 'primeng/confirmdialog';
import { MessageService, ConfirmationService } from 'primeng/api';
import { catchError, throwError } from 'rxjs';
import { DocumentService } from '../../../core/services/document.service';
import { ExportService } from '../../../core/services/export.service';
import {
  DocumentModule,
  DocumentResponse,
  DocumentType,
  DocumentUploadMetadata,
} from '../../../core/models/document.model';

/** Option de dropdown generique. */
interface SelectOption {
  label: string;
  value: string;
}

/**
 * Composant de liste et gestion des documents (GED CliniTrak).
 *
 * <p>Fonctionnalites :
 * <ul>
 *   <li>Upload de fichiers via p-fileUpload (drag & drop)</li>
 *   <li>Liste paginee avec filtres par type et module</li>
 *   <li>Telechargement via URL pre-signee MinIO</li>
 *   <li>Suppression avec confirmation</li>
 *   <li>Export CSV</li>
 * </ul>
 */
@Component({
  selector: 'app-document-list',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    DatePipe,
    TableModule,
    ButtonModule,
    InputTextModule,
    DropdownModule,
    FileUploadModule,
    TagModule,
    TooltipModule,
    CardModule,
    ProgressSpinnerModule,
    ConfirmDialogModule,
  ],
  providers: [MessageService, ConfirmationService],
  template: `
    <div class="tw-space-y-6">

      <!-- En-tete de page -->
      <div class="tw-flex tw-items-center tw-justify-between">
        <div>
          <h1 class="tw-text-2xl tw-font-bold tw-text-gray-800">Documents</h1>
          <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
            Gestion electronique des documents de la recherche clinique
          </p>
        </div>
        <p-button
          label="Exporter CSV"
          icon="pi pi-download"
          [outlined]="true"
          severity="secondary"
          size="small"
          (click)="onExportCsv()"
          [disabled]="documents().length === 0"
        />
      </div>

      <!-- Zone d'upload -->
      <p-card styleClass="!tw-shadow-sm">
        <ng-template pTemplate="header">
          <div class="tw-px-5 tw-pt-4 tw-pb-0">
            <h2 class="tw-text-base tw-font-semibold tw-text-gray-700">
              <i class="pi pi-upload tw-mr-2 tw-text-blue-500"></i>
              Deposer un document
            </h2>
          </div>
        </ng-template>

        <div class="tw-grid tw-grid-cols-1 md:tw-grid-cols-3 tw-gap-4 tw-mb-4">
          <!-- Type de document -->
          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-600">Type de document</label>
            <p-dropdown
              [options]="documentTypeOptions"
              [(ngModel)]="uploadMetadata.documentType"
              optionLabel="label"
              optionValue="value"
              placeholder="Selectionner un type"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Module -->
          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-600">Module</label>
            <p-dropdown
              [options]="moduleOptions"
              [(ngModel)]="uploadMetadata.module"
              optionLabel="label"
              optionValue="value"
              placeholder="Selectionner un module"
              styleClass="tw-w-full"
            />
          </div>

          <!-- Description -->
          <div class="tw-flex tw-flex-col tw-gap-1.5">
            <label class="tw-text-sm tw-font-medium tw-text-gray-600">Description (optionnel)</label>
            <input
              pInputText
              type="text"
              [(ngModel)]="uploadMetadata.description"
              placeholder="Description du document"
              class="tw-w-full"
            />
          </div>
        </div>

        <!-- Composant d'upload fichier -->
        <p-fileUpload
          mode="advanced"
          [multiple]="false"
          accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.txt,.jpg,.jpeg,.png"
          [maxFileSize]="50000000"
          chooseLabel="Choisir un fichier"
          uploadLabel="Uploader"
          cancelLabel="Annuler"
          [auto]="false"
          (onSelect)="onFileSelect($event)"
          (onUpload)="onUploadSuccess($event)"
          (onError)="onUploadError()"
          styleClass="tw-w-full"
          [customUpload]="true"
          (uploadHandler)="onCustomUpload($event)"
        >
          <ng-template pTemplate="empty">
            <div class="tw-flex tw-flex-col tw-items-center tw-py-6 tw-text-gray-400">
              <i class="pi pi-cloud-upload tw-text-4xl tw-mb-2"></i>
              <p class="tw-text-sm">Glisser-deposer un fichier ici ou cliquer pour selectionner</p>
              <p class="tw-text-xs tw-mt-1">PDF, Word, Excel, images — max 50 Mo</p>
            </div>
          </ng-template>
        </p-fileUpload>
      </p-card>

      <!-- Filtres -->
      <div class="tw-flex tw-flex-wrap tw-gap-3 tw-items-center">
        <!-- Recherche -->
        <span class="p-input-icon-left">
          <i class="pi pi-search"></i>
          <input
            pInputText
            type="text"
            [(ngModel)]="searchQuery"
            placeholder="Rechercher un document..."
            class="tw-w-64"
            (input)="onSearch()"
          />
        </span>

        <!-- Filtre par type -->
        <p-dropdown
          [options]="[{ label: 'Tous les types', value: '' }, ...documentTypeOptions]"
          [(ngModel)]="filterType"
          optionLabel="label"
          optionValue="value"
          (onChange)="loadDocuments()"
          styleClass="tw-min-w-[180px]"
        />

        <!-- Filtre par module -->
        <p-dropdown
          [options]="[{ label: 'Tous les modules', value: '' }, ...moduleOptions]"
          [(ngModel)]="filterModule"
          optionLabel="label"
          optionValue="value"
          (onChange)="loadDocuments()"
          styleClass="tw-min-w-[160px]"
        />

        <!-- Compteur -->
        <span class="tw-text-sm tw-text-gray-500 tw-ml-auto">
          {{ totalRecords() }} document(s)
        </span>
      </div>

      <!-- Tableau des documents -->
      <p-table
        [value]="documents()"
        [loading]="isLoading()"
        [paginator]="true"
        [rows]="pageSize"
        [totalRecords]="totalRecords()"
        [lazy]="true"
        (onLazyLoad)="onLazyLoad($event)"
        [rowsPerPageOptions]="[10, 25, 50]"
        dataKey="id"
        styleClass="p-datatable-sm p-datatable-striped"
        [tableStyle]="{ 'min-width': '60rem' }"
        responsiveLayout="scroll"
      >
        <ng-template pTemplate="header">
          <tr>
            <th pSortableColumn="originalFileName" class="tw-font-semibold">
              Nom du fichier <p-sortIcon field="originalFileName" />
            </th>
            <th class="tw-font-semibold">Type</th>
            <th class="tw-font-semibold">Module</th>
            <th class="tw-font-semibold tw-text-center">Version</th>
            <th class="tw-font-semibold">Taille</th>
            <th pSortableColumn="uploadedAt" class="tw-font-semibold">
              Date <p-sortIcon field="uploadedAt" />
            </th>
            <th class="tw-font-semibold">Depose par</th>
            <th class="tw-font-semibold tw-text-center">Actions</th>
          </tr>
        </ng-template>

        <ng-template pTemplate="body" let-doc>
          <tr>
            <!-- Nom du fichier -->
            <td>
              <div class="tw-flex tw-items-center tw-gap-2">
                <i [class]="getFileIcon(doc.mimeType) + ' tw-text-gray-400'"></i>
                <span class="tw-text-sm tw-text-gray-800 tw-font-medium tw-truncate tw-max-w-[200px]"
                      [pTooltip]="doc.originalFileName">
                  {{ doc.originalFileName }}
                </span>
              </div>
              @if (doc.description) {
                <p class="tw-text-xs tw-text-gray-400 tw-mt-0.5">{{ doc.description }}</p>
              }
            </td>

            <!-- Type -->
            <td>
              <p-tag
                [value]="documentService.getDocumentTypeLabel(doc.documentType)"
                severity="info"
                styleClass="tw-text-xs"
              />
            </td>

            <!-- Module -->
            <td>
              <p-tag
                [value]="documentService.getModuleLabel(doc.module)"
                [severity]="getModuleSeverity(doc.module)"
                styleClass="tw-text-xs"
              />
            </td>

            <!-- Version -->
            <td class="tw-text-center">
              <span class="tw-text-sm tw-text-gray-600">v{{ doc.version }}</span>
            </td>

            <!-- Taille -->
            <td>
              <span class="tw-text-sm tw-text-gray-500">
                {{ documentService.formatFileSize(doc.fileSize) }}
              </span>
            </td>

            <!-- Date -->
            <td>
              <span class="tw-text-sm tw-text-gray-600">
                {{ doc.uploadedAt | date:'dd/MM/yy HH:mm' }}
              </span>
            </td>

            <!-- Depose par -->
            <td>
              <span class="tw-text-sm tw-text-gray-600">{{ doc.uploadedBy }}</span>
            </td>

            <!-- Actions -->
            <td>
              <div class="tw-flex tw-justify-center tw-gap-1">
                <p-button
                  icon="pi pi-download"
                  [text]="true"
                  [rounded]="true"
                  severity="info"
                  size="small"
                  pTooltip="Telecharger"
                  (click)="onDownload(doc)"
                />
                <p-button
                  icon="pi pi-history"
                  [text]="true"
                  [rounded]="true"
                  severity="secondary"
                  size="small"
                  pTooltip="Historique des versions"
                  (click)="onViewVersions(doc)"
                />
                <p-button
                  icon="pi pi-trash"
                  [text]="true"
                  [rounded]="true"
                  severity="danger"
                  size="small"
                  pTooltip="Supprimer"
                  (click)="onDelete(doc)"
                />
              </div>
            </td>
          </tr>
        </ng-template>

        <!-- Etat vide -->
        <ng-template pTemplate="emptymessage">
          <tr>
            <td colspan="8" class="tw-text-center tw-py-12 tw-text-gray-400">
              <i class="pi pi-folder-open tw-text-4xl tw-mb-3 tw-block"></i>
              <p class="tw-text-sm">Aucun document trouve</p>
              <p class="tw-text-xs tw-mt-1">Deposez votre premier document ci-dessus</p>
            </td>
          </tr>
        </ng-template>
      </p-table>
    </div>
  `,
})
export class DocumentListComponent implements OnInit {

  protected readonly documentService = inject(DocumentService);
  private readonly exportService = inject(ExportService);
  private readonly messageService = inject(MessageService);
  private readonly confirmationService = inject(ConfirmationService);

  /** Liste des documents affiches. */
  protected readonly documents = signal<DocumentResponse[]>([]);

  /** Nombre total de documents (pour la pagination). */
  protected readonly totalRecords = signal<number>(0);

  /** Indicateur de chargement. */
  protected readonly isLoading = signal<boolean>(false);

  /** Taille de page par defaut. */
  protected readonly pageSize = 25;

  /** Requete de recherche textuelle. */
  protected searchQuery = '';

  /** Filtre actif par type. */
  protected filterType = '';

  /** Filtre actif par module. */
  protected filterModule = '';

  /** Metadonnees pour le prochain upload. */
  protected uploadMetadata: DocumentUploadMetadata = {
    documentType: DocumentType.OTHER,
    module: DocumentModule.OTHER,
  };

  /** Fichier selectionne pour upload. */
  private selectedFile: File | null = null;

  /** Options de dropdown pour les types de documents. */
  protected readonly documentTypeOptions: SelectOption[] = [
    { label: 'Protocole',             value: DocumentType.PROTOCOL },
    { label: 'Consentement eclaire',  value: DocumentType.INFORMED_CONSENT },
    { label: 'Soumission CE',         value: DocumentType.ETHICS_SUBMISSION },
    { label: 'Decision CE',           value: DocumentType.ETHICS_DECISION },
    { label: 'CRF',                   value: DocumentType.CRF },
    { label: 'Brochure investigateur',value: DocumentType.INVESTIGATOR_BROCHURE },
    { label: 'Reglementaire',         value: DocumentType.REGULATORY },
    { label: 'Contrat',               value: DocumentType.CONTRACT },
    { label: 'Rapport',               value: DocumentType.REPORT },
    { label: 'Correspondance',        value: DocumentType.CORRESPONDENCE },
    { label: 'Autre',                 value: DocumentType.OTHER },
  ];

  /** Options de dropdown pour les modules. */
  protected readonly moduleOptions: SelectOption[] = [
    { label: 'Etudes',       value: DocumentModule.STUDY },
    { label: 'Ethique',      value: DocumentModule.ETHICS },
    { label: 'CTC',          value: DocumentModule.CTC },
    { label: 'Pharmacie',    value: DocumentModule.PHARMACY },
    { label: 'Facturation',  value: DocumentModule.BILLING },
    { label: 'Administration', value: DocumentModule.ADMIN },
    { label: 'Autre',        value: DocumentModule.OTHER },
  ];

  /** Charge les documents au montage du composant. */
  ngOnInit(): void {
    this.loadDocuments();
  }

  /** Charge (ou recharge) la liste des documents avec les filtres courants. */
  protected loadDocuments(page = 0): void {
    this.isLoading.set(true);
    this.documentService.getDocuments({
      page,
      size: this.pageSize,
      search: this.searchQuery || undefined,
      documentType: (this.filterType as DocumentType) || undefined,
      module: (this.filterModule as DocumentModule) || undefined,
    }).pipe(
      catchError(err => {
        this.isLoading.set(false);
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Impossible de charger les documents',
        });
        return throwError(() => err);
      })
    ).subscribe(pageResult => {
      this.documents.set(pageResult.content);
      this.totalRecords.set(pageResult.totalElements);
      this.isLoading.set(false);
    });
  }

  /** Gere l'evenement de chargement lazy de p-table. */
  protected onLazyLoad(event: { first: number; rows: number }): void {
    const page = Math.floor(event.first / event.rows);
    this.loadDocuments(page);
  }

  /** Declenche la recherche textuelle (debounce manuel via ngModel). */
  protected onSearch(): void {
    this.loadDocuments(0);
  }

  /** Stocke le fichier selectionne pour l'upload personnalise. */
  protected onFileSelect(event: { currentFiles: File[] }): void {
    if (event.currentFiles.length > 0) {
      this.selectedFile = event.currentFiles[0];
    }
  }

  /** Gere l'upload personnalise (customUpload). */
  protected onCustomUpload(event: { files: File[] }): void {
    const file = event.files[0];
    if (!file) return;

    if (!this.uploadMetadata.documentType) {
      this.messageService.add({
        severity: 'warn',
        summary: 'Champ requis',
        detail: 'Veuillez selectionner un type de document avant d\'uploader',
      });
      return;
    }

    this.documentService.uploadDocument(file, this.uploadMetadata).pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur d\'upload',
          detail: err.error?.detail ?? 'L\'upload du fichier a echoue',
        });
        return throwError(() => err);
      })
    ).subscribe(doc => {
      this.messageService.add({
        severity: 'success',
        summary: 'Document uploade',
        detail: `"${doc.originalFileName}" a ete depose avec succes`,
      });
      this.loadDocuments(0);
      this.resetUploadForm();
    });
  }

  /** Callback apres upload reussi (upload automatique). */
  protected onUploadSuccess(event: FileUploadEvent): void {
    this.messageService.add({
      severity: 'success',
      summary: 'Document uploade',
      detail: 'Le document a ete depose avec succes',
    });
    this.loadDocuments(0);
  }

  /** Callback en cas d'erreur d'upload. */
  protected onUploadError(): void {
    this.messageService.add({
      severity: 'error',
      summary: 'Erreur d\'upload',
      detail: 'L\'upload du fichier a echoue. Verifiez la taille et le format du fichier.',
    });
  }

  /** Declenche le telechargement d'un document. */
  protected onDownload(doc: DocumentResponse): void {
    this.documentService.downloadDocument(doc.id).pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Impossible de generer le lien de telechargement',
        });
        return throwError(() => err);
      })
    ).subscribe(response => {
      window.open(response.downloadUrl, '_blank', 'noopener,noreferrer');
    });
  }

  /** Affiche l'historique des versions d'un document. */
  protected onViewVersions(doc: DocumentResponse): void {
    // TODO : ouvrir un dialog avec l'historique des versions
    this.messageService.add({
      severity: 'info',
      summary: 'Versions',
      detail: `Historique des versions de "${doc.originalFileName}" (a implementer)`,
    });
  }

  /** Demande de confirmation avant suppression. */
  protected onDelete(doc: DocumentResponse): void {
    this.confirmationService.confirm({
      message: `Supprimer definitivement "${doc.originalFileName}" ?`,
      header: 'Confirmation de suppression',
      icon: 'pi pi-exclamation-triangle',
      acceptLabel: 'Supprimer',
      rejectLabel: 'Annuler',
      acceptButtonStyleClass: 'p-button-danger',
      accept: () => this.deleteDocument(doc),
    });
  }

  /** Exporte la liste courante en CSV. */
  protected onExportCsv(): void {
    const data = this.documents().map(doc => ({
      Fichier: doc.originalFileName,
      Type: this.documentService.getDocumentTypeLabel(doc.documentType),
      Module: this.documentService.getModuleLabel(doc.module),
      Version: `v${doc.version}`,
      Taille: this.documentService.formatFileSize(doc.fileSize),
      'Depose par': doc.uploadedBy,
      'Date upload': doc.uploadedAt,
      Description: doc.description ?? '',
    }));
    this.exportService.exportToCsv(data, 'clinitrak-documents');
  }

  /**
   * Retourne l'icone PrimeIcons correspondant au type MIME d'un fichier.
   *
   * @param mimeType Type MIME du fichier
   * @returns Classe CSS PrimeIcons
   */
  protected getFileIcon(mimeType: string): string {
    if (mimeType.includes('pdf'))       return 'pi pi-file-pdf';
    if (mimeType.includes('word'))      return 'pi pi-file-word';
    if (mimeType.includes('excel') || mimeType.includes('spreadsheet')) return 'pi pi-file-excel';
    if (mimeType.includes('image'))     return 'pi pi-image';
    if (mimeType.includes('text'))      return 'pi pi-file';
    return 'pi pi-file';
  }

  /**
   * Retourne la severite PrimeNG correspondant a un module.
   *
   * @param module Module du document
   * @returns Severite PrimeNG (pour p-tag)
   */
  protected getModuleSeverity(module: DocumentModule): 'success' | 'info' | 'warning' | 'danger' | 'secondary' {
    const severities: Record<DocumentModule, 'success' | 'info' | 'warning' | 'danger' | 'secondary'> = {
      [DocumentModule.STUDY]:    'info',
      [DocumentModule.ETHICS]:   'warning',
      [DocumentModule.CTC]:      'success',
      [DocumentModule.PHARMACY]: 'danger',
      [DocumentModule.BILLING]:  'secondary',
      [DocumentModule.ADMIN]:    'secondary',
      [DocumentModule.OTHER]:    'secondary',
    };
    return severities[module] ?? 'secondary';
  }

  /** Supprime un document apres confirmation. */
  private deleteDocument(doc: DocumentResponse): void {
    this.documentService.deleteDocument(doc.id).pipe(
      catchError(err => {
        this.messageService.add({
          severity: 'error',
          summary: 'Erreur',
          detail: err.error?.detail ?? 'Impossible de supprimer le document',
        });
        return throwError(() => err);
      })
    ).subscribe(() => {
      this.messageService.add({
        severity: 'success',
        summary: 'Document supprime',
        detail: `"${doc.originalFileName}" a ete supprime`,
      });
      this.loadDocuments(0);
    });
  }

  /** Remet a zero le formulaire d'upload. */
  private resetUploadForm(): void {
    this.uploadMetadata = {
      documentType: DocumentType.OTHER,
      module: DocumentModule.OTHER,
    };
    this.selectedFile = null;
  }
}

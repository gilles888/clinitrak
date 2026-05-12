import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import {
  DocumentDownloadResponse,
  DocumentFilter,
  DocumentModule,
  DocumentResponse,
  DocumentType,
  DocumentUploadMetadata,
} from '../models/document.model';
import { Page } from '../models/notification.model';

/**
 * Service Angular pour la gestion des documents (GED MinIO).
 *
 * <p>Toutes les operations passent par le document-service (port 8088)
 * via la gateway (port 8080).
 *
 * <p>Upload : multipart/form-data avec le fichier + les metadonnees JSON.
 * Download : retourne une URL pre-signee MinIO a consommer cote navigateur.
 */
@Injectable({ providedIn: 'root' })
export class DocumentService {

  private readonly http = inject(HttpClient);
  private readonly API = `${environment.apiBaseUrl}/v1/documents`;

  /**
   * Uploade un document avec ses metadonnees.
   *
   * <p>Envoie un {@code multipart/form-data} avec deux parts :
   * {@code file} (binary) et {@code metadata} (application/json).
   *
   * @param file Fichier a uploader
   * @param metadata Metadonnees du document
   * @returns Observable du document cree
   */
  uploadDocument(file: File, metadata: DocumentUploadMetadata): Observable<DocumentResponse> {
    const formData = new FormData();
    formData.append('file', file, file.name);
    formData.append(
      'metadata',
      new Blob([JSON.stringify(metadata)], { type: 'application/json' })
    );
    return this.http.post<DocumentResponse>(`${this.API}/upload`, formData);
  }

  /**
   * Recupere la liste paginee des documents avec filtres optionnels.
   *
   * @param filter Criteres de filtrage (module, type, referenceId, recherche)
   * @returns Observable d'une page de documents
   */
  getDocuments(filter: DocumentFilter = {}): Observable<Page<DocumentResponse>> {
    let params = new HttpParams();

    if (filter.module)       params = params.set('module', filter.module);
    if (filter.documentType) params = params.set('documentType', filter.documentType);
    if (filter.referenceId)  params = params.set('referenceId', filter.referenceId);
    if (filter.uploadedBy)   params = params.set('uploadedBy', filter.uploadedBy);
    if (filter.search)       params = params.set('search', filter.search);
    if (filter.page !== undefined) params = params.set('page', filter.page.toString());
    if (filter.size !== undefined) params = params.set('size', filter.size.toString());

    return this.http.get<Page<DocumentResponse>>(this.API, { params });
  }

  /**
   * Recupere les metadonnees d'un document par son identifiant.
   *
   * @param id UUID du document
   * @returns Observable du document
   */
  getById(id: string): Observable<DocumentResponse> {
    return this.http.get<DocumentResponse>(`${this.API}/${id}`);
  }

  /**
   * Genere une URL pre-signee pour le telechargement d'un document.
   *
   * <p>L'URL est valable pour une duree limitee (configuree dans document-service).
   * Redirige directement vers MinIO.
   *
   * @param id UUID du document
   * @returns Observable contenant l'URL de telechargement
   */
  downloadDocument(id: string): Observable<DocumentDownloadResponse> {
    return this.http.get<DocumentDownloadResponse>(`${this.API}/${id}/download-url`);
  }

  /**
   * Declenche le telechargement d'un document en ouvrant l'URL dans un nouvel onglet.
   *
   * @param id UUID du document
   */
  downloadAndOpen(id: string): void {
    this.downloadDocument(id).subscribe(response => {
      window.open(response.downloadUrl, '_blank', 'noopener,noreferrer');
    });
  }

  /**
   * Supprime un document (soft-delete).
   *
   * @param id UUID du document
   * @returns Observable vide (204)
   */
  deleteDocument(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  /**
   * Recupere l'historique des versions d'un document.
   *
   * @param id UUID du document parent ou d'une version
   * @returns Observable de la liste ordonnee des versions (plus recente en premier)
   */
  getVersionHistory(id: string): Observable<DocumentResponse[]> {
    return this.http.get<DocumentResponse[]>(`${this.API}/${id}/versions`);
  }

  /**
   * Retourne le libelle affichable d'un type de document.
   *
   * @param type Type de document
   * @returns Libelle en francais
   */
  getDocumentTypeLabel(type: DocumentType): string {
    const labels: Record<DocumentType, string> = {
      [DocumentType.PROTOCOL]:              'Protocole',
      [DocumentType.INFORMED_CONSENT]:      'Consentement eclaire',
      [DocumentType.ETHICS_SUBMISSION]:     'Soumission CE',
      [DocumentType.ETHICS_DECISION]:       'Decision CE',
      [DocumentType.CRF]:                   'CRF',
      [DocumentType.INVESTIGATOR_BROCHURE]: 'Brochure investigateur',
      [DocumentType.REGULATORY]:            'Reglementaire',
      [DocumentType.CONTRACT]:              'Contrat',
      [DocumentType.REPORT]:                'Rapport',
      [DocumentType.CORRESPONDENCE]:        'Correspondance',
      [DocumentType.OTHER]:                 'Autre',
    };
    return labels[type] ?? type;
  }

  /**
   * Retourne le libelle affichable d'un module.
   *
   * @param module Module associe au document
   * @returns Libelle en francais
   */
  getModuleLabel(module: DocumentModule): string {
    const labels: Record<DocumentModule, string> = {
      [DocumentModule.STUDY]:    'Etudes',
      [DocumentModule.ETHICS]:   'Ethique',
      [DocumentModule.CTC]:      'CTC',
      [DocumentModule.PHARMACY]: 'Pharmacie',
      [DocumentModule.BILLING]:  'Facturation',
      [DocumentModule.ADMIN]:    'Administration',
      [DocumentModule.OTHER]:    'Autre',
    };
    return labels[module] ?? module;
  }

  /**
   * Formate la taille d'un fichier en unite lisible.
   *
   * @param bytes Taille en octets
   * @returns Chaine formatee (ex: "2.4 Mo")
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 o';
    const units = ['o', 'Ko', 'Mo', 'Go'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return `${(bytes / Math.pow(1024, i)).toFixed(1)} ${units[i]}`;
  }
}

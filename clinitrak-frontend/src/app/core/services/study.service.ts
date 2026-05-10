import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  ContactRequest,
  ContactResponse,
  PageResponse,
  PatientRequest,
  PatientResponse,
  StatusUpdateRequest,
  StudyCreateRequest,
  StudyResponse,
  StudySearchCriteria,
  StudyStatisticsResponse,
  StudyStatusHistory,
  StudySummaryResponse,
  StudyUpdateRequest,
  SubmissionRequest,
  SubmissionResponse,
} from '../models/study.model';

/**
 * Service Angular pour la gestion des études cliniques.
 *
 * <p>Encapsule tous les appels HTTP vers le study-service via la gateway API.
 * Toutes les méthodes retournent des {@link Observable} (RxJS).
 * L'URL de base est construite depuis {@link environment.apiBaseUrl}.
 *
 * @example
 * ```ts
 * private readonly studyService = inject(StudyService);
 *
 * this.studyService.searchStudies({ status: StudyStatus.ONGOING, size: 20 })
 *   .subscribe(page => this.studies.set(page.content));
 * ```
 */
@Injectable({ providedIn: 'root' })
export class StudyService {

  private readonly http = inject(HttpClient);

  /** URL de base du study-service via la gateway. */
  private readonly API = `${environment.apiBaseUrl}/v1/studies`;

  // ─────────────────────────────────────────────────────────────
  // Études — CRUD principal
  // ─────────────────────────────────────────────────────────────

  /**
   * Recherche des études avec pagination, tri et filtres.
   *
   * @param criteria critères de recherche et de pagination
   * @returns page de résumés d'études
   */
  searchStudies(criteria: StudySearchCriteria): Observable<PageResponse<StudySummaryResponse>> {
    let params = new HttpParams();
    const entries = Object.entries(criteria) as [string, unknown][];
    for (const [key, value] of entries) {
      if (value !== null && value !== undefined && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<PageResponse<StudySummaryResponse>>(this.API, { params });
  }

  /**
   * Récupère le détail complet d'une étude.
   *
   * @param id identifiant UUID de l'étude
   * @returns détail de l'étude
   */
  getStudy(id: string): Observable<StudyResponse> {
    return this.http.get<StudyResponse>(`${this.API}/${id}`);
  }

  /**
   * Crée une nouvelle étude clinique.
   *
   * @param request données de création
   * @returns étude créée (201)
   */
  createStudy(request: StudyCreateRequest): Observable<StudyResponse> {
    return this.http.post<StudyResponse>(this.API, request);
  }

  /**
   * Met à jour une étude existante.
   *
   * @param id  identifiant UUID de l'étude
   * @param request données de mise à jour
   * @returns étude mise à jour
   */
  updateStudy(id: string, request: StudyUpdateRequest): Observable<StudyResponse> {
    return this.http.put<StudyResponse>(`${this.API}/${id}`, request);
  }

  /**
   * Change le statut d'une étude (PATCH).
   *
   * @param id identifiant UUID de l'étude
   * @param request nouveau statut + date + commentaire optionnel
   * @returns étude avec le nouveau statut
   */
  updateStatus(id: string, request: StatusUpdateRequest): Observable<StudyResponse> {
    return this.http.patch<StudyResponse>(`${this.API}/${id}/status`, request);
  }

  /**
   * Supprime définitivement une étude.
   *
   * @param id identifiant UUID de l'étude
   * @returns void (204)
   */
  deleteStudy(id: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }

  // ─────────────────────────────────────────────────────────────
  // Historique de statuts
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère l'historique complet des changements de statut d'une étude.
   *
   * @param id identifiant UUID de l'étude
   * @returns liste des entrées d'historique
   */
  getStatusHistory(id: string): Observable<StudyStatusHistory[]> {
    return this.http.get<StudyStatusHistory[]>(`${this.API}/${id}/statuses`);
  }

  /**
   * Ajoute une entrée dans l'historique de statut.
   *
   * @param id identifiant UUID de l'étude
   * @param request nouveau statut à enregistrer
   * @returns entrée d'historique créée (201)
   */
  addStatus(id: string, request: StatusUpdateRequest): Observable<StudyStatusHistory> {
    return this.http.post<StudyStatusHistory>(`${this.API}/${id}/statuses`, request);
  }

  // ─────────────────────────────────────────────────────────────
  // Contacts
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste des contacts d'une étude.
   *
   * @param id identifiant UUID de l'étude
   * @returns liste des contacts
   */
  getContacts(id: string): Observable<ContactResponse[]> {
    return this.http.get<ContactResponse[]>(`${this.API}/${id}/contacts`);
  }

  /**
   * Ajoute un contact à une étude.
   *
   * @param id identifiant UUID de l'étude
   * @param request données du contact
   * @returns contact créé (201)
   */
  addContact(id: string, request: ContactRequest): Observable<ContactResponse> {
    return this.http.post<ContactResponse>(`${this.API}/${id}/contacts`, request);
  }

  /**
   * Supprime un contact d'une étude.
   *
   * @param id identifiant UUID de l'étude
   * @param contactId identifiant UUID du contact
   * @returns void (204)
   */
  removeContact(id: string, contactId: string): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}/contacts/${contactId}`);
  }

  // ─────────────────────────────────────────────────────────────
  // Soumissions réglementaires
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les soumissions réglementaires d'une étude (paginé).
   *
   * @param id identifiant UUID de l'étude
   * @param page numéro de page (0-based)
   * @param size nombre d'éléments par page
   * @returns page de soumissions
   */
  getSubmissions(id: string, page = 0, size = 20): Observable<PageResponse<SubmissionResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<SubmissionResponse>>(`${this.API}/${id}/submissions`, { params });
  }

  /**
   * Ajoute une soumission réglementaire à une étude.
   *
   * @param id identifiant UUID de l'étude
   * @param request données de la soumission
   * @returns soumission créée (201)
   */
  addSubmission(id: string, request: SubmissionRequest): Observable<SubmissionResponse> {
    return this.http.post<SubmissionResponse>(`${this.API}/${id}/submissions`, request);
  }

  // ─────────────────────────────────────────────────────────────
  // Patients
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste paginée des patients d'une étude (pseudonymisés).
   *
   * @param id identifiant UUID de l'étude
   * @param page numéro de page (0-based)
   * @param size nombre d'éléments par page
   * @returns page de patients
   */
  getPatients(id: string, page = 0, size = 20): Observable<PageResponse<PatientResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<PatientResponse>>(`${this.API}/${id}/patients`, { params });
  }

  /**
   * Inscrit un patient dans une étude.
   *
   * @param id identifiant UUID de l'étude
   * @param request données du patient (pseudonymisées)
   * @returns patient créé (201)
   */
  addPatient(id: string, request: PatientRequest): Observable<PatientResponse> {
    return this.http.post<PatientResponse>(`${this.API}/${id}/patients`, request);
  }

  // ─────────────────────────────────────────────────────────────
  // Statistiques
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les statistiques globales sur les études cliniques.
   *
   * @returns objet contenant les compteurs par statut, phase et aire thérapeutique
   */
  getStatistics(): Observable<StudyStatisticsResponse> {
    return this.http.get<StudyStatisticsResponse>(`${this.API}/statistics`);
  }
}

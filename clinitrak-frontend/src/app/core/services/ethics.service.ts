import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  AgendaItemResponse,
  AnnualReportResponse,
  CorrespondenceGenerateRequest,
  CorrespondenceResponse,
  DecisionUpdateRequest,
  EthicsDashboardResponse,
  EthicsReviewCreateRequest,
  EthicsReviewResponse,
  MeetingCreateRequest,
  MeetingResponse,
  TemplateResponse,
} from '../models/ethics.model';
import { PageResponse } from '../models/study.model';

/**
 * Service Angular pour la gestion du Comité d'Éthique.
 *
 * <p>Encapsule tous les appels HTTP vers l'ethics-service via la gateway API (port 8083).
 * Toutes les méthodes retournent des {@link Observable} (RxJS).
 * L'URL de base est construite depuis {@link environment.apiBaseUrl}.
 *
 * @example
 * ```ts
 * private readonly ethicsService = inject(EthicsService);
 *
 * this.ethicsService.getDashboard()
 *   .subscribe(dashboard => this.dashboard.set(dashboard));
 * ```
 */
@Injectable({ providedIn: 'root' })
export class EthicsService {

  private readonly http = inject(HttpClient);

  /** URL de base de l'ethics-service via la gateway. */
  private readonly API = `${environment.apiBaseUrl}/v1/ethics`;

  // ─────────────────────────────────────────────────────────────
  // Tableau de bord
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les données agrégées du tableau de bord CE.
   *
   * @returns données du dashboard CE
   */
  getDashboard(): Observable<EthicsDashboardResponse> {
    return this.http.get<EthicsDashboardResponse>(`${this.API}/dashboard`);
  }

  // ─────────────────────────────────────────────────────────────
  // Avis CE — Reviews
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste paginée de tous les avis CE.
   *
   * @param page numéro de page (0-based), défaut 0
   * @param size nombre d'éléments par page, défaut 20
   * @returns page d'avis CE
   */
  getReviews(page = 0, size = 20): Observable<PageResponse<EthicsReviewResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<EthicsReviewResponse>>(`${this.API}/reviews`, { params });
  }

  /**
   * Récupère le détail d'un avis CE par son identifiant.
   *
   * @param id identifiant UUID de l'avis CE
   * @returns détail de l'avis CE
   */
  getReview(id: string): Observable<EthicsReviewResponse> {
    return this.http.get<EthicsReviewResponse>(`${this.API}/reviews/${id}`);
  }

  /**
   * Récupère tous les avis CE associés à une étude.
   *
   * @param studyId identifiant UUID de l'étude
   * @returns liste des avis CE de l'étude
   */
  getReviewsByStudy(studyId: string): Observable<EthicsReviewResponse[]> {
    return this.http.get<EthicsReviewResponse[]>(`${this.API}/reviews/by-study/${studyId}`);
  }

  /**
   * Crée un nouvel avis CE.
   *
   * @param request données de création
   * @returns avis CE créé (201)
   */
  createReview(request: EthicsReviewCreateRequest): Observable<EthicsReviewResponse> {
    return this.http.post<EthicsReviewResponse>(`${this.API}/reviews`, request);
  }

  /**
   * Met à jour la décision sur un avis CE existant (PATCH).
   *
   * @param id      identifiant UUID de l'avis CE
   * @param request nouvelles données de décision
   * @returns avis CE mis à jour
   */
  updateDecision(id: string, request: DecisionUpdateRequest): Observable<EthicsReviewResponse> {
    return this.http.patch<EthicsReviewResponse>(`${this.API}/reviews/${id}/decision`, request);
  }

  // ─────────────────────────────────────────────────────────────
  // Réunions — Meetings
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste des réunions CE dans une plage de dates.
   *
   * @param from date de début (ISO 8601, optionnel)
   * @param to   date de fin (ISO 8601, optionnel)
   * @returns liste des réunions
   */
  getMeetings(from?: string, to?: string): Observable<MeetingResponse[]> {
    let params = new HttpParams();
    if (from) params = params.set('from', from);
    if (to)   params = params.set('to', to);
    return this.http.get<MeetingResponse[]>(`${this.API}/meetings`, { params });
  }

  /**
   * Crée une nouvelle réunion CE.
   *
   * @param request données de la réunion
   * @returns réunion créée (201)
   */
  createMeeting(request: MeetingCreateRequest): Observable<MeetingResponse> {
    return this.http.post<MeetingResponse>(`${this.API}/meetings`, request);
  }

  /**
   * Met à jour une réunion CE existante (PUT).
   *
   * @param id      identifiant UUID de la réunion
   * @param request données de mise à jour
   * @returns réunion mise à jour
   */
  updateMeeting(id: string, request: MeetingCreateRequest): Observable<MeetingResponse> {
    return this.http.put<MeetingResponse>(`${this.API}/meetings/${id}`, request);
  }

  /**
   * Récupère les items de l'ordre du jour d'une réunion.
   *
   * @param meetingId identifiant UUID de la réunion
   * @returns liste des items de l'ordre du jour
   */
  getAgendaItems(meetingId: string): Observable<AgendaItemResponse[]> {
    return this.http.get<AgendaItemResponse[]>(`${this.API}/meetings/${meetingId}/agenda`);
  }

  /**
   * Ajoute un item à l'ordre du jour d'une réunion.
   *
   * @param meetingId identifiant UUID de la réunion
   * @param item      données de l'item
   * @returns item créé (201)
   */
  addAgendaItem(
    meetingId: string,
    item: { studyId?: string; itemType?: string; durationMinutes: number; comments?: string },
  ): Observable<AgendaItemResponse> {
    return this.http.post<AgendaItemResponse>(`${this.API}/meetings/${meetingId}/agenda`, item);
  }

  /**
   * Met à jour le statut d'une réunion CE (PATCH).
   *
   * @param id     identifiant UUID de la réunion
   * @param status nouveau statut
   * @returns void
   */
  updateMeetingStatus(id: string, status: string): Observable<void> {
    const params = new HttpParams().set('status', status);
    return this.http.patch<void>(`${this.API}/meetings/${id}/status`, null, { params });
  }

  // ─────────────────────────────────────────────────────────────
  // Rapports annuels — Annual Reports
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste paginée des rapports annuels.
   *
   * @param page numéro de page (0-based), défaut 0
   * @param size nombre d'éléments par page, défaut 20
   * @returns page de rapports annuels
   */
  getAnnualReports(page = 0, size = 20): Observable<PageResponse<AnnualReportResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<AnnualReportResponse>>(`${this.API}/annual-reports`, { params });
  }

  /**
   * Récupère les rapports annuels dont l'échéance approche.
   *
   * @param daysAhead horizon en jours (défaut 60)
   * @returns liste des rapports annuels à échéance proche
   */
  getDueReports(daysAhead = 60): Observable<AnnualReportResponse[]> {
    const params = new HttpParams().set('daysAhead', daysAhead);
    return this.http.get<AnnualReportResponse[]>(`${this.API}/annual-reports/due`, { params });
  }

  /**
   * Marque un rapport annuel comme reçu (PATCH).
   *
   * @param id           identifiant UUID du rapport
   * @param receivedDate date de réception (ISO 8601)
   * @returns rapport annuel mis à jour
   */
  markReportReceived(id: string, receivedDate: string): Observable<AnnualReportResponse> {
    const params = new HttpParams().set('receivedDate', receivedDate);
    return this.http.patch<AnnualReportResponse>(`${this.API}/annual-reports/${id}/received`, null, { params });
  }

  // ─────────────────────────────────────────────────────────────
  // Correspondances — Correspondence
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste des modèles de correspondance disponibles.
   *
   * @returns liste des modèles actifs
   */
  getTemplates(): Observable<TemplateResponse[]> {
    return this.http.get<TemplateResponse[]>(`${this.API}/correspondence/templates`);
  }

  /**
   * Génère une correspondance à partir d'un modèle.
   *
   * @param request paramètres de génération
   * @returns correspondance générée (201)
   */
  generateCorrespondence(request: CorrespondenceGenerateRequest): Observable<CorrespondenceResponse> {
    return this.http.post<CorrespondenceResponse>(`${this.API}/correspondence/generate`, request);
  }

  /**
   * Envoie une correspondance déjà générée par email.
   *
   * @param id identifiant UUID de la correspondance
   * @returns correspondance mise à jour avec sentDate
   */
  sendCorrespondence(id: string): Observable<CorrespondenceResponse> {
    return this.http.post<CorrespondenceResponse>(`${this.API}/correspondence/${id}/send`, null);
  }

  /**
   * Récupère les correspondances paginées d'une étude.
   *
   * @param studyId identifiant UUID de l'étude
   * @param page    numéro de page (0-based), défaut 0
   * @param size    nombre d'éléments par page, défaut 20
   * @returns page de correspondances
   */
  getCorrespondenceByStudy(studyId: string, page = 0, size = 20): Observable<PageResponse<CorrespondenceResponse>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PageResponse<CorrespondenceResponse>>(`${this.API}/correspondence/by-study/${studyId}`, { params });
  }

  /**
   * Télécharge le PDF d'une correspondance.
   *
   * @param id identifiant UUID de la correspondance
   * @returns blob PDF (application/pdf)
   */
  downloadPdf(id: string): Observable<Blob> {
    return this.http.get(`${this.API}/correspondence/${id}/pdf`, { responseType: 'blob' });
  }
}

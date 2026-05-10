import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  CTCDashboard,
  FinancialContract,
  MonitoringVisit,
  Priority,
  QualityEvent,
  RequestStatus,
  SponsorStudy,
  StatisticsRequest,
  StudyTimeline,
  TrialDeskRequest,
} from '../models/ctc.model';

/**
 * Service Angular pour la gestion du Clinical Trial Center (CTC).
 *
 * <p>Encapsule tous les appels HTTP vers le ctc-service via la gateway API (port 8084).
 * Toutes les méthodes retournent des {@link Observable} (RxJS).
 * L'URL de base est construite depuis {@link environment.apiBaseUrl}.
 *
 * @example
 * ```ts
 * private readonly ctcService = inject(CtcService);
 *
 * this.ctcService.getDashboard()
 *   .subscribe(dashboard => this.dashboard.set(dashboard));
 * ```
 */
@Injectable({ providedIn: 'root' })
export class CtcService {

  private readonly http = inject(HttpClient);

  /** URL de base du ctc-service via la gateway. */
  private readonly API = `${environment.apiBaseUrl}/v1/ctc`;

  // ─────────────────────────────────────────────────────────────
  // Tableau de bord
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les données agrégées du tableau de bord CTC.
   *
   * @returns données du dashboard CTC
   */
  getDashboard(): Observable<CTCDashboard> {
    return this.http.get<CTCDashboard>(`${this.API}/dashboard`);
  }

  // ─────────────────────────────────────────────────────────────
  // Demandes guichet — Desk Requests
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de toutes les demandes guichet CTC.
   *
   * @returns liste des demandes guichet
   */
  getDeskRequests(): Observable<TrialDeskRequest[]> {
    return this.http.get<TrialDeskRequest[]>(`${this.API}/desk-requests`);
  }

  /**
   * Crée une nouvelle demande guichet CTC.
   *
   * @param req données de la demande (partiel)
   * @returns demande créée (201)
   */
  createDeskRequest(req: Partial<TrialDeskRequest>): Observable<TrialDeskRequest> {
    return this.http.post<TrialDeskRequest>(`${this.API}/desk-requests`, req);
  }

  /**
   * Assigne une demande guichet à un agent CTC avec une priorité et une deadline.
   *
   * @param id         identifiant UUID de la demande
   * @param assignData données d'assignation (agent, priorité, deadline optionnelle)
   * @returns demande mise à jour
   */
  assignDeskRequest(
    id: string,
    assignData: { assignedTo: string; priority: Priority; deadline?: string },
  ): Observable<TrialDeskRequest> {
    return this.http.patch<TrialDeskRequest>(`${this.API}/desk-requests/${id}/assign`, assignData);
  }

  /**
   * Met à jour le statut d'une demande guichet (PATCH).
   *
   * @param id         identifiant UUID de la demande
   * @param statusData nouveau statut et notes optionnelles
   * @returns demande mise à jour
   */
  updateDeskRequestStatus(
    id: string,
    statusData: { status: RequestStatus; notes?: string },
  ): Observable<TrialDeskRequest> {
    return this.http.patch<TrialDeskRequest>(`${this.API}/desk-requests/${id}/status`, statusData);
  }

  // ─────────────────────────────────────────────────────────────
  // Visites de monitoring — Monitoring Visits
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de toutes les visites de monitoring.
   *
   * @returns liste des visites de monitoring
   */
  getMonitoringVisits(): Observable<MonitoringVisit[]> {
    return this.http.get<MonitoringVisit[]>(`${this.API}/monitoring-visits`);
  }

  /**
   * Crée une nouvelle visite de monitoring.
   *
   * @param req données de la visite (partiel)
   * @returns visite créée (201)
   */
  createMonitoringVisit(req: Partial<MonitoringVisit>): Observable<MonitoringVisit> {
    return this.http.post<MonitoringVisit>(`${this.API}/monitoring-visits`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Contrats financiers — Financial Contracts
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de tous les contrats financiers.
   *
   * @returns liste des contrats financiers
   */
  getFinancialContracts(): Observable<FinancialContract[]> {
    return this.http.get<FinancialContract[]>(`${this.API}/financial-contracts`);
  }

  /**
   * Crée un nouveau contrat financier.
   *
   * @param req données du contrat (partiel)
   * @returns contrat créé (201)
   */
  createFinancialContract(req: Partial<FinancialContract>): Observable<FinancialContract> {
    return this.http.post<FinancialContract>(`${this.API}/financial-contracts`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Demandes statistiques — Statistics Requests
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de toutes les demandes statistiques.
   *
   * @returns liste des demandes statistiques
   */
  getStatisticsRequests(): Observable<StatisticsRequest[]> {
    return this.http.get<StatisticsRequest[]>(`${this.API}/statistics-requests`);
  }

  /**
   * Crée une nouvelle demande d'analyse statistique.
   *
   * @param req données de la demande (partiel)
   * @returns demande créée (201)
   */
  createStatisticsRequest(req: Partial<StatisticsRequest>): Observable<StatisticsRequest> {
    return this.http.post<StatisticsRequest>(`${this.API}/statistics-requests`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Études promoteur — Sponsor Studies
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de toutes les études dont le CUSL est promoteur.
   *
   * @returns liste des études promoteur
   */
  getSponsorStudies(): Observable<SponsorStudy[]> {
    return this.http.get<SponsorStudy[]>(`${this.API}/sponsor-studies`);
  }

  /**
   * Crée une nouvelle étude promoteur.
   *
   * @param req données de l'étude (partiel)
   * @returns étude créée (201)
   */
  createSponsorStudy(req: Partial<SponsorStudy>): Observable<SponsorStudy> {
    return this.http.post<SponsorStudy>(`${this.API}/sponsor-studies`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Événements qualité — Quality Events
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de tous les événements qualité.
   *
   * @returns liste des événements qualité
   */
  getQualityEvents(): Observable<QualityEvent[]> {
    return this.http.get<QualityEvent[]>(`${this.API}/quality-events`);
  }

  /**
   * Crée un nouvel événement qualité.
   *
   * @param req données de l'événement (partiel)
   * @returns événement créé (201)
   */
  createQualityEvent(req: Partial<QualityEvent>): Observable<QualityEvent> {
    return this.http.post<QualityEvent>(`${this.API}/quality-events`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Timeline
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la timeline complète d'une étude.
   *
   * @param studyId identifiant UUID de l'étude
   * @returns timeline de l'étude avec tous ses événements
   */
  getStudyTimeline(studyId: string): Observable<StudyTimeline> {
    return this.http.get<StudyTimeline>(`${this.API}/studies/${studyId}/timeline`);
  }
}

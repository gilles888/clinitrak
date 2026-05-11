import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Dispensation,
  DrugStock,
  EmergencyUnblinding,
  InvestigationalDrug,
  PharmacyAlertsResponse,
  PharmacyDashboard,
  StockImportResult,
  StockStatus,
} from '../models/pharmacy.model';

/**
 * Service Angular pour la gestion de la pharmacie clinique.
 *
 * <p>Encapsule tous les appels HTTP vers le pharmacy-service via la gateway API (port 8085).
 * Toutes les méthodes retournent des {@link Observable} (RxJS).
 * L'URL de base est construite depuis {@link environment.apiBaseUrl}.
 *
 * @example
 * ```ts
 * private readonly pharmacyService = inject(PharmacyService);
 *
 * this.pharmacyService.getDashboard()
 *   .subscribe(d => this.dashboard.set(d));
 * ```
 */
@Injectable({ providedIn: 'root' })
export class PharmacyService {

  private readonly http = inject(HttpClient);

  /** URL de base du pharmacy-service via la gateway. */
  private readonly base = `${environment.apiBaseUrl}/v1/pharmacy`;

  // ─────────────────────────────────────────────────────────────
  // Tableau de bord
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les données agrégées du tableau de bord pharmacie.
   *
   * @returns données du dashboard (KPIs + alertes urgentes)
   */
  getDashboard(): Observable<PharmacyDashboard> {
    return this.http.get<PharmacyDashboard>(`${this.base}/dashboard`);
  }

  // ─────────────────────────────────────────────────────────────
  // Médicaments expérimentaux
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de tous les médicaments expérimentaux enregistrés.
   *
   * @returns liste des médicaments expérimentaux
   */
  getDrugs(): Observable<InvestigationalDrug[]> {
    return this.http.get<InvestigationalDrug[]>(`${this.base}/drugs`);
  }

  /**
   * Crée un nouveau médicament expérimental.
   *
   * @param req données du médicament avec code de randomisation optionnel
   * @returns médicament créé (201)
   */
  createDrug(
    req: Partial<InvestigationalDrug> & { randomizationCode?: string },
  ): Observable<InvestigationalDrug> {
    return this.http.post<InvestigationalDrug>(`${this.base}/drugs`, req);
  }

  // ─────────────────────────────────────────────────────────────
  // Stocks
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de tous les stocks de médicaments.
   *
   * @returns liste des unités de stock
   */
  getStocks(): Observable<DrugStock[]> {
    return this.http.get<DrugStock[]>(`${this.base}/stocks`);
  }

  /**
   * Enregistre la réception d'un lot de médicament.
   *
   * @param req données de réception (drugId, quantité, lot, dates, emplacement)
   * @returns unité de stock créée (201)
   */
  receiveStock(req: Record<string, unknown>): Observable<DrugStock> {
    return this.http.post<DrugStock>(`${this.base}/stocks/receive`, req);
  }

  /**
   * Met à jour le statut d'une unité de stock (ex : QUARANTINE → AVAILABLE).
   *
   * @param id     identifiant UUID de l'unité de stock
   * @param status nouveau statut
   * @returns unité de stock mise à jour
   */
  updateStockStatus(id: string, status: StockStatus): Observable<DrugStock> {
    return this.http.patch<DrugStock>(`${this.base}/stocks/${id}/status`, { status });
  }

  /**
   * Importe des stocks depuis un fichier CSV ou XLSX.
   *
   * @param file fichier CSV/XLSX avec les colonnes attendues
   * @returns résultat d'import (compteurs + erreurs éventuelles)
   */
  importStocksFromCsv(file: File): Observable<StockImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<StockImportResult>(`${this.base}/stocks/import`, formData);
  }

  // ─────────────────────────────────────────────────────────────
  // Dispensations
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère la liste de toutes les dispensations.
   *
   * @returns liste des actes de dispensation
   */
  getDispensations(): Observable<Dispensation[]> {
    return this.http.get<Dispensation[]>(`${this.base}/dispensations`);
  }

  /**
   * Enregistre un acte de dispensation de médicament à un patient.
   *
   * @param req données de la dispensation
   * @returns dispensation créée (201)
   */
  createDispensation(req: Record<string, unknown>): Observable<Dispensation> {
    return this.http.post<Dispensation>(`${this.base}/dispensations`, req);
  }

  /**
   * Récupère l'historique des dispensations pour un patient donné.
   *
   * @param patientCode code anonymisé du patient
   * @returns liste des dispensations du patient
   */
  getDispensationsByPatient(patientCode: string): Observable<Dispensation[]> {
    return this.http.get<Dispensation[]>(
      `${this.base}/dispensations/patient/${patientCode}`,
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Alertes
  // ─────────────────────────────────────────────────────────────

  /**
   * Récupère les alertes pharmacie actives (péremptions, stocks faibles, quarantaines).
   *
   * @returns réponse agrégée avec liste d'alertes et compteur critique
   */
  getAlerts(): Observable<PharmacyAlertsResponse> {
    return this.http.get<PharmacyAlertsResponse>(`${this.base}/alerts`);
  }

  // ─────────────────────────────────────────────────────────────
  // Levée d'aveugle
  // ─────────────────────────────────────────────────────────────

  /**
   * Soumet une demande de levée d'aveugle d'urgence.
   *
   * <p>Action irréversible enregistrée dans l'audit trail réglementaire.
   *
   * @param req données de la demande (étude, patient, demandeur, raison)
   * @returns levée d'aveugle créée
   */
  requestEmergencyUnblinding(
    req: Record<string, unknown>,
  ): Observable<EmergencyUnblinding> {
    return this.http.post<EmergencyUnblinding>(
      `${this.base}/emergency-unblinding`,
      req,
    );
  }

  // ─────────────────────────────────────────────────────────────
  // Rapports
  // ─────────────────────────────────────────────────────────────

  /**
   * Télécharge le rapport PDF d'inventaire pharmacie.
   *
   * @returns blob PDF à ouvrir ou télécharger
   */
  downloadInventoryReport(): Observable<Blob> {
    return this.http.get(`${this.base}/reports/inventory`, {
      responseType: 'blob',
    });
  }
}

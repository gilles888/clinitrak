/**
 * Modèles de domaine du module Pharmacie de CliniTrak.
 *
 * <p>Ce fichier contient tous les types, interfaces, enums et constantes
 * utilisés par le module de gestion des médicaments expérimentaux (IMP),
 * des stocks, des dispensations et des levées d'aveugle.
 */

// ─────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────

/** Catégorie réglementaire d'un médicament expérimental. */
export enum DrugCategory {
  IMP     = 'IMP',
  NIMP    = 'NIMP',
  PLACEBO = 'PLACEBO',
}

/** Statut réglementaire d'un médicament expérimental. */
export enum DrugRegulatoryStatus {
  PENDING  = 'PENDING',
  APPROVED = 'APPROVED',
  EXPIRED  = 'EXPIRED',
  RECALLED = 'RECALLED',
}

/** Statut d'une unité de stock de médicament. */
export enum StockStatus {
  QUARANTINE = 'QUARANTINE',
  AVAILABLE  = 'AVAILABLE',
  DISPENSED  = 'DISPENSED',
  RETURNED   = 'RETURNED',
  DESTROYED  = 'DESTROYED',
}

/** Forme galénique d'un médicament. */
export enum DrugForm {
  TABLET    = 'TABLET',
  CAPSULE   = 'CAPSULE',
  INJECTION = 'INJECTION',
  SOLUTION  = 'SOLUTION',
  CREAM     = 'CREAM',
  OTHER     = 'OTHER',
}

/** Statut de facturation d'une dispensation. */
export enum BillingStatus {
  DRAFT     = 'DRAFT',
  SENT      = 'SENT',
  PAID      = 'PAID',
  DISPUTED  = 'DISPUTED',
  CANCELLED = 'CANCELLED',
}

/** Type d'alerte pharmacie. */
export enum AlertType {
  LOW_STOCK  = 'LOW_STOCK',
  EXPIRY_30  = 'EXPIRY_30',
  EXPIRY_7   = 'EXPIRY_7',
  QUARANTINE = 'QUARANTINE',
}

// ─────────────────────────────────────────────────────────────
// Interfaces
// ─────────────────────────────────────────────────────────────

/**
 * Médicament expérimental enregistré dans l'étude clinique.
 *
 * <p>Correspond à l'entité {@code InvestigationalDrug} côté pharmacy-service.
 */
export interface InvestigationalDrug {
  id: string;
  studyId: string;
  drugName: string;
  inn?: string;
  dosage?: string;
  form: DrugForm;
  formLabel: string;
  manufacturer?: string;
  batchNumber?: string;
  expiryDate?: string;
  storageConditions?: string;
  category: DrugCategory;
  categoryLabel: string;
  regulatoryStatus: DrugRegulatoryStatus;
  regulatoryStatusLabel: string;
  createdAt: string;
}

/**
 * Unité de stock d'un médicament expérimental.
 *
 * <p>Tracée de la réception jusqu'à la destruction ou le retour.
 */
export interface DrugStock {
  id: string;
  drugId: string;
  drugName: string;
  studyId: string;
  quantity: number;
  unit: string;
  receivedDate: string;
  expiryDate: string;
  batchNumber: string;
  location?: string;
  status: StockStatus;
  statusLabel: string;
  temperatureLog?: string;
  createdAt: string;
}

/**
 * Acte de dispensation d'un médicament à un patient.
 *
 * <p>Lie le médicament dispensé au patient (via son code anonymisé),
 * au pharmacien et au prescripteur.
 */
export interface Dispensation {
  id: string;
  drugId: string;
  drugName: string;
  studyId: string;
  patientCode: string;
  dispensationDate: string;
  pharmacistId: string;
  prescriberId: string;
  quantity: number;
  prescription?: string;
  visitNumber?: string;
  returnDate?: string;
  returnQuantity?: number;
  notes?: string;
  createdAt: string;
}

/**
 * Levée d'aveugle en urgence pour un patient d'une étude en double aveugle.
 *
 * <p>Action irréversible tracée dans l'audit trail réglementaire.
 */
export interface EmergencyUnblinding {
  id: string;
  studyId: string;
  patientCode: string;
  requestDate: string;
  requestedBy: string;
  reason: string;
  treatment?: string;
  approvedBy?: string;
  approvedAt?: string;
  createdAt: string;
}

/**
 * Alerte pharmacie individuelle (péremption, stock faible, quarantaine).
 */
export interface PharmacyAlert {
  type: AlertType;
  typeLabel: string;
  studyId: string;
  drugName: string;
  detail: string;
  alertDate: string;
}

/**
 * Réponse agrégée des alertes pharmacie avec compteur critique.
 */
export interface PharmacyAlertsResponse {
  alerts: PharmacyAlert[];
  criticalCount: number;
}

/**
 * Données agrégées du tableau de bord pharmacie.
 */
export interface PharmacyDashboard {
  totalDrugs: number;
  availableStocks: number;
  lowStockCount: number;
  expiringIn30Days: number;
  pendingDispensations: number;
  openUnblindings: number;
  urgentAlerts: PharmacyAlert[];
}

/**
 * Résultat d'un import CSV de stocks.
 */
export interface StockImportResult {
  imported: number;
  failed: number;
  errors: string[];
}

// ─────────────────────────────────────────────────────────────
// Mappings de sévérité PrimeNG
// ─────────────────────────────────────────────────────────────

/**
 * Mapping statut stock → sévérité PrimeNG Tag.
 */
export const STOCK_STATUS_SEVERITY: Record<StockStatus, string> = {
  [StockStatus.QUARANTINE]: 'warning',
  [StockStatus.AVAILABLE]:  'success',
  [StockStatus.DISPENSED]:  'info',
  [StockStatus.RETURNED]:   'secondary',
  [StockStatus.DESTROYED]:  'danger',
};

/**
 * Mapping statut réglementaire → sévérité PrimeNG Tag.
 */
export const DRUG_REGULATORY_SEVERITY: Record<DrugRegulatoryStatus, string> = {
  [DrugRegulatoryStatus.PENDING]:  'warning',
  [DrugRegulatoryStatus.APPROVED]: 'success',
  [DrugRegulatoryStatus.EXPIRED]:  'danger',
  [DrugRegulatoryStatus.RECALLED]: 'danger',
};

/**
 * Mapping type d'alerte → sévérité PrimeNG Tag.
 */
export const ALERT_TYPE_SEVERITY: Record<AlertType, string> = {
  [AlertType.LOW_STOCK]:  'warning',
  [AlertType.EXPIRY_30]:  'warning',
  [AlertType.EXPIRY_7]:   'danger',
  [AlertType.QUARANTINE]: 'info',
};

// ─────────────────────────────────────────────────────────────
// Options dropdown
// ─────────────────────────────────────────────────────────────

/** Options dropdown pour le champ forme galénique. */
export const DRUG_FORM_OPTIONS = [
  { label: 'Comprimé',    value: DrugForm.TABLET },
  { label: 'Gélule',      value: DrugForm.CAPSULE },
  { label: 'Injectable',  value: DrugForm.INJECTION },
  { label: 'Solution',    value: DrugForm.SOLUTION },
  { label: 'Crème',       value: DrugForm.CREAM },
  { label: 'Autre',       value: DrugForm.OTHER },
];

/** Options dropdown pour le champ catégorie de médicament. */
export const DRUG_CATEGORY_OPTIONS = [
  { label: 'Médicament expérimental (IMP)', value: DrugCategory.IMP },
  { label: 'Non-IMP',                        value: DrugCategory.NIMP },
  { label: 'Placebo',                        value: DrugCategory.PLACEBO },
];

/** Options dropdown pour le champ statut de stock. */
export const STOCK_STATUS_OPTIONS = Object.values(StockStatus).map(s => ({
  label: s,
  value: s,
}));

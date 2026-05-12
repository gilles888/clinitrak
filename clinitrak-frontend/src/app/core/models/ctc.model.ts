/**
 * Modèles de données du domaine CTC (Clinical Trial Center).
 *
 * <p>Contient les enums, interfaces, mappings de sévérité PrimeNG et options
 * de dropdowns utilisés dans l'ensemble du module CTC.
 */

// ─────────────────────────────────────────────────────────────
// Enums
// ─────────────────────────────────────────────────────────────

/** Type de guichet CTC. */
export enum DeskType { ACADEMIC = 'ACADEMIC', COMMERCIAL = 'COMMERCIAL' }

/** Type de demande guichet. */
export enum RequestType {
  NEW_STUDY  = 'NEW_STUDY',
  AMENDMENT  = 'AMENDMENT',
  EXTENSION  = 'EXTENSION',
  CLOSURE    = 'CLOSURE',
}

/** Statut d'une demande guichet. */
export enum RequestStatus {
  PENDING     = 'PENDING',
  ASSIGNED    = 'ASSIGNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED   = 'COMPLETED',
  REJECTED    = 'REJECTED',
}

/** Niveau de priorité. */
export enum Priority {
  LOW    = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH   = 'HIGH',
  URGENT = 'URGENT',
}

/** Type de visite de monitoring. */
export enum VisitType {
  INITIATION = 'INITIATION',
  ROUTINE    = 'ROUTINE',
  CLOSE_OUT  = 'CLOSE_OUT',
}

/** Statut d'une visite de monitoring. */
export enum VisitStatus {
  PLANNED   = 'PLANNED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/** Type de contrat financier. */
export enum ContractType {
  CONVENTION = 'CONVENTION',
  AMENDMENT  = 'AMENDMENT',
  SPONSOR    = 'SPONSOR',
}

/** Statut d'un contrat financier. */
export enum ContractStatus {
  DRAFT     = 'DRAFT',
  ACTIVE    = 'ACTIVE',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

/** Devise. */
export enum Currency { EUR = 'EUR', USD = 'USD', GBP = 'GBP' }

/** Type d'analyse statistique. */
export enum AnalysisType {
  DESCRIPTIVE  = 'DESCRIPTIVE',
  INFERENTIAL  = 'INFERENTIAL',
  SURVIVAL     = 'SURVIVAL',
  LONGITUDINAL = 'LONGITUDINAL',
}

/** Statut d'une demande statistique. */
export enum StatisticsStatus {
  PENDING     = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  DELIVERED   = 'DELIVERED',
  CANCELLED   = 'CANCELLED',
}

/** Type d'événement qualité. */
export enum EventType {
  DEVIATION = 'DEVIATION',
  SAE       = 'SAE',
  CAPA      = 'CAPA',
  AUDIT     = 'AUDIT',
}

/** Sévérité d'un événement qualité. */
export enum Severity {
  LOW      = 'LOW',
  MEDIUM   = 'MEDIUM',
  HIGH     = 'HIGH',
  CRITICAL = 'CRITICAL',
}

/** Statut d'un événement qualité. */
export enum EventStatus {
  OPEN        = 'OPEN',
  IN_PROGRESS = 'IN_PROGRESS',
  CLOSED      = 'CLOSED',
  CANCELLED   = 'CANCELLED',
}

/** Statut réglementaire d'une étude promoteur. */
export enum RegulatoryStatus {
  IN_PREPARATION = 'IN_PREPARATION',
  SUBMITTED      = 'SUBMITTED',
  APPROVED       = 'APPROVED',
  ONGOING        = 'ONGOING',
  COMPLETED      = 'COMPLETED',
}

// ─────────────────────────────────────────────────────────────
// Interfaces
// ─────────────────────────────────────────────────────────────

/** Demande guichet CTC. */
export interface TrialDeskRequest {
  id: string;
  studyId: string;
  deskType: DeskType;
  deskTypeLabel: string;
  requestDate: string;
  requestorName: string;
  requestorEmail: string;
  requestorOrganization: string;
  requestType: RequestType;
  requestTypeLabel: string;
  status: RequestStatus;
  statusLabel: string;
  assignedTo?: string;
  priority: Priority;
  priorityLabel: string;
  deadline?: string;
  notes?: string;
  createdAt: string;
}

/** Visite de monitoring d'une étude. */
export interface MonitoringVisit {
  id: string;
  studyId: string;
  visitDate: string;
  visitType: VisitType;
  visitTypeLabel: string;
  monitorName: string;
  findings?: string;
  correctionDeadline?: string;
  status: VisitStatus;
  statusLabel: string;
  report?: string;
  createdAt: string;
}

/** Contrat financier associé à une étude. */
export interface FinancialContract {
  id: string;
  studyId: string;
  contractType: ContractType;
  contractTypeLabel: string;
  contractDate: string;
  amount: number;
  currency: Currency;
  currencyLabel: string;
  billingSchedule?: string;
  paymentTerms?: string;
  status: ContractStatus;
  statusLabel: string;
  createdAt: string;
}

/** Demande d'analyse statistique. */
export interface StatisticsRequest {
  id: string;
  studyId: string;
  requestDate: string;
  requestorName: string;
  deadline: string;
  analysisType: AnalysisType;
  analysisTypeLabel: string;
  dataFormat: string;
  dataFormatLabel: string;
  status: StatisticsStatus;
  statusLabel: string;
  deliveredDate?: string;
  createdAt: string;
}

/** Étude dont le CUSL est promoteur. */
export interface SponsorStudy {
  id: string;
  studyId: string;
  projectManagerId?: string;
  craIds: string[];
  budgetTotal?: number;
  budgetSpent?: number;
  milestones?: string;
  regulatoryStatus: RegulatoryStatus;
  regulatoryStatusLabel: string;
  createdAt: string;
}

/** Événement qualité (déviation, EIG, CAPA, audit). */
export interface QualityEvent {
  id: string;
  studyId: string;
  eventType: EventType;
  eventTypeLabel: string;
  eventDate: string;
  severity: Severity;
  severityLabel: string;
  description: string;
  rootCause?: string;
  correctiveAction?: string;
  status: EventStatus;
  statusLabel: string;
  closureDate?: string;
  createdAt: string;
}

/** Données agrégées du tableau de bord CTC. */
export interface CTCDashboard {
  totalDeskRequests: number;
  pendingRequests: number;
  plannedVisits: number;
  openQualityEvents: number;
  criticalEvents: number;
  activeContracts: number;
  pendingStatistics: number;
  sponsorStudies: number;
  recentRequests: TrialDeskRequest[];
}

/** Événement individuel d'une timeline d'étude. */
export interface TimelineEvent {
  studyId: string;
  eventType: string;
  title: string;
  description?: string;
  eventDate: string;
  severity?: string;
}

/** Timeline complète d'une étude. */
export interface StudyTimeline {
  studyId: string;
  events: TimelineEvent[];
}

// ─────────────────────────────────────────────────────────────
// Severity mappings pour PrimeNG p-tag
// ─────────────────────────────────────────────────────────────

/** Type sévérité PrimeNG Tag. */
export type PrimeNGSeverity = 'success' | 'info' | 'secondary' | 'contrast' | 'warning' | 'danger' | undefined;

/** Mapping statut demande → sévérité PrimeNG Tag. */
export const REQUEST_STATUS_SEVERITY: Record<RequestStatus, PrimeNGSeverity> = {
  [RequestStatus.PENDING]:     'warning',
  [RequestStatus.ASSIGNED]:    'info',
  [RequestStatus.IN_PROGRESS]: 'info',
  [RequestStatus.COMPLETED]:   'success',
  [RequestStatus.REJECTED]:    'danger',
};

/** Mapping priorité → sévérité PrimeNG Tag. */
export const PRIORITY_SEVERITY: Record<Priority, PrimeNGSeverity> = {
  [Priority.LOW]:    'secondary',
  [Priority.MEDIUM]: 'info',
  [Priority.HIGH]:   'warning',
  [Priority.URGENT]: 'danger',
};

/** Mapping sévérité événement qualité → couleur CSS hexadécimale. */
export const SEVERITY_COLOR: Record<Severity, string> = {
  [Severity.LOW]:      '#10b981',
  [Severity.MEDIUM]:   '#f59e0b',
  [Severity.HIGH]:     '#ef4444',
  [Severity.CRITICAL]: '#7c3aed',
};

/** Mapping statut événement qualité → sévérité PrimeNG Tag. */
export const EVENT_STATUS_SEVERITY: Record<EventStatus, PrimeNGSeverity> = {
  [EventStatus.OPEN]:        'danger',
  [EventStatus.IN_PROGRESS]: 'warning',
  [EventStatus.CLOSED]:      'success',
  [EventStatus.CANCELLED]:   'secondary',
};

/** Mapping statut contrat → sévérité PrimeNG Tag. */
export const CONTRACT_STATUS_SEVERITY: Record<ContractStatus, PrimeNGSeverity> = {
  [ContractStatus.DRAFT]:     'secondary',
  [ContractStatus.ACTIVE]:    'success',
  [ContractStatus.COMPLETED]: 'info',
  [ContractStatus.CANCELLED]: 'danger',
};

/** Mapping statut réglementaire → sévérité PrimeNG Tag. */
export const REGULATORY_STATUS_SEVERITY: Record<RegulatoryStatus, PrimeNGSeverity> = {
  [RegulatoryStatus.IN_PREPARATION]: 'secondary',
  [RegulatoryStatus.SUBMITTED]:      'info',
  [RegulatoryStatus.APPROVED]:       'success',
  [RegulatoryStatus.ONGOING]:        'success',
  [RegulatoryStatus.COMPLETED]:      'secondary',
};

/** Mapping statut visite → sévérité PrimeNG Tag. */
export const VISIT_STATUS_SEVERITY: Record<VisitStatus, PrimeNGSeverity> = {
  [VisitStatus.PLANNED]:   'info',
  [VisitStatus.COMPLETED]: 'success',
  [VisitStatus.CANCELLED]: 'secondary',
};

/** Mapping statut statistique → sévérité PrimeNG Tag. */
export const STATISTICS_STATUS_SEVERITY: Record<StatisticsStatus, PrimeNGSeverity> = {
  [StatisticsStatus.PENDING]:     'warning',
  [StatisticsStatus.IN_PROGRESS]: 'info',
  [StatisticsStatus.DELIVERED]:   'success',
  [StatisticsStatus.CANCELLED]:   'secondary',
};

// ─────────────────────────────────────────────────────────────
// Options de dropdowns
// ─────────────────────────────────────────────────────────────

/** Options pour le type de guichet. */
export const DESK_TYPE_OPTIONS = Object.values(DeskType).map(v => ({
  label: v === DeskType.ACADEMIC ? 'Académique' : 'Commercial',
  value: v,
}));

/** Options pour le type de demande. */
export const REQUEST_TYPE_OPTIONS = [
  { label: 'Nouvelle étude', value: RequestType.NEW_STUDY },
  { label: 'Amendement',     value: RequestType.AMENDMENT },
  { label: 'Extension',      value: RequestType.EXTENSION },
  { label: 'Clôture',        value: RequestType.CLOSURE },
];

/** Options pour la priorité. */
export const PRIORITY_OPTIONS = [
  { label: 'Basse',  value: Priority.LOW },
  { label: 'Moyenne', value: Priority.MEDIUM },
  { label: 'Haute',  value: Priority.HIGH },
  { label: 'Urgent', value: Priority.URGENT },
];

/** Options pour le type de visite. */
export const VISIT_TYPE_OPTIONS = [
  { label: 'Initiation', value: VisitType.INITIATION },
  { label: 'Routine',    value: VisitType.ROUTINE },
  { label: 'Clôture',    value: VisitType.CLOSE_OUT },
];

/** Options pour le type d'événement qualité. */
export const EVENT_TYPE_OPTIONS = [
  { label: 'Déviation', value: EventType.DEVIATION },
  { label: 'EIG',       value: EventType.SAE },
  { label: 'CAPA',      value: EventType.CAPA },
  { label: 'Audit',     value: EventType.AUDIT },
];

/** Options pour le type d'analyse statistique. */
export const ANALYSIS_TYPE_OPTIONS = [
  { label: 'Descriptive',   value: AnalysisType.DESCRIPTIVE },
  { label: 'Inférentielle', value: AnalysisType.INFERENTIAL },
  { label: 'Survie',        value: AnalysisType.SURVIVAL },
  { label: 'Longitudinale', value: AnalysisType.LONGITUDINAL },
];

/** Options pour le statut de demande. */
export const REQUEST_STATUS_OPTIONS = [
  { label: 'En attente',   value: RequestStatus.PENDING },
  { label: 'Assignée',     value: RequestStatus.ASSIGNED },
  { label: 'En cours',     value: RequestStatus.IN_PROGRESS },
  { label: 'Terminée',     value: RequestStatus.COMPLETED },
  { label: 'Rejetée',      value: RequestStatus.REJECTED },
];

/** Options pour le type de contrat. */
export const CONTRACT_TYPE_OPTIONS = [
  { label: 'Convention', value: ContractType.CONVENTION },
  { label: 'Amendement', value: ContractType.AMENDMENT },
  { label: 'Promoteur',  value: ContractType.SPONSOR },
];

/** Options pour la sévérité. */
export const SEVERITY_OPTIONS = [
  { label: 'Basse',     value: Severity.LOW },
  { label: 'Moyenne',   value: Severity.MEDIUM },
  { label: 'Haute',     value: Severity.HIGH },
  { label: 'Critique',  value: Severity.CRITICAL },
];

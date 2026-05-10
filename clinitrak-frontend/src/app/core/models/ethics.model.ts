/**
 * Modèles de données du domaine Comité d'Éthique (frontend).
 *
 * <p>Ces interfaces, enums et constantes sont des miroirs des DTOs et enums Java du ethics-service.
 * Ils couvrent l'ensemble des entités manipulées : avis CE, réunions, rapports annuels,
 * correspondances et tableau de bord.
 */

// ─────────────────────────────────────────────────────────────
// Enums — miroirs des enums Java du ethics-service
// ─────────────────────────────────────────────────────────────

/** Type de demande soumise au Comité d'Éthique. */
export enum ReviewType {
  INITIAL       = 'INITIAL',
  AMENDMENT     = 'AMENDMENT',
  ANNUAL_REPORT = 'ANNUAL_REPORT',
  DEVIATION     = 'DEVIATION',
  SAE           = 'SAE',
}

/** Décision rendue par le Comité d'Éthique sur une demande. */
export enum ReviewDecision {
  PENDING              = 'PENDING',
  APPROVED             = 'APPROVED',
  REJECTED             = 'REJECTED',
  MORE_INFO_REQUESTED  = 'MORE_INFO_REQUESTED',
  WITHDRAWN            = 'WITHDRAWN',
}

/** Type de réunion du Comité d'Éthique. */
export enum MeetingType {
  ORDINARY      = 'ORDINARY',
  EXTRAORDINARY = 'EXTRAORDINARY',
}

/** Statut d'une réunion du Comité d'Éthique. */
export enum MeetingStatus {
  PLANNED     = 'PLANNED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED   = 'COMPLETED',
  CANCELLED   = 'CANCELLED',
}

/** Statut d'un rapport annuel d'étude. */
export enum AnnualReportStatus {
  PENDING  = 'PENDING',
  OVERDUE  = 'OVERDUE',
  RECEIVED = 'RECEIVED',
  REVIEWED = 'REVIEWED',
  WAIVED   = 'WAIVED',
}

/** Type de modèle de correspondance. */
export enum TemplateType {
  APPROVAL_LETTER,
  REJECTION_LETTER,
  MORE_INFO_LETTER,
  ANNUAL_REPORT_REQUEST,
  REMINDER,
  MEETING_NOTICE,
}

// ─────────────────────────────────────────────────────────────
// Interfaces — réponses API
// ─────────────────────────────────────────────────────────────

/**
 * Réponse complète d'un avis CE.
 */
export interface EthicsReviewResponse {
  id: string;
  tenantId: string;
  studyId: string;
  ethicsNumber: string;
  reviewType: ReviewType;
  reviewTypeLabel: string;
  submissionDate: string;
  reviewDate?: string;
  decision: ReviewDecision;
  decisionLabel: string;
  decisionDate?: string;
  comments?: string;
  nextReviewDate?: string;
  rapporteurName?: string;
  reminderSent: boolean;
  createdAt: string;
  updatedAt: string;
}

/**
 * Réponse complète d'une réunion CE.
 */
export interface MeetingResponse {
  id: string;
  tenantId: string;
  meetingDate: string;
  meetingTime?: string;
  meetingType: MeetingType;
  meetingTypeLabel: string;
  location: string;
  status: MeetingStatus;
  statusLabel: string;
  notes?: string;
  agendaItemCount: number;
  createdAt: string;
}

/**
 * Élément de l'ordre du jour d'une réunion CE.
 */
export interface AgendaItemResponse {
  id: string;
  studyId?: string;
  itemOrder: number;
  itemType?: string;
  durationMinutes: number;
  decision?: ReviewDecision;
  decisionLabel?: string;
  comments?: string;
}

/**
 * Réponse complète d'un rapport annuel.
 */
export interface AnnualReportResponse {
  id: string;
  tenantId: string;
  studyId: string;
  reportYear: number;
  dueDate: string;
  receivedDate?: string;
  status: AnnualReportStatus;
  statusLabel: string;
  reminderSent60: boolean;
  reminderSent30: boolean;
  reminderSent0: boolean;
  lastReminderDate?: string;
  notes?: string;
  daysUntilDue: number;
}

/**
 * Modèle de correspondance disponible.
 */
export interface TemplateResponse {
  id: string;
  templateCode: string;
  templateName: string;
  templateType: string;
  language: string;
  isActive: boolean;
  subject: string;
}

/**
 * Correspondance générée ou envoyée.
 */
export interface CorrespondenceResponse {
  id: string;
  studyId: string;
  reviewId?: string;
  subject: string;
  recipientEmail: string;
  recipientName: string;
  generatedDate: string;
  sentDate?: string;
  sent: boolean;
  sendError?: string;
  createdAt: string;
}

/**
 * Données du tableau de bord Comité d'Éthique.
 */
export interface EthicsDashboardResponse {
  pendingReviews: number;
  reviewsLastMonth: number;
  upcomingMeetings: number;
  nextMeeting?: MeetingResponse;
  annualReportsDue: number;
  annualReportsOverdue: number;
  reviewsByDecision: Record<string, number>;
  recentPendingReviews: EthicsReviewResponse[];
}

// ─────────────────────────────────────────────────────────────
// Interfaces — requêtes API
// ─────────────────────────────────────────────────────────────

/**
 * Requête de création d'un avis CE.
 */
export interface EthicsReviewCreateRequest {
  studyId: string;
  reviewType: ReviewType;
  submissionDate: string;
  rapporteurName?: string;
  comments?: string;
}

/**
 * Requête de mise à jour de la décision sur un avis CE.
 */
export interface DecisionUpdateRequest {
  decision: ReviewDecision;
  decisionDate: string;
  reviewDate?: string;
  nextReviewDate?: string;
  comments?: string;
}

/**
 * Requête de création d'une réunion CE.
 */
export interface MeetingCreateRequest {
  meetingDate: string;
  meetingTime?: string;
  meetingType: MeetingType;
  location: string;
  notes?: string;
}

/**
 * Requête de génération d'une correspondance.
 */
export interface CorrespondenceGenerateRequest {
  studyId: string;
  reviewId?: string;
  templateCode: string;
  recipientEmail: string;
  recipientName: string;
  additionalVariables?: Record<string, string>;
}

// ─────────────────────────────────────────────────────────────
// Constantes — options pour les dropdowns PrimeNG
// ─────────────────────────────────────────────────────────────

/** Options du dropdown "type d'avis CE". */
export const REVIEW_TYPE_OPTIONS = [
  { label: 'Initial',                           value: ReviewType.INITIAL },
  { label: 'Amendement',                        value: ReviewType.AMENDMENT },
  { label: 'Rapport annuel',                    value: ReviewType.ANNUAL_REPORT },
  { label: 'Déviation',                         value: ReviewType.DEVIATION },
  { label: 'Événement indésirable grave (SAE)', value: ReviewType.SAE },
];

/** Options du dropdown "décision CE". */
export const REVIEW_DECISION_OPTIONS = [
  { label: 'En attente',                  value: ReviewDecision.PENDING },
  { label: 'Approuvé',                    value: ReviewDecision.APPROVED },
  { label: 'Refusé',                      value: ReviewDecision.REJECTED },
  { label: 'Informations complémentaires', value: ReviewDecision.MORE_INFO_REQUESTED },
  { label: 'Retiré',                      value: ReviewDecision.WITHDRAWN },
];

/** Options du dropdown "type de réunion". */
export const MEETING_TYPE_OPTIONS = [
  { label: 'Ordinaire',     value: MeetingType.ORDINARY },
  { label: 'Extraordinaire', value: MeetingType.EXTRAORDINARY },
];

/** Options du dropdown "statut rapport annuel". */
export const ANNUAL_REPORT_STATUS_OPTIONS = [
  { label: 'En attente', value: AnnualReportStatus.PENDING },
  { label: 'En retard',  value: AnnualReportStatus.OVERDUE },
  { label: 'Reçu',       value: AnnualReportStatus.RECEIVED },
  { label: 'Examiné',    value: AnnualReportStatus.REVIEWED },
  { label: 'Dispensé',   value: AnnualReportStatus.WAIVED },
];

// ─────────────────────────────────────────────────────────────
// Constantes — severités PrimeNG Tag
// ─────────────────────────────────────────────────────────────

/**
 * Mapping décision CE → severité PrimeNG Tag.
 */
export const DECISION_SEVERITY: Record<string, string> = {
  PENDING:             'warning',
  APPROVED:            'success',
  REJECTED:            'danger',
  MORE_INFO_REQUESTED: 'info',
  WITHDRAWN:           'secondary',
};

/**
 * Mapping statut réunion → severité PrimeNG Tag.
 */
export const MEETING_STATUS_SEVERITY: Record<string, string> = {
  PLANNED:     'info',
  IN_PROGRESS: 'warning',
  COMPLETED:   'success',
  CANCELLED:   'danger',
};

/**
 * Mapping statut rapport annuel → severité PrimeNG Tag.
 */
export const ANNUAL_REPORT_STATUS_SEVERITY: Record<string, string> = {
  PENDING:  'info',
  OVERDUE:  'danger',
  RECEIVED: 'warning',
  REVIEWED: 'success',
  WAIVED:   'secondary',
};

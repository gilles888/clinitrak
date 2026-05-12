/**
 * Modèles de données du domaine Études Cliniques (frontend).
 *
 * <p>Ces interfaces et enums sont des miroirs des DTOs et enums Java du study-service.
 * Ils couvrent l'ensemble des entités manipulées : études, contacts, soumissions, patients.
 */

// ─────────────────────────────────────────────────────────────
// Enums — miroirs des enums Java du study-service
// ─────────────────────────────────────────────────────────────

/** Type d'étude clinique. */
export enum StudyType {
  INTERVENTIONAL  = 'INTERVENTIONAL',
  OBSERVATIONAL   = 'OBSERVATIONAL',
  EXPANDED_ACCESS = 'EXPANDED_ACCESS',
}

/** Type de sponsor. */
export enum SponsorType {
  ACADEMIC      = 'ACADEMIC',
  COMMERCIAL    = 'COMMERCIAL',
  INSTITUTIONAL = 'INSTITUTIONAL',
}

/** Phase de l'étude clinique. */
export enum StudyPhase {
  PHASE_1 = 'PHASE_1',
  PHASE_2 = 'PHASE_2',
  PHASE_3 = 'PHASE_3',
  PHASE_4 = 'PHASE_4',
  NA      = 'NA',
}

/** Statut de l'étude. */
export enum StudyStatus {
  DRAFT     = 'DRAFT',
  SUBMITTED = 'SUBMITTED',
  APPROVED  = 'APPROVED',
  ONGOING   = 'ONGOING',
  SUSPENDED = 'SUSPENDED',
  CLOSED    = 'CLOSED',
  WITHDRAWN = 'WITHDRAWN',
}

/** Type de contact associé à une étude. */
export enum ContactType {
  INVESTIGATOR = 'INVESTIGATOR',
  CRA          = 'CRA',
  COORDINATOR  = 'COORDINATOR',
  SPONSOR      = 'SPONSOR',
}

/** Type de soumission réglementaire. */
export enum SubmissionType {
  INITIAL        = 'INITIAL',
  AMENDMENT      = 'AMENDMENT',
  ANNUAL_REPORT  = 'ANNUAL_REPORT',
  SAFETY_REPORT  = 'SAFETY_REPORT',
  FINAL_REPORT   = 'FINAL_REPORT',
}

/** Statut d'une soumission réglementaire. */
export enum SubmissionStatus {
  PENDING      = 'PENDING',
  SUBMITTED    = 'SUBMITTED',
  ACKNOWLEDGED = 'ACKNOWLEDGED',
  APPROVED     = 'APPROVED',
  REJECTED     = 'REJECTED',
  WITHDRAWN    = 'WITHDRAWN',
}

/** Statut d'un patient dans une étude. */
export enum PatientStatus {
  SCREENED      = 'SCREENED',
  ENROLLED      = 'ENROLLED',
  ONGOING       = 'ONGOING',
  COMPLETED     = 'COMPLETED',
  WITHDRAWN     = 'WITHDRAWN',
  SCREEN_FAILED = 'SCREEN_FAILED',
}

// ─────────────────────────────────────────────────────────────
// Interfaces — miroirs des DTOs Java (responses)
// ─────────────────────────────────────────────────────────────

/** Résumé d'une étude (utilisé dans les listes paginées). */
export interface StudySummaryResponse {
  id: string;
  studyNumber: string;
  title: string;
  acronym: string | null;
  studyType: StudyType;
  currentStatus: StudyStatus;
  currentStatusLabel: string;
  sponsorType: SponsorType;
  sponsor: string;
  principalInvestigator: string;
  phase: StudyPhase;
  phaseLabel: string;
  startDate: string | null;
  endDate: string | null;
  targetEnrollment: number | null;
  currentEnrollment: number;
  isSponsorCusl: boolean;
}

/** Détail complet d'une étude. */
export interface StudyResponse {
  id: string;
  tenantId: string;
  studyNumber: string;
  ethicsNumber: string | null;
  eudractNumber: string | null;
  ctisNumber: string | null;
  title: string;
  acronym: string | null;
  studyType: StudyType;
  studyTypeLabel: string;
  sponsorType: SponsorType;
  sponsorTypeLabel: string;
  sponsor: string;
  principalInvestigator: string;
  therapeuticArea: string | null;
  phase: StudyPhase;
  phaseLabel: string;
  currentStatus: StudyStatus;
  currentStatusLabel: string;
  startDate: string | null;
  endDate: string | null;
  approvalDate: string | null;
  targetEnrollment: number | null;
  currentEnrollment: number;
  isSponsorCusl: boolean;
  description: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string;
}

/** Statistiques globales sur les études. */
export interface StudyStatisticsResponse {
  totalStudies: number;
  draftStudies: number;
  ongoingStudies: number;
  approvedStudies: number;
  closedStudies: number;
  sponsorCuslStudies: number;
  byTherapeuticArea: Record<string, number>;
  byPhase: Record<string, number>;
}

/** Entrée d'historique de statut. */
export interface StudyStatusHistory {
  id: string;
  status: StudyStatus;
  statusDate: string;
  comment: string | null;
  changedBy: string;
}

/** Contact associé à une étude. */
export interface ContactResponse {
  id: string;
  contactType: ContactType;
  contactTypeLabel: string;
  firstName: string;
  lastName: string;
  email: string | null;
  phone: string | null;
  organization: string | null;
  isPrimary: boolean;
  active: boolean;
}

/** Soumission réglementaire d'une étude. */
export interface SubmissionResponse {
  id: string;
  submissionType: SubmissionType;
  submissionDate: string;
  dueDate: string | null;
  status: SubmissionStatus;
  submittedBy: string | null;
  receivedDate: string | null;
  referenceNumber: string | null;
  comments: string | null;
  createdAt: string;
}

/** Patient (pseudonymisé) inscrit dans une étude. */
export interface PatientResponse {
  id: string;
  patientCode: string;
  inclusionDate: string | null;
  exclusionDate: string | null;
  status: PatientStatus;
  siteCode: string | null;
  notes: string | null;
  createdAt: string;
}

/** Réponse paginée générique du backend Spring Page<T>. */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

// ─────────────────────────────────────────────────────────────
// Critères de recherche (query params)
// ─────────────────────────────────────────────────────────────

/** Critères de recherche pour le endpoint GET /api/v1/studies. */
export interface StudySearchCriteria {
  ethicsNumber?: string;
  eudractNumber?: string;
  ctisNumber?: string;
  acronym?: string;
  titleKeyword?: string;
  studyType?: string;
  sponsorType?: SponsorType;
  status?: StudyStatus;
  therapeuticArea?: string;
  phase?: StudyPhase;
  /** Date ISO yyyy-MM-dd */
  startDateFrom?: string;
  /** Date ISO yyyy-MM-dd */
  startDateTo?: string;
  isSponsorCusl?: boolean;
  page?: number;
  size?: number;
  sort?: string;
}

// ─────────────────────────────────────────────────────────────
// Requêtes (request bodies)
// ─────────────────────────────────────────────────────────────

/** Corps de requête pour créer une étude. */
export interface StudyCreateRequest {
  title: string;
  acronym?: string;
  studyType: StudyType;
  sponsorType: SponsorType;
  sponsor: string;
  principalInvestigator: string;
  therapeuticArea?: string;
  phase: StudyPhase;
  startDate?: string;
  endDate?: string;
  targetEnrollment?: number;
  isSponsorCusl: boolean;
  description?: string;
  ethicsNumber?: string;
  eudractNumber?: string;
  ctisNumber?: string;
}

/** Corps de requête pour mettre à jour une étude (identique à la création). */
export interface StudyUpdateRequest extends StudyCreateRequest {}

/** Corps de requête pour changer le statut d'une étude. */
export interface StatusUpdateRequest {
  status: StudyStatus;
  /** Date ISO yyyy-MM-dd */
  statusDate: string;
  comment?: string;
}

/** Corps de requête pour ajouter un contact. */
export interface ContactRequest {
  contactType: ContactType;
  firstName: string;
  lastName: string;
  email?: string;
  phone?: string;
  organization?: string;
  isPrimary?: boolean;
}

/** Corps de requête pour ajouter une soumission. */
export interface SubmissionRequest {
  submissionType: SubmissionType;
  /** Date ISO yyyy-MM-dd */
  submissionDate: string;
  /** Date ISO yyyy-MM-dd */
  dueDate?: string;
  submittedBy?: string;
  referenceNumber?: string;
  comments?: string;
}

/** Corps de requête pour inscrire un patient. */
export interface PatientRequest {
  patientCode: string;
  /** Date ISO yyyy-MM-dd */
  inclusionDate?: string;
  status?: PatientStatus;
  siteCode?: string;
  notes?: string;
}

// ─────────────────────────────────────────────────────────────
// Constantes d'affichage (options pour les dropdowns PrimeNG)
// ─────────────────────────────────────────────────────────────

/** Options pour le dropdown StudyType. */
export const STUDY_TYPE_OPTIONS = [
  { label: 'Interventionnelle',  value: StudyType.INTERVENTIONAL },
  { label: 'Observationnelle',   value: StudyType.OBSERVATIONAL },
  { label: 'Accès élargi',       value: StudyType.EXPANDED_ACCESS },
];

/** Options pour le dropdown SponsorType. */
export const SPONSOR_TYPE_OPTIONS = [
  { label: 'Académique',         value: SponsorType.ACADEMIC },
  { label: 'Commercial',         value: SponsorType.COMMERCIAL },
  { label: 'Institutionnel',     value: SponsorType.INSTITUTIONAL },
];

/** Options pour le dropdown StudyPhase. */
export const STUDY_PHASE_OPTIONS = [
  { label: 'Phase I',            value: StudyPhase.PHASE_1 },
  { label: 'Phase II',           value: StudyPhase.PHASE_2 },
  { label: 'Phase III',          value: StudyPhase.PHASE_3 },
  { label: 'Phase IV',           value: StudyPhase.PHASE_4 },
  { label: 'N/A',                value: StudyPhase.NA },
];

/** Options pour le dropdown StudyStatus (tous les statuts). */
export const STUDY_STATUS_OPTIONS = [
  { label: 'Brouillon',          value: StudyStatus.DRAFT },
  { label: 'Soumis',             value: StudyStatus.SUBMITTED },
  { label: 'Approuvé',           value: StudyStatus.APPROVED },
  { label: 'En cours',           value: StudyStatus.ONGOING },
  { label: 'Suspendu',           value: StudyStatus.SUSPENDED },
  { label: 'Clôturé',            value: StudyStatus.CLOSED },
  { label: 'Retiré',             value: StudyStatus.WITHDRAWN },
];

/** Options pour le dropdown ContactType. */
export const CONTACT_TYPE_OPTIONS = [
  { label: 'Investigateur',      value: ContactType.INVESTIGATOR },
  { label: 'CRA',                value: ContactType.CRA },
  { label: 'Coordinateur',       value: ContactType.COORDINATOR },
  { label: 'Sponsor',            value: ContactType.SPONSOR },
];

/** Options pour le dropdown SubmissionType. */
export const SUBMISSION_TYPE_OPTIONS = [
  { label: 'Initiale',           value: SubmissionType.INITIAL },
  { label: 'Amendement',         value: SubmissionType.AMENDMENT },
  { label: 'Rapport annuel',     value: SubmissionType.ANNUAL_REPORT },
  { label: 'Rapport de sécurité',value: SubmissionType.SAFETY_REPORT },
  { label: 'Rapport final',      value: SubmissionType.FINAL_REPORT },
];

/** Options pour le dropdown PatientStatus. */
export const PATIENT_STATUS_OPTIONS = [
  { label: 'Screené',            value: PatientStatus.SCREENED },
  { label: 'Inscrit',            value: PatientStatus.ENROLLED },
  { label: 'En cours',           value: PatientStatus.ONGOING },
  { label: 'Terminé',            value: PatientStatus.COMPLETED },
  { label: 'Retiré',             value: PatientStatus.WITHDRAWN },
  { label: 'Échec screening',    value: PatientStatus.SCREEN_FAILED },
];

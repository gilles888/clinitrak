/**
 * Modèles de données du module Exchange (portail externe public).
 */

/** Rôles disponibles pour les utilisateurs externes du portail Exchange. */
export enum ExternalUserRole {
  COMPANY      = 'COMPANY',
  INVESTIGATOR = 'INVESTIGATOR',
  CE_REQUESTOR = 'CE_REQUESTOR',
}

/** Module interne cible d'une demande Exchange. */
export enum TargetModule {
  CE  = 'CE',
  CTC = 'CTC',
}

/** Type de demande Exchange. */
export enum ExchangeRequestType {
  NEW_STUDY   = 'NEW_STUDY',
  AMENDMENT   = 'AMENDMENT',
  EXTENSION   = 'EXTENSION',
  INFORMATION = 'INFORMATION',
}

/** Statut du cycle de vie d'une demande Exchange. */
export enum ExchangeStatus {
  DRAFT        = 'DRAFT',
  SUBMITTED    = 'SUBMITTED',
  UNDER_REVIEW = 'UNDER_REVIEW',
  ACCEPTED     = 'ACCEPTED',
  REJECTED     = 'REJECTED',
  MORE_INFO    = 'MORE_INFO',
}

/** Origine de l'émetteur d'un message Exchange. */
export enum SenderType {
  INTERNAL = 'INTERNAL',
  EXTERNAL = 'EXTERNAL',
}

/** Utilisateur externe enregistré sur le portail Exchange. */
export interface ExternalUser {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  organization?: string;
  role: ExternalUserRole;
  roleLabel: string;
  verifiedEmail: boolean;
  createdAt: string;
}

/** Demande soumise par un utilisateur externe via le portail Exchange. */
export interface ExchangeRequest {
  id: string;
  externalUserId: string;
  externalUserName: string;
  targetModule: TargetModule;
  targetModuleLabel: string;
  requestType: ExchangeRequestType;
  requestTypeLabel: string;
  title: string;
  description?: string;
  submissionDate?: string;
  status: ExchangeStatus;
  statusLabel: string;
  internalStudyId?: string;
  documents: ExchangeDocument[];
  createdAt: string;
}

/** Document joint à une demande Exchange. */
export interface ExchangeDocument {
  id: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  uploadedAt: string;
}

/** Message échangé dans le fil de discussion d'une demande Exchange. */
export interface ExchangeMessage {
  id: string;
  requestId: string;
  senderId: string;
  senderType: SenderType;
  senderTypeLabel: string;
  content: string;
  attachmentPath?: string;
  sentAt: string;
  readAt?: string;
}

/** Réponse de l'API lors d'un login Exchange réussi. */
export interface ExchangeLoginResponse {
  accessToken: string;
  tokenType: string;
  user: ExternalUser;
}

/** Severité PrimeNG par statut Exchange (pour p-tag). */
export const EXCHANGE_STATUS_SEVERITY: Record<ExchangeStatus, string> = {
  [ExchangeStatus.DRAFT]:        'secondary',
  [ExchangeStatus.SUBMITTED]:    'info',
  [ExchangeStatus.UNDER_REVIEW]: 'warning',
  [ExchangeStatus.ACCEPTED]:     'success',
  [ExchangeStatus.REJECTED]:     'danger',
  [ExchangeStatus.MORE_INFO]:    'warning',
};

/** Options de sélection pour le module cible. */
export const TARGET_MODULE_OPTIONS = [
  { label: 'Comité d\'Éthique (CE)', value: TargetModule.CE },
  { label: 'Centre de Thérapie Cellulaire (CTC)', value: TargetModule.CTC },
];

/** Options de sélection pour le type de demande. */
export const REQUEST_TYPE_OPTIONS = [
  { label: 'Nouvelle étude',         value: ExchangeRequestType.NEW_STUDY },
  { label: 'Amendement',             value: ExchangeRequestType.AMENDMENT },
  { label: 'Extension',              value: ExchangeRequestType.EXTENSION },
  { label: 'Demande d\'information', value: ExchangeRequestType.INFORMATION },
];

/** Options de sélection pour le rôle de l'utilisateur externe. */
export const EXTERNAL_ROLE_OPTIONS = [
  { label: 'Firme pharmaceutique', value: ExternalUserRole.COMPANY },
  { label: 'Investigateur',        value: ExternalUserRole.INVESTIGATOR },
  { label: 'Demandeur CE',         value: ExternalUserRole.CE_REQUESTOR },
];

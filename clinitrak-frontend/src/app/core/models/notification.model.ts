/**
 * Modèles de données pour le module Notifications de CliniTrak.
 */

/** Types de notifications générées par les différents services. */
export enum NotificationType {
  STOCK_ALERT        = 'STOCK_ALERT',
  MEETING_REMINDER   = 'MEETING_REMINDER',
  STUDY_UPDATE       = 'STUDY_UPDATE',
  ETHICS_DECISION    = 'ETHICS_DECISION',
  CTC_STATUS         = 'CTC_STATUS',
  DOCUMENT_UPLOADED  = 'DOCUMENT_UPLOADED',
  USER_INVITATION    = 'USER_INVITATION',
  SYSTEM             = 'SYSTEM',
  EXPIRY_WARNING     = 'EXPIRY_WARNING',
}

/** Représentation d'une notification utilisateur. */
export interface Notification {
  id: string;
  type: NotificationType;
  subject: string;
  message: string;
  read: boolean;
  createdAt: string;
  /** Lien optionnel vers la ressource concernée. */
  targetUrl?: string;
  /** Identifiant de la ressource liée (étude, dossier CE, etc.). */
  referenceId?: string;
}

/** Page paginée générique retournée par le backend Spring. */
export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
}

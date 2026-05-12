/**
 * Modeles de donnees pour le module Documents (GED MinIO) de CliniTrak.
 */

/** Types de documents supportes par la GED CliniTrak. */
export enum DocumentType {
  PROTOCOL           = 'PROTOCOL',
  INFORMED_CONSENT   = 'INFORMED_CONSENT',
  ETHICS_SUBMISSION  = 'ETHICS_SUBMISSION',
  ETHICS_DECISION    = 'ETHICS_DECISION',
  CRF                = 'CRF',
  INVESTIGATOR_BROCHURE = 'INVESTIGATOR_BROCHURE',
  REGULATORY         = 'REGULATORY',
  CONTRACT           = 'CONTRACT',
  REPORT             = 'REPORT',
  CORRESPONDENCE     = 'CORRESPONDENCE',
  OTHER              = 'OTHER',
}

/** Modules fonctionnels pouvant etre associes a un document. */
export enum DocumentModule {
  STUDY    = 'STUDY',
  ETHICS   = 'ETHICS',
  CTC      = 'CTC',
  PHARMACY = 'PHARMACY',
  BILLING  = 'BILLING',
  ADMIN    = 'ADMIN',
  OTHER    = 'OTHER',
}

/** DTO de reponse pour un document stocke dans MinIO. */
export interface DocumentResponse {
  id: string;
  fileName: string;
  originalFileName: string;
  fileSize: number;
  mimeType: string;
  documentType: DocumentType;
  module: DocumentModule;
  /** Identifiant de l'etude ou dossier associe. */
  referenceId?: string;
  description?: string;
  version: number;
  /** Identifiant du document parent (si c'est une version). */
  parentId?: string;
  uploadedBy: string;
  uploadedAt: string;
  updatedAt: string;
  tenantId: string;
}

/** Metadonnees envoyees avec l'upload d'un fichier. */
export interface DocumentUploadMetadata {
  documentType: DocumentType;
  module: DocumentModule;
  referenceId?: string;
  description?: string;
  /** true si ce document est une nouvelle version d'un document existant. */
  isNewVersion?: boolean;
  parentId?: string;
}

/** DTO de reponse contenant une URL pre-signee pour telechargement. */
export interface DocumentDownloadResponse {
  id: string;
  fileName: string;
  /** URL pre-signee MinIO valable pendant une duree limitee. */
  downloadUrl: string;
  expiresAt: string;
}

/** Parametres de filtrage pour la liste des documents. */
export interface DocumentFilter {
  module?: DocumentModule;
  documentType?: DocumentType;
  referenceId?: string;
  uploadedBy?: string;
  search?: string;
  page?: number;
  size?: number;
}

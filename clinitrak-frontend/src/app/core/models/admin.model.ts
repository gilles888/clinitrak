/**
 * Modèles de données du module Admin (administration multi-tenant).
 */

/** Statut d'un tenant dans la plateforme. */
export enum TenantStatus {
  ACTIVE    = 'ACTIVE',
  INACTIVE  = 'INACTIVE',
  SUSPENDED = 'SUSPENDED',
}

/** Type d'abonnement souscrit par un tenant. */
export enum SubscriptionType {
  BASIC        = 'BASIC',
  PROFESSIONAL = 'PROFESSIONAL',
  ENTERPRISE   = 'ENTERPRISE',
}

/** Modules fonctionnels activables par tenant. */
export enum ModuleType {
  STUDIES   = 'STUDIES',
  ETHICS    = 'ETHICS',
  CTC       = 'CTC',
  PHARMACY  = 'PHARMACY',
  EXCHANGE  = 'EXCHANGE',
  BILLING   = 'BILLING',
  DOCUMENTS = 'DOCUMENTS',
}

/** Représentation d'un tenant administré via le module Super Admin. */
export interface AdminTenant {
  id: string;
  name: string;
  slug: string;
  domain?: string;
  logoUrl?: string;
  activeModules: ModuleType[];
  configuration?: string;
  subscriptionType: SubscriptionType;
  subscriptionTypeLabel: string;
  status: TenantStatus;
  statusLabel: string;
  createdAt: string;
}

/** Entrée du journal d'audit système. */
export interface AuditLog {
  id: string;
  tenantId: string;
  userId: string;
  action: string;
  entityType?: string;
  entityId?: string;
  oldValue?: string;
  newValue?: string;
  ipAddress?: string;
  timestamp: string;
}

/** État de santé du système (services). */
export interface SystemHealth {
  status: string;
  services: Record<string, string>;
  checkedAt: string;
}

/** Statistiques d'utilisation d'un tenant. */
export interface TenantStatistics {
  tenantId: string;
  tenantName: string;
  userCount: number;
  studyCount: number;
  activeStudies: number;
  moduleUsage: Record<string, number>;
}

/** Severité PrimeNG par statut de tenant. */
export const TENANT_STATUS_SEVERITY: Record<TenantStatus, string> = {
  [TenantStatus.ACTIVE]:    'success',
  [TenantStatus.INACTIVE]:  'secondary',
  [TenantStatus.SUSPENDED]: 'danger',
};

/** Severité PrimeNG par type d'abonnement. */
export const SUBSCRIPTION_SEVERITY: Record<SubscriptionType, string> = {
  [SubscriptionType.BASIC]:        'secondary',
  [SubscriptionType.PROFESSIONAL]: 'info',
  [SubscriptionType.ENTERPRISE]:   'success',
};

/** Options de sélection pour les modules. */
export const MODULE_OPTIONS = Object.values(ModuleType).map(m => ({ label: m, value: m }));

/** Options de sélection pour les types d'abonnement. */
export const SUBSCRIPTION_OPTIONS = [
  { label: 'Basique',       value: SubscriptionType.BASIC },
  { label: 'Professionnel', value: SubscriptionType.PROFESSIONAL },
  { label: 'Entreprise',    value: SubscriptionType.ENTERPRISE },
];

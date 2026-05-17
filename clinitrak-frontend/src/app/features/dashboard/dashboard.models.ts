/**
 * Modèles de données du tableau de bord CliniTrak.
 *
 * <p>Ces interfaces couvrent les cards modules, les KPI, les tâches urgentes
 * et la timeline des échéances cliniques.
 */

import { SystemRole } from '../../core/models/user.model';

// ─────────────────────────────────────────────────────────────
// Actions rapides sur les cards modules
// ─────────────────────────────────────────────────────────────

/** Action rapide disponible depuis une card module. */
export interface QuickAction {
  /** Libellé du bouton. */
  label: string;
  /** Icône PrimeIcons (ex: 'pi-plus'). */
  icon: string;
  /** Route cible. */
  route: string;
  /** Paramètres de query optionnels. */
  queryParams?: Record<string, string>;
}

// ─────────────────────────────────────────────────────────────
// Badge sur les cards modules
// ─────────────────────────────────────────────────────────────

/** Badge informatif affiché sur une card module. */
export interface BadgeInfo {
  /** Texte du badge. */
  label: string;
  /** Sévérité PrimeNG. */
  severity: 'success' | 'warning' | 'danger' | 'info' | 'secondary';
}

// ─────────────────────────────────────────────────────────────
// Cards modules
// ─────────────────────────────────────────────────────────────

/**
 * Définition d'une card module du tableau de bord.
 * Chaque module opérationnel dispose d'une card cliquable.
 */
export interface DashboardCard {
  /** Identifiant unique de la card. */
  id: string;
  /** Titre affiché dans l'en-tête de la card. */
  title: string;
  /** Sous-titre / description courte. */
  subtitle: string;
  /** Route Angular cible au clic. */
  route: string;
  /** Icône PrimeIcons (ex: 'pi-book'). */
  icon: string;
  /** Couleur hex principale du module. */
  color: string;
  /** Couleur hex atténuée pour les fonds (ex: color + '20'). */
  colorLight: string;
  /** Valeur affichée dans le KPI de la card. */
  value: number | string;
  /** Texte de tendance / sous-valeur (ex: '+3 ce mois'). */
  trend?: string;
  /** true si la tendance est positive (vert), false sinon. */
  trendUp?: boolean;
  /** Actions rapides disponibles depuis cette card. */
  actions: QuickAction[];
  /** Badge optionnel affiché en haut à droite de la card. */
  badge?: BadgeInfo;
  /** Rôles requis pour voir cette card (undefined = tous les rôles). */
  requiredRoles?: SystemRole[];
}

// ─────────────────────────────────────────────────────────────
// Tâches urgentes
// ─────────────────────────────────────────────────────────────

/** Type d'une tâche urgente à traiter. */
export type UrgentTaskType =
  | 'approval'
  | 'review'
  | 'dispensation'
  | 'submission'
  | 'report';

/** Priorité d'une tâche urgente. */
export type TaskPriority = 'high' | 'medium' | 'low';

/**
 * Tâche urgente nécessitant une action dans les 7 prochains jours.
 */
export interface UrgentTask {
  /** Identifiant unique. */
  id: string;
  /** Titre de la tâche. */
  title: string;
  /** Nom du module source. */
  module: string;
  /** Icône PrimeIcons du module source. */
  moduleIcon: string;
  /** Date limite d'action. */
  deadline: Date;
  /** Priorité de la tâche. */
  priority: TaskPriority;
  /** Route vers la tâche. */
  route: string;
  /** Type de tâche. */
  type: UrgentTaskType;
}

// ─────────────────────────────────────────────────────────────
// Timeline des échéances
// ─────────────────────────────────────────────────────────────

/**
 * Élément de la timeline des échéances cliniques.
 */
export interface TimelineItem {
  /** Identifiant unique. */
  id: string;
  /** Libellé de l'échéance. */
  label: string;
  /** Date de l'échéance. */
  date: Date;
  /** Icône PrimeIcons. */
  icon: string;
  /** Couleur hex de l'item. */
  color: string;
  /** Nom du module source. */
  module: string;
  /** Route vers le détail. */
  route: string;
  /** true si l'échéance est dans moins de 7 jours. */
  isUrgent: boolean;
}

// ─────────────────────────────────────────────────────────────
// Statistiques sectorielles
// ─────────────────────────────────────────────────────────────

/** Statistiques résumées du module Études. */
export interface StudyStatsSummary {
  /** Nombre total d'études. */
  total: number;
  /** Nombre d'études actives (ONGOING + APPROVED). */
  active: number;
  /** Nombre d'études en attente (DRAFT + SUBMITTED). */
  pending: number;
  /** Variation en pourcentage vs mois précédent. */
  trend: number;
}

/** Statistiques résumées du module Comité d'Éthique. */
export interface EthicsStatsSummary {
  /** Soumissions en attente de traitement. */
  pendingSubmissions: number;
  /** Dossiers avec deadline urgente (< 7 jours). */
  urgentDeadlines: number;
  /** Délai moyen de traitement en jours. */
  averageProcessingDays: number;
}

/** Statistiques résumées du module CTC. */
export interface CtcStatsSummary {
  /** Dossiers CTC actifs. */
  activeDossiers: number;
  /** Fabrications en attente de validation. */
  pendingManufacturing: number;
}

/** Statistiques résumées du module Pharmacie. */
export interface PharmacyStatsSummary {
  /** Dispensations enregistrées aujourd'hui. */
  dispensationsToday: number;
  /** Médicaments en stock critique. */
  criticalStock: number;
}

/**
 * Agrégat de toutes les statistiques du tableau de bord.
 */
export interface DashboardStats {
  studyStats: StudyStatsSummary;
  ethicsStats: EthicsStatsSummary;
  ctcStats: CtcStatsSummary;
  pharmacyStats: PharmacyStatsSummary;
}

/**
 * Données complètes chargées pour le tableau de bord.
 */
export interface DashboardData {
  stats: DashboardStats;
  urgentTasks: UrgentTask[];
  timeline: TimelineItem[];
}

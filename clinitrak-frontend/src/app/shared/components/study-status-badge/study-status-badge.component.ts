import { Component, computed, input } from '@angular/core';
import { TagModule } from 'primeng/tag';
import { StudyStatus } from '../../../core/models/study.model';

/**
 * Mapping des statuts d'étude vers leurs libellés français.
 *
 * @internal
 */
const STATUS_LABELS: Record<StudyStatus, string> = {
  [StudyStatus.DRAFT]:     'Brouillon',
  [StudyStatus.SUBMITTED]: 'Soumis',
  [StudyStatus.APPROVED]:  'Approuvé',
  [StudyStatus.ONGOING]:   'En cours',
  [StudyStatus.SUSPENDED]: 'Suspendu',
  [StudyStatus.CLOSED]:    'Clôturé',
  [StudyStatus.WITHDRAWN]: 'Retiré',
};

/**
 * Mapping des statuts d'étude vers les sévérités PrimeNG Tag.
 *
 * @internal
 */
const STATUS_SEVERITY: Record<StudyStatus, 'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast'> = {
  [StudyStatus.DRAFT]:     'secondary',
  [StudyStatus.SUBMITTED]: 'info',
  [StudyStatus.APPROVED]:  'success',
  [StudyStatus.ONGOING]:   'success',
  [StudyStatus.SUSPENDED]: 'warning',
  [StudyStatus.CLOSED]:    'secondary',
  [StudyStatus.WITHDRAWN]: 'danger',
};

/**
 * Composant badge d'affichage du statut d'une étude clinique.
 *
 * <p>Utilise le composant {@code p-tag} de PrimeNG 17 avec la couleur sémantique
 * correspondant au statut. Basé sur les signals Angular pour la réactivité.
 *
 * @example
 * ```html
 * <ct-study-status-badge [status]="study.currentStatus" />
 * ```
 */
@Component({
  selector: 'ct-study-status-badge',
  standalone: true,
  imports: [TagModule],
  template: `<p-tag [value]="label()" [severity]="severity()" />`,
})
export class StudyStatusBadgeComponent {

  /** Statut de l'étude à afficher (obligatoire). */
  status = input.required<StudyStatus>();

  /** Libellé français calculé depuis le statut. */
  protected readonly label = computed(() => STATUS_LABELS[this.status()] ?? this.status());

  /** Sévérité PrimeNG Tag calculée depuis le statut. */
  protected readonly severity = computed<'success' | 'info' | 'warning' | 'danger' | 'secondary' | 'contrast'>(
    () => STATUS_SEVERITY[this.status()] ?? 'secondary'
  );
}

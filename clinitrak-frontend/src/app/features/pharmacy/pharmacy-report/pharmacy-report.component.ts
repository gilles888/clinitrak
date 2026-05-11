import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { PharmacyService } from '../../../core/services/pharmacy.service';

/**
 * Composant de génération et téléchargement des rapports pharmacie.
 *
 * <p>Expose actuellement le téléchargement du rapport PDF d'inventaire complet.
 * Le fichier est généré côté serveur par le pharmacy-service et renvoyé
 * en tant que blob binaire.
 *
 * <p>Le téléchargement s'effectue via une ancre dynamique injectée dans le DOM,
 * conformément à la pratique standard Angular pour les téléchargements de fichiers.
 */
@Component({
  selector: 'app-pharmacy-report',
  standalone: true,
  imports: [RouterLink, ButtonModule, CardModule],
  template: `
    <div class="tw-max-w-2xl">

      <!-- En-tête -->
      <div class="tw-mb-6">
        <h2 class="tw-text-2xl tw-font-bold tw-text-gray-900">Rapports pharmacie</h2>
        <p class="tw-text-sm tw-text-gray-500 tw-mt-1">Génération et téléchargement des rapports réglementaires</p>
      </div>

      <!-- Rapport d'inventaire -->
      <p-card styleClass="tw-border tw-border-gray-100">
        <div class="tw-space-y-4">
          <div class="tw-flex tw-items-start tw-gap-4">
            <i class="pi pi-file-pdf tw-text-4xl tw-text-red-500 tw-mt-1 tw-flex-shrink-0"></i>
            <div>
              <h3 class="tw-font-semibold tw-text-gray-800 tw-text-lg">Rapport d'inventaire pharmacie</h3>
              <p class="tw-text-sm tw-text-gray-500 tw-mt-1">
                Contient tous les stocks actifs avec médicament, étude, lot, quantité,
                emplacement, péremption et statut. Conforme aux exigences GCP.
              </p>
              <p class="tw-text-xs tw-text-gray-400 tw-mt-2">
                Format : PDF — Généré à la demande avec horodatage
              </p>
            </div>
          </div>

          <button
            pButton
            [label]="isGenerating() ? 'Génération en cours…' : 'Télécharger le rapport PDF'"
            [icon]="isGenerating() ? 'pi pi-spin pi-spinner' : 'pi pi-download'"
            [disabled]="isGenerating()"
            (click)="downloadReport()"
            class="tw-w-full"
          ></button>
        </div>
      </p-card>

      <!-- Autres rapports (placeholders) -->
      <div class="tw-mt-4 tw-grid tw-grid-cols-1 md:tw-grid-cols-2 tw-gap-4">

        <p-card styleClass="tw-border tw-border-gray-100 tw-opacity-50">
          <div class="tw-flex tw-items-center tw-gap-3">
            <i class="pi pi-file-excel tw-text-2xl tw-text-green-500"></i>
            <div>
              <p class="tw-font-medium tw-text-gray-700">Export dispensations</p>
              <p class="tw-text-xs tw-text-gray-400">Disponible prochainement</p>
            </div>
          </div>
        </p-card>

        <p-card styleClass="tw-border tw-border-gray-100 tw-opacity-50">
          <div class="tw-flex tw-items-center tw-gap-3">
            <i class="pi pi-file tw-text-2xl tw-text-blue-500"></i>
            <div>
              <p class="tw-font-medium tw-text-gray-700">Rapport levées d'aveugle</p>
              <p class="tw-text-xs tw-text-gray-400">Disponible prochainement</p>
            </div>
          </div>
        </p-card>

      </div>

    </div>
  `,
})
export class PharmacyReportComponent {

  private readonly pharmacyService = inject(PharmacyService);

  /** Indicateur de génération du rapport en cours. */
  protected readonly isGenerating = signal(false);

  /**
   * Télécharge le rapport PDF d'inventaire pharmacie.
   *
   * <p>Crée une URL objet à partir du blob reçu, déclenche le téléchargement
   * via une ancre dynamique, puis libère l'URL objet.
   */
  protected downloadReport(): void {
    this.isGenerating.set(true);
    this.pharmacyService.downloadInventoryReport().subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `inventaire-pharmacie-${new Date().toISOString().slice(0, 10)}.pdf`;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        this.isGenerating.set(false);
      },
      error: err => {
        console.error('Erreur génération rapport', err);
        this.isGenerating.set(false);
      },
    });
  }
}

import { Injectable } from '@angular/core';

/**
 * Service utilitaire d'export de donnees (CSV et Excel).
 *
 * <p>Fournit des methodes pour transformer des tableaux d'objets JavaScript
 * en fichiers telechargeables. L'export Excel utilise un fallback CSV
 * car la bibliotheque {@code xlsx} n'est pas incluse dans les dependances.
 *
 * <p>Usage :
 * <pre>
 * const exportService = inject(ExportService);
 * exportService.exportToCsv(myData, 'rapport-etudes');
 * </pre>
 */
@Injectable({ providedIn: 'root' })
export class ExportService {

  /**
   * Exporte un tableau d'objets au format CSV et declenche le telechargement.
   *
   * <p>Les valeurs sont entourees de guillemets doubles et les guillemets
   * dans les valeurs sont echappes (doublement des guillemets).
   *
   * @param data Tableau d'objets a exporter (toutes les cles du premier objet deviennent les en-tetes)
   * @param fileName Nom du fichier sans extension
   */
  exportToCsv(data: object[], fileName: string): void {
    if (!data.length) return;

    const headers = Object.keys(data[0]);
    const escapeCsv = (value: unknown): string => {
      const str = String(value ?? '').replace(/"/g, '""');
      return `"${str}"`;
    };

    const rows = data.map(row =>
      headers.map(h => escapeCsv((row as Record<string, unknown>)[h])).join(',')
    );

    const bom = '﻿'; // BOM UTF-8 pour compatibilite Excel
    const csv = bom + [headers.map(h => escapeCsv(h)).join(','), ...rows].join('\r\n');
    this.downloadFile(csv, `${fileName}.csv`, 'text/csv;charset=utf-8;');
  }

  /**
   * Exporte un tableau d'objets au format Excel (.csv avec extension .xlsx fallback).
   *
   * <p>La bibliotheque {@code xlsx} n'etant pas dans les dependances du projet,
   * cette methode utilise un export CSV avec l'extension .csv.
   * Pour un vrai Excel, installer {@code xlsx} via npm et adapter cette methode.
   *
   * @param data Tableau d'objets a exporter
   * @param fileName Nom du fichier sans extension
   */
  exportToExcel(data: object[], fileName: string): void {
    // Fallback CSV — xlsx non disponible dans les dependances actuelles
    // Pour activer le vrai Excel : npm install xlsx puis implementer avec WorkBook/WorkSheet
    this.exportToCsv(data, fileName);
  }

  /**
   * Exporte des donnees tabulaires en CSV avec en-tetes personnalises.
   *
   * @param headers Labels des colonnes (dans l'ordre)
   * @param rows Lignes de donnees sous forme de tableaux de valeurs
   * @param fileName Nom du fichier sans extension
   */
  exportTableToCsv(headers: string[], rows: unknown[][], fileName: string): void {
    if (!rows.length) return;

    const escapeCsv = (value: unknown): string => {
      const str = String(value ?? '').replace(/"/g, '""');
      return `"${str}"`;
    };

    const headerLine = headers.map(h => escapeCsv(h)).join(',');
    const dataLines = rows.map(row => row.map(cell => escapeCsv(cell)).join(','));

    const bom = '﻿';
    const csv = bom + [headerLine, ...dataLines].join('\r\n');
    this.downloadFile(csv, `${fileName}.csv`, 'text/csv;charset=utf-8;');
  }

  /**
   * Cree un lien de telechargement temporaire et declenche le download.
   *
   * @param content Contenu du fichier en chaine de caracteres
   * @param fileName Nom du fichier avec extension
   * @param mimeType Type MIME du fichier
   */
  private downloadFile(content: string, fileName: string, mimeType: string): void {
    const blob = new Blob([content], { type: mimeType });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = fileName;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
    document.body.removeChild(anchor);
    URL.revokeObjectURL(url);
  }
}

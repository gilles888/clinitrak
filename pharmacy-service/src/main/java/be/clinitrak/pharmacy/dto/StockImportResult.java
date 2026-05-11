package be.clinitrak.pharmacy.dto;

import java.util.List;

/**
 * Résultat d'un import de stocks depuis un fichier CSV/XLSX.
 *
 * @param imported nombre de lignes importées avec succès
 * @param failed   nombre de lignes en erreur
 * @param errors   liste des messages d'erreur par ligne
 */
public record StockImportResult(
    int imported,
    int failed,
    List<String> errors
) {}

package be.clinitrak.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Requête de réception d'un lot de médicament en stock.
 *
 * <p>Tout stock reçu est automatiquement mis en quarantaine ({@code QUARANTINE})
 * jusqu'à validation par le pharmacien.
 *
 * @param drugId       identifiant du médicament (obligatoire)
 * @param studyId      identifiant de l'étude (peut être null lors d'un import CSV)
 * @param quantity     quantité reçue (obligatoire)
 * @param unit         unité de mesure (ex: "comprimés", "flacons") (obligatoire)
 * @param receivedDate date de réception (obligatoire)
 * @param expiryDate   date de péremption du lot (obligatoire)
 * @param batchNumber  numéro de lot du fabricant (obligatoire)
 * @param location     emplacement physique en pharmacie
 */
public record StockReceiveRequest(
    @NotNull UUID drugId,
    @NotBlank String studyId,
    @NotNull Integer quantity,
    @NotBlank String unit,
    @NotNull LocalDate receivedDate,
    @NotNull LocalDate expiryDate,
    @NotBlank String batchNumber,
    String location
) {}

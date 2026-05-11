package be.clinitrak.pharmacy.dto;

import be.clinitrak.pharmacy.domain.enums.StockStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations complètes d'un stock de médicament.
 *
 * @param id            identifiant UUID du stock
 * @param drugId        identifiant UUID du médicament associé
 * @param drugName      nom du médicament associé
 * @param studyId       identifiant de l'étude
 * @param quantity      quantité disponible
 * @param unit          unité de mesure
 * @param receivedDate  date de réception
 * @param expiryDate    date de péremption
 * @param batchNumber   numéro de lot
 * @param location      emplacement physique
 * @param status        statut du stock (enum)
 * @param statusLabel   libellé français du statut
 * @param temperatureLog journal des températures
 * @param createdAt     date de création
 */
public record StockResponse(
    UUID id,
    UUID drugId,
    String drugName,
    String studyId,
    Integer quantity,
    String unit,
    LocalDate receivedDate,
    LocalDate expiryDate,
    String batchNumber,
    String location,
    StockStatus status,
    String statusLabel,
    String temperatureLog,
    LocalDateTime createdAt
) {}

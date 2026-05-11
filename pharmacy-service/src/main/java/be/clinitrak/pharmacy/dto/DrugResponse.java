package be.clinitrak.pharmacy.dto;

import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import be.clinitrak.pharmacy.domain.enums.DrugRegulatoryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Réponse contenant les informations complètes d'un médicament expérimental.
 *
 * @param id                    identifiant UUID du médicament
 * @param studyId               identifiant de l'étude associée
 * @param drugName              dénomination commerciale ou nom de code
 * @param inn                   dénomination commune internationale
 * @param dosage                dosage
 * @param form                  forme pharmaceutique (enum)
 * @param formLabel             libellé français de la forme
 * @param manufacturer          fabricant
 * @param batchNumber           numéro de lot
 * @param expiryDate            date de péremption
 * @param storageConditions     conditions de stockage
 * @param category              catégorie réglementaire (enum)
 * @param categoryLabel         libellé français de la catégorie
 * @param regulatoryStatus      statut réglementaire (enum)
 * @param regulatoryStatusLabel libellé français du statut réglementaire
 * @param createdAt             date de création
 */
public record DrugResponse(
    UUID id,
    String studyId,
    String drugName,
    String inn,
    String dosage,
    DrugForm form,
    String formLabel,
    String manufacturer,
    String batchNumber,
    LocalDate expiryDate,
    String storageConditions,
    DrugCategory category,
    String categoryLabel,
    DrugRegulatoryStatus regulatoryStatus,
    String regulatoryStatusLabel,
    LocalDateTime createdAt
) {}

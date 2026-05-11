package be.clinitrak.pharmacy.dto;

import be.clinitrak.pharmacy.domain.enums.DrugCategory;
import be.clinitrak.pharmacy.domain.enums.DrugForm;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/**
 * Requête de création d'un médicament expérimental.
 *
 * @param studyId            identifiant de l'étude clinique associée (obligatoire)
 * @param drugName           dénomination commerciale ou nom de code du médicament (obligatoire)
 * @param inn                dénomination commune internationale (DCI)
 * @param dosage             dosage (ex: "100mg")
 * @param form               forme pharmaceutique (obligatoire)
 * @param manufacturer       nom du fabricant
 * @param batchNumber        numéro de lot
 * @param expiryDate         date de péremption
 * @param storageConditions  conditions de stockage
 * @param category           catégorie réglementaire (IMP, NIMP, Placebo) (obligatoire)
 * @param randomizationCode  code de randomisation en clair (sera chiffré avant persistance)
 */
public record DrugCreateRequest(
    @NotBlank String studyId,
    @NotBlank String drugName,
    String inn,
    String dosage,
    @NotNull DrugForm form,
    String manufacturer,
    String batchNumber,
    LocalDate expiryDate,
    String storageConditions,
    @NotNull DrugCategory category,
    String randomizationCode
) {}

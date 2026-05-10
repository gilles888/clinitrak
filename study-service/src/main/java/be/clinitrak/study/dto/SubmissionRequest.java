package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.SubmissionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de création d'une soumission réglementaire.
 *
 * @param submissionType  type de soumission (obligatoire)
 * @param submissionDate  date de soumission effective (obligatoire)
 * @param dueDate         date limite réglementaire
 * @param submittedBy     identifiant de la personne ayant soumis
 * @param referenceNumber numéro de référence attribué par les autorités
 * @param comments        commentaires libres sur la soumission
 */
public record SubmissionRequest(
    @NotNull(message = "Le type de soumission est obligatoire")
    SubmissionType submissionType,

    @NotNull(message = "La date de soumission est obligatoire")
    LocalDate submissionDate,

    LocalDate dueDate,

    @Size(max = 255, message = "Le soumetteur ne peut dépasser 255 caractères")
    String submittedBy,

    @Size(max = 100, message = "Le numéro de référence ne peut dépasser 100 caractères")
    String referenceNumber,

    String comments
) {}

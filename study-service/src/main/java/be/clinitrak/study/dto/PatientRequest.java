package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.PatientStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de création d'un patient pseudonymisé dans une étude.
 *
 * <p>Conformément au RGPD, aucune donnée directement identifiante ne doit être
 * incluse dans ce DTO. L'identité réelle est gérée dans le DPI hospitalier.
 *
 * @param patientCode    code pseudonyme unique dans l'étude (obligatoire)
 * @param inclusionDate  date d'inclusion dans l'étude
 * @param status         statut initial du patient (défaut: SCREENED)
 * @param siteCode       code du centre participant
 * @param notes          notes non identifiantes sur le suivi
 */
public record PatientRequest(
    @NotBlank(message = "Le code patient est obligatoire")
    @Size(max = 50, message = "Le code patient ne peut dépasser 50 caractères")
    String patientCode,

    LocalDate inclusionDate,

    PatientStatus status,

    @Size(max = 50, message = "Le code site ne peut dépasser 50 caractères")
    String siteCode,

    String notes
) {}

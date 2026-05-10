package be.clinitrak.study.dto;

import be.clinitrak.study.domain.enums.StudyPhase;
import be.clinitrak.study.domain.enums.StudyType;
import be.clinitrak.study.domain.enums.SponsorType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * DTO de création d'une étude clinique.
 *
 * <p>Seuls le titre, le type d'étude et le type de promoteur sont obligatoires.
 * Le numéro d'étude est généré automatiquement si absent.
 *
 * @param title                titre complet de l'étude (obligatoire)
 * @param acronym              acronyme de l'étude
 * @param studyType            type d'étude (obligatoire)
 * @param sponsorType          type de promoteur (obligatoire)
 * @param sponsor              nom du promoteur
 * @param principalInvestigator nom de l'investigateur principal
 * @param therapeuticArea      domaine thérapeutique
 * @param phase                phase de l'essai clinique
 * @param startDate            date de début prévue
 * @param endDate              date de fin prévue
 * @param targetEnrollment     nombre cible de patients
 * @param isSponsorCusl        indique si les CUSL sont promoteur
 * @param description          description libre du protocole
 * @param ethicsNumber         numéro du Comité d'Éthique
 * @param eudractNumber        numéro EudraCT
 * @param ctisNumber           numéro CTIS EU
 */
public record StudyCreateRequest(
    @NotBlank(message = "Le titre de l'étude est obligatoire")
    @Size(max = 500, message = "Le titre ne peut dépasser 500 caractères")
    String title,

    @Size(max = 50, message = "L'acronyme ne peut dépasser 50 caractères")
    String acronym,

    @NotNull(message = "Le type d'étude est obligatoire")
    StudyType studyType,

    @NotNull(message = "Le type de promoteur est obligatoire")
    SponsorType sponsorType,

    @Size(max = 255, message = "Le nom du promoteur ne peut dépasser 255 caractères")
    String sponsor,

    @Size(max = 255, message = "Le nom de l'investigateur ne peut dépasser 255 caractères")
    String principalInvestigator,

    @Size(max = 100, message = "Le domaine thérapeutique ne peut dépasser 100 caractères")
    String therapeuticArea,

    StudyPhase phase,

    LocalDate startDate,

    LocalDate endDate,

    Integer targetEnrollment,

    boolean isSponsorCusl,

    String description,

    @Size(max = 100, message = "Le numéro éthique ne peut dépasser 100 caractères")
    String ethicsNumber,

    @Size(max = 20, message = "Le numéro EudraCT ne peut dépasser 20 caractères")
    String eudractNumber,

    @Size(max = 30, message = "Le numéro CTIS ne peut dépasser 30 caractères")
    String ctisNumber
) {}

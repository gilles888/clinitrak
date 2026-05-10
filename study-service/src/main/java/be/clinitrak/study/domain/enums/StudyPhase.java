package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Phase d'un essai clinique interventionnel selon la classification ICH.
 */
public enum StudyPhase {

    /** Phase I : premiers essais chez l'humain, évaluation de la sécurité. */
    PHASE_1("Phase I"),

    /** Phase II : évaluation de l'efficacité et des effets secondaires. */
    PHASE_2("Phase II"),

    /** Phase III : essais à grande échelle pour confirmer l'efficacité. */
    PHASE_3("Phase III"),

    /** Phase IV : pharmacovigilance post-commercialisation. */
    PHASE_4("Phase IV"),

    /** Non applicable (études observationnelles, accès élargi). */
    NA("N/A");

    private final String label;

    StudyPhase(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé de la phase d'étude.
     *
     * @return libellé de la phase
     */
    public String getLabel() {
        return label;
    }

    /**
     * Retourne le nom de l'enum pour la sérialisation JSON.
     *
     * @return nom de la constante enum
     */
    @JsonValue
    public String getName() {
        return name();
    }
}

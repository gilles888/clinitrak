package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type d'étude clinique selon la classification internationale.
 */
public enum StudyType {

    /** Étude interventionnelle : administration d'un traitement expérimental. */
    INTERVENTIONAL("Interventionnel"),

    /** Étude observationnelle : aucune intervention, observation uniquement. */
    OBSERVATIONAL("Observationnel"),

    /** Accès élargi (usage compassionnel) pour des patients hors essai. */
    EXPANDED_ACCESS("Accès élargi");

    private final String label;

    StudyType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type d'étude.
     *
     * @return libellé en français
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

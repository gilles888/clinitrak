package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type de contact associé à une étude clinique.
 */
public enum ContactType {

    /** Investigateur principal ou co-investigateur. */
    INVESTIGATOR("Investigateur"),

    /** Attaché de recherche clinique (CRA / Clinical Research Associate). */
    CRA("Attaché de recherche clinique"),

    /** Coordinateur de recherche clinique. */
    COORDINATOR("Coordinateur"),

    /** Représentant du promoteur. */
    SPONSOR("Promoteur");

    private final String label;

    ContactType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de contact.
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

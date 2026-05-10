package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type de promoteur d'une étude clinique.
 */
public enum SponsorType {

    /** Promoteur académique (université, hôpital universitaire). */
    ACADEMIC("Académique"),

    /** Promoteur commercial (industrie pharmaceutique). */
    COMMERCIAL("Commercial"),

    /** Promoteur institutionnel (organisme public, association). */
    INSTITUTIONAL("Institutionnel");

    private final String label;

    SponsorType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de promoteur.
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

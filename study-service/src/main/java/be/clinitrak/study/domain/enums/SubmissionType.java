package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Type de soumission réglementaire pour une étude clinique.
 */
public enum SubmissionType {

    /** Soumission initiale du protocole. */
    INITIAL("Soumission initiale"),

    /** Amendement au protocole ou au dossier. */
    AMENDMENT("Amendement"),

    /** Rapport annuel de sécurité (DSUR/ASR). */
    ANNUAL_REPORT("Rapport annuel"),

    /** Rapport de sécurité ad hoc (SUSAR, IND safety report). */
    SAFETY_REPORT("Rapport de sécurité"),

    /** Rapport final de l'étude. */
    FINAL_REPORT("Rapport final");

    private final String label;

    SubmissionType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de soumission.
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

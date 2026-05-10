package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Statut d'une soumission réglementaire.
 */
public enum SubmissionStatus {

    /** Soumission préparée, pas encore envoyée. */
    PENDING("En attente"),

    /** Soumission envoyée aux autorités. */
    SUBMITTED("Soumis"),

    /** Réception accusée par les autorités. */
    ACKNOWLEDGED("Accusé de réception"),

    /** Soumission approuvée par les autorités. */
    APPROVED("Approuvé"),

    /** Soumission rejetée par les autorités. */
    REJECTED("Rejeté"),

    /** Soumission retirée par le promoteur. */
    WITHDRAWN("Retiré");

    private final String label;

    SubmissionStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut de soumission.
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

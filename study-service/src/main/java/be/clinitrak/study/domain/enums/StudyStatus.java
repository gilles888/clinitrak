package be.clinitrak.study.domain.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Statut réglementaire d'une étude clinique.
 *
 * <p>Les transitions de statut sont tracées dans {@code StudyStatusHistory}.
 */
public enum StudyStatus {

    /** Étude en cours de rédaction, non encore soumise. */
    DRAFT("Brouillon"),

    /** Dossier soumis aux autorités compétentes (FAMHP, CE). */
    SUBMITTED("Soumis"),

    /** Autorisation accordée par les autorités. */
    APPROVED("Approuvé"),

    /** Étude active, recrutement et/ou traitement en cours. */
    ONGOING("En cours"),

    /** Étude temporairement suspendue. */
    SUSPENDED("Suspendu"),

    /** Étude terminée, tous les objectifs atteints. */
    CLOSED("Clôturé"),

    /** Étude retirée avant complétion. */
    WITHDRAWN("Retiré");

    private final String label;

    StudyStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut.
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

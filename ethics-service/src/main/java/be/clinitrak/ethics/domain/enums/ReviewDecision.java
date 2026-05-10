package be.clinitrak.ethics.domain.enums;

/**
 * Décision rendue par le Comité d'Éthique sur un avis.
 *
 * <p>Le cycle de vie habituel est : PENDING → (APPROVED | REJECTED | MORE_INFO_REQUESTED | WITHDRAWN).
 */
public enum ReviewDecision {

    /** Avis en attente de décision du Comité. */
    PENDING("En attente"),

    /** Avis approuvé par le Comité d'Éthique. */
    APPROVED("Approuvé"),

    /** Avis refusé par le Comité d'Éthique. */
    REJECTED("Refusé"),

    /** Le Comité demande des informations complémentaires avant de statuer. */
    MORE_INFO_REQUESTED("Informations complémentaires requises"),

    /** Le promoteur ou l'investigateur a retiré la soumission. */
    WITHDRAWN("Retiré");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    ReviewDecision(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de la décision.
     *
     * @return libellé de la décision
     */
    public String getLabel() {
        return label;
    }
}

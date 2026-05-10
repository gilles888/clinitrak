package be.clinitrak.ethics.domain.enums;

/**
 * Statut du cycle de vie d'une réunion du Comité d'Éthique.
 */
public enum MeetingStatus {

    /** Réunion planifiée, pas encore tenue. */
    PLANNED("Planifié"),

    /** Réunion en cours de déroulement. */
    IN_PROGRESS("En cours"),

    /** Réunion terminée et clôturée. */
    COMPLETED("Clôturé"),

    /** Réunion annulée avant sa tenue. */
    CANCELLED("Annulé");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    MeetingStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut de réunion.
     *
     * @return libellé du statut
     */
    public String getLabel() {
        return label;
    }
}

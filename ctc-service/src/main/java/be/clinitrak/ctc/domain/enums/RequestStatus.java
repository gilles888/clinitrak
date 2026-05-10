package be.clinitrak.ctc.domain.enums;

/**
 * Statut d'une demande adressée au desk CTC.
 *
 * <p>Cycle de vie : PENDING → ASSIGNED → IN_PROGRESS → COMPLETED (ou REJECTED).
 */
public enum RequestStatus {

    /** Demande reçue, en attente d'assignation. */
    PENDING("En attente"),

    /** Demande assignée à un responsable. */
    ASSIGNED("Assignée"),

    /** Demande en cours de traitement. */
    IN_PROGRESS("En cours"),

    /** Demande traitée et clôturée. */
    COMPLETED("Terminée"),

    /** Demande rejetée (non recevable ou incomplète). */
    REJECTED("Rejetée");

    private final String label;

    RequestStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce statut.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

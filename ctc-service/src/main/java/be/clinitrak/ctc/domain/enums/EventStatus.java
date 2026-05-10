package be.clinitrak.ctc.domain.enums;

/**
 * Statut d'un événement qualité.
 *
 * <p>Cycle de vie : OPEN → IN_PROGRESS → CLOSED (ou CANCELLED).
 */
public enum EventStatus {

    /** Événement ouvert, en attente de prise en charge. */
    OPEN("Ouverte"),

    /** Événement pris en charge, actions correctives en cours. */
    IN_PROGRESS("En cours"),

    /** Événement clôturé après résolution. */
    CLOSED("Clôturée"),

    /** Événement annulé (sans suite nécessaire). */
    CANCELLED("Annulée");

    private final String label;

    EventStatus(String label) {
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

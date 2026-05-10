package be.clinitrak.ctc.domain.enums;

/**
 * Statut d'une visite de monitoring.
 *
 * <p>Cycle de vie : PLANNED → COMPLETED (ou CANCELLED).
 */
public enum VisitStatus {

    /** Visite planifiée, pas encore réalisée. */
    PLANNED("Planifiée"),

    /** Visite réalisée et clôturée. */
    COMPLETED("Complétée"),

    /** Visite annulée (raison documentée dans le rapport). */
    CANCELLED("Annulée");

    private final String label;

    VisitStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce statut de visite.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

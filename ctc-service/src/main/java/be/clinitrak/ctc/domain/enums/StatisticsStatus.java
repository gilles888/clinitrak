package be.clinitrak.ctc.domain.enums;

/**
 * Statut d'une demande d'analyse statistique.
 *
 * <p>Cycle de vie : PENDING → IN_PROGRESS → DELIVERED (ou CANCELLED).
 */
public enum StatisticsStatus {

    /** Demande reçue, en attente de prise en charge par le biostatisticien. */
    PENDING("En attente"),

    /** Analyse en cours de réalisation. */
    IN_PROGRESS("En cours"),

    /** Résultats livrés au demandeur. */
    DELIVERED("Livré"),

    /** Demande annulée. */
    CANCELLED("Annulé");

    private final String label;

    StatisticsStatus(String label) {
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

package be.clinitrak.ethics.domain.enums;

/**
 * Statut d'un rapport annuel attendu par le Comité d'Éthique.
 *
 * <p>Les rappels automatiques sont déclenchés pour les statuts PENDING et OVERDUE.
 */
public enum AnnualReportStatus {

    /** Rapport attendu, délai non encore dépassé. */
    PENDING("En attente"),

    /** Délai de soumission dépassé sans réception du rapport. */
    OVERDUE("En retard"),

    /** Rapport reçu par le secrétariat CE. */
    RECEIVED("Reçu"),

    /** Rapport reçu et examiné par le Comité. */
    REVIEWED("Examiné"),

    /** Le Comité a dispensé l'investigateur de soumettre ce rapport. */
    WAIVED("Dispensé");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    AnnualReportStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut du rapport annuel.
     *
     * @return libellé du statut
     */
    public String getLabel() {
        return label;
    }
}

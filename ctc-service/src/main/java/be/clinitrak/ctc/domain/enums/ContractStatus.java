package be.clinitrak.ctc.domain.enums;

/**
 * Statut d'un contrat financier.
 *
 * <p>Cycle de vie : DRAFT → ACTIVE → COMPLETED (ou CANCELLED).
 */
public enum ContractStatus {

    /** Contrat en cours de rédaction, non signé. */
    DRAFT("Brouillon"),

    /** Contrat signé et en vigueur. */
    ACTIVE("Actif"),

    /** Contrat arrivé à terme normalement. */
    COMPLETED("Terminé"),

    /** Contrat résilié avant terme. */
    CANCELLED("Annulé");

    private final String label;

    ContractStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce statut de contrat.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

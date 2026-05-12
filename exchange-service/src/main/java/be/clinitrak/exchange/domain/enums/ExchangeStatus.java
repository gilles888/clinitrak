package be.clinitrak.exchange.domain.enums;

/**
 * Statut du cycle de vie d'une demande d'échange.
 *
 * <p>Les transitions valides sont :
 * DRAFT → SUBMITTED → UNDER_REVIEW → ACCEPTED | REJECTED | MORE_INFO
 */
public enum ExchangeStatus {

    /** Brouillon : demande créée mais non soumise. */
    DRAFT("Brouillon"),

    /** Soumis : demande soumise, en attente de traitement. */
    SUBMITTED("Soumis"),

    /** En cours d'examen par l'équipe interne. */
    UNDER_REVIEW("En cours d'examen"),

    /** Demande acceptée, un identifiant d'étude interne sera assigné. */
    ACCEPTED("Accepté"),

    /** Demande rejetée. */
    REJECTED("Rejeté"),

    /** Informations complémentaires demandées à l'utilisateur externe. */
    MORE_INFO("Informations complémentaires");

    private final String label;

    ExchangeStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut.
     *
     * @return libellé du statut
     */
    public String getLabel() {
        return label;
    }
}

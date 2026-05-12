package be.clinitrak.exchange.domain.enums;

/**
 * Type d'une demande d'échange soumise par un utilisateur externe.
 *
 * <p>Catégorise la nature de la demande pour orienter son traitement interne.
 */
public enum ExchangeRequestType {

    /** Demande pour une nouvelle étude clinique. */
    NEW_STUDY("Nouvelle étude"),

    /** Amendement à une étude existante. */
    AMENDMENT("Amendement"),

    /** Extension de durée ou de périmètre d'une étude existante. */
    EXTENSION("Extension"),

    /** Demande d'information sur une étude ou une procédure. */
    INFORMATION("Demande d'information");

    private final String label;

    ExchangeRequestType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de demande.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

package be.clinitrak.exchange.domain.enums;

/**
 * Type d'expéditeur d'un message dans une demande d'échange.
 *
 * <p>Distingue les messages provenant des équipes internes
 * de ceux envoyés par les utilisateurs externes.
 */
public enum SenderType {

    /** Message envoyé par un utilisateur interne (staff hospitalier). */
    INTERNAL("Interne"),

    /** Message envoyé par un utilisateur externe (firme, investigateur). */
    EXTERNAL("Externe");

    private final String label;

    SenderType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type d'expéditeur.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

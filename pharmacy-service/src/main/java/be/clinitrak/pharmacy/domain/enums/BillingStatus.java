package be.clinitrak.pharmacy.domain.enums;

/**
 * Statut d'une facturation pharmaceutique.
 *
 * <p>Suit le cycle de vie d'une facture depuis sa création en brouillon
 * jusqu'à son paiement ou son annulation.
 */
public enum BillingStatus {

    /** Facture en cours de préparation, non encore envoyée. */
    DRAFT("Brouillon"),

    /** Facture envoyée au promoteur ou au service de facturation. */
    SENT("Envoyée"),

    /** Facture payée. */
    PAID("Payée"),

    /** Facture contestée par le destinataire. */
    DISPUTED("Contestée"),

    /** Facture annulée. */
    CANCELLED("Annulée");

    private final String label;

    BillingStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut de facturation.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}

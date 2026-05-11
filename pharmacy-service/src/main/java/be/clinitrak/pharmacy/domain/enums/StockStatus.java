package be.clinitrak.pharmacy.domain.enums;

/**
 * Statut d'une unité de stock de médicament.
 *
 * <p>Représente le cycle de vie d'un lot de médicament depuis sa réception
 * en quarantaine jusqu'à sa destruction ou son retour.
 */
public enum StockStatus {

    /** Stock en quarantaine à la réception, en attente de contrôle qualité. */
    QUARANTINE("Quarantaine"),

    /** Stock libéré, disponible pour la dispensation. */
    AVAILABLE("Disponible"),

    /** Stock dispensé à un patient. */
    DISPENSED("Dispensé"),

    /** Stock retourné par le patient ou renvoyé au fabricant. */
    RETURNED("Retourné"),

    /** Stock détruit selon les procédures réglementaires. */
    DESTROYED("Détruit");

    private final String label;

    StockStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut de stock.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}

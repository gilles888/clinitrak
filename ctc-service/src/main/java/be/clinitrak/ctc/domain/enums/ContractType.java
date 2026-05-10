package be.clinitrak.ctc.domain.enums;

/**
 * Type de contrat financier lié à une étude clinique.
 *
 * <p>Distingue les conventions initiales, les avenants modificatifs
 * et les contrats liés au promoteur externe.
 */
public enum ContractType {

    /** Convention initiale entre l'hôpital et le promoteur/financeur. */
    CONVENTION("Convention"),

    /** Avenant modifiant une convention existante. */
    AMENDMENT("Avenant"),

    /** Contrat spécifique au promoteur externe. */
    SPONSOR("Promoteur");

    private final String label;

    ContractType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type de contrat.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

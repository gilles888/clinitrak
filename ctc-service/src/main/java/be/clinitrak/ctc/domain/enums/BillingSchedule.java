package be.clinitrak.ctc.domain.enums;

/**
 * Périodicité de facturation d'un contrat financier.
 *
 * <p>Définit le rythme selon lequel les factures sont émises
 * dans le cadre d'une convention ou d'un contrat sponsor.
 */
public enum BillingSchedule {

    /** Facturation mensuelle. */
    MONTHLY("Mensuel"),

    /** Facturation trimestrielle. */
    QUARTERLY("Trimestriel"),

    /** Facturation annuelle. */
    ANNUAL("Annuel"),

    /** Facturation par jalons/étapes de l'étude. */
    MILESTONE("Par jalon");

    private final String label;

    BillingSchedule(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de cette périodicité.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

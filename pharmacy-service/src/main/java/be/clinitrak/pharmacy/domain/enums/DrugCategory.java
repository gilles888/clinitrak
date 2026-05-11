package be.clinitrak.pharmacy.domain.enums;

/**
 * Catégorie d'un médicament expérimental selon la classification ICH E6.
 *
 * <p>Distingue les médicaments à l'essai (IMP), les médicaments de référence ou
 * de soutien (NIMP) et les placebos utilisés dans les essais cliniques en aveugle.
 */
public enum DrugCategory {

    /** Médicament expérimental (Investigational Medicinal Product). */
    IMP("Médicament expérimental"),

    /** Médicament non-expérimental (Non-Investigational Medicinal Product). */
    NIMP("Non-IMP"),

    /** Placebo utilisé dans un essai en aveugle. */
    PLACEBO("Placebo");

    private final String label;

    DrugCategory(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de la catégorie.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}

package be.clinitrak.pharmacy.domain.enums;

/**
 * Forme pharmaceutique d'un médicament expérimental.
 *
 * <p>Détermine le mode d'administration et les contraintes de stockage
 * et de dispensation associées.
 */
public enum DrugForm {

    /** Comprimé à avaler ou à croquer. */
    TABLET("Comprimé"),

    /** Gélule à avaler. */
    CAPSULE("Gélule"),

    /** Forme injectable (IV, IM, SC). */
    INJECTION("Injectable"),

    /** Solution buvable ou pour perfusion. */
    SOLUTION("Solution"),

    /** Crème ou pommade à application cutanée. */
    CREAM("Crème"),

    /** Autre forme pharmaceutique non répertoriée. */
    OTHER("Autre");

    private final String label;

    DrugForm(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de la forme pharmaceutique.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}

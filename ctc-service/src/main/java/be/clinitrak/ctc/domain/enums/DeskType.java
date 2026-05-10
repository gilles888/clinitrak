package be.clinitrak.ctc.domain.enums;

/**
 * Type de desk du Centre de Thérapie Cellulaire.
 *
 * <p>Distingue les études académiques (initiées par des chercheurs hospitaliers)
 * des études commerciales (initiées par des promoteurs industriels).
 */
public enum DeskType {

    /** Étude initiée dans un contexte académique ou hospitalier. */
    ACADEMIC("Académique"),

    /** Étude initiée par un promoteur commercial (industrie pharmaceutique). */
    COMMERCIAL("Commercial");

    private final String label;

    DeskType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type de desk.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

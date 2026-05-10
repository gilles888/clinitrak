package be.clinitrak.ctc.domain.enums;

/**
 * Devise monétaire utilisée dans les contrats financiers.
 *
 * <p>Regroupe les devises les plus fréquentes pour les études cliniques
 * réalisées en Europe et avec des promoteurs internationaux.
 */
public enum Currency {

    /** Euro (zone euro). */
    EUR("Euro"),

    /** Dollar américain. */
    USD("Dollar US"),

    /** Livre sterling britannique. */
    GBP("Livre sterling");

    private final String label;

    Currency(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de cette devise.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

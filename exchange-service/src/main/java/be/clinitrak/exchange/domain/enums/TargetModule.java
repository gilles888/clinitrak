package be.clinitrak.exchange.domain.enums;

/**
 * Module cible d'une demande d'échange.
 *
 * <p>Indique vers quel service interne la demande externe est destinée.
 */
public enum TargetModule {

    /** Comité d'Éthique. */
    CE("Comité d'Éthique"),

    /** Centre de Thérapie Cellulaire. */
    CTC("Centre de Thérapie Cellulaire");

    private final String label;

    TargetModule(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du module cible.
     *
     * @return libellé du module
     */
    public String getLabel() {
        return label;
    }
}

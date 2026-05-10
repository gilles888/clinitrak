package be.clinitrak.ctc.domain.enums;

/**
 * Priorité d'une demande CTC.
 *
 * <p>Permet de prioriser le traitement des demandes desk selon leur urgence.
 */
public enum Priority {

    /** Priorité basse — peut attendre. */
    LOW("Basse"),

    /** Priorité normale (défaut). */
    MEDIUM("Moyenne"),

    /** Priorité haute — à traiter rapidement. */
    HIGH("Haute"),

    /** Urgence — traitement immédiat requis. */
    URGENT("Urgent");

    private final String label;

    Priority(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de cette priorité.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

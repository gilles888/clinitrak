package be.clinitrak.ctc.domain.enums;

/**
 * Sévérité d'un événement qualité.
 *
 * <p>Permet de prioriser les actions correctives selon l'impact
 * de l'événement sur la qualité et la sécurité de l'étude.
 */
public enum Severity {

    /** Impact faible sur la qualité, sans conséquence significative. */
    LOW("Faible"),

    /** Impact modéré nécessitant une correction planifiée. */
    MEDIUM("Modérée"),

    /** Impact élevé nécessitant une action rapide. */
    HIGH("Élevée"),

    /** Impact critique mettant en jeu la sécurité ou l'intégrité des données. */
    CRITICAL("Critique");

    private final String label;

    Severity(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de cette sévérité.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

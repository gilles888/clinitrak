package be.clinitrak.ctc.domain.enums;

/**
 * Type d'analyse statistique demandée pour une étude clinique.
 *
 * <p>Chaque type correspond à une méthodologie statistique
 * adaptée aux objectifs de l'étude.
 */
public enum AnalysisType {

    /** Analyse descriptive (statistiques de base : moyennes, médianes, distributions). */
    DESCRIPTIVE("Descriptive"),

    /** Analyse inférentielle (tests statistiques, intervalles de confiance). */
    INFERENTIAL("Inférentielle"),

    /** Analyse de survie (Kaplan-Meier, Cox). */
    SURVIVAL("Survie"),

    /** Analyse longitudinale (données répétées dans le temps). */
    LONGITUDINAL("Longitudinale");

    private final String label;

    AnalysisType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce type d'analyse.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

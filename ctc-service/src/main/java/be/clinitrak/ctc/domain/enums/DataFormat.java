package be.clinitrak.ctc.domain.enums;

/**
 * Format de sortie des données statistiques.
 *
 * <p>Spécifie le format dans lequel les résultats de l'analyse
 * statistique doivent être livrés au demandeur.
 */
public enum DataFormat {

    /** Format PDF (rapport de résultats). */
    PDF("PDF"),

    /** Format Microsoft Excel. */
    EXCEL("Excel"),

    /** Format SAS (Statistical Analysis System). */
    SAS("SAS"),

    /** Format SPSS (Statistical Package for the Social Sciences). */
    SPSS("SPSS"),

    /** Format R (langage statistique open-source). */
    R_FORMAT("R");

    private final String label;

    DataFormat(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français de ce format de données.
     *
     * @return libellé lisible en français
     */
    public String getLabel() {
        return label;
    }
}

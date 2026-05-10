package be.clinitrak.ethics.domain.enums;

/**
 * Type de modèle de correspondance utilisé par le Comité d'Éthique.
 *
 * <p>Chaque type correspond à un modèle Thymeleaf stocké en base de données.
 */
public enum TemplateType {

    /** Lettre d'approbation de protocole. */
    APPROVAL_LETTER("Lettre d'approbation"),

    /** Lettre de refus de protocole. */
    REJECTION_LETTER("Lettre de refus"),

    /** Demande d'informations complémentaires. */
    MORE_INFO_LETTER("Demande d'informations complémentaires"),

    /** Demande de soumission du rapport annuel. */
    ANNUAL_REPORT_REQUEST("Demande de rapport annuel"),

    /** Rappel générique (relance, échéance). */
    REMINDER("Rappel"),

    /** Convocation à une réunion du Comité d'Éthique. */
    MEETING_NOTICE("Convocation réunion");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    TemplateType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de template.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

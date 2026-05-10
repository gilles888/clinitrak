package be.clinitrak.ethics.domain.enums;

/**
 * Type de réunion du Comité d'Éthique.
 */
public enum MeetingType {

    /** Réunion ordinaire planifiée dans le calendrier annuel. */
    ORDINARY("Ordinaire"),

    /** Réunion extraordinaire convoquée en urgence. */
    EXTRAORDINARY("Extraordinaire");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    MeetingType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type de réunion.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

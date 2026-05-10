package be.clinitrak.ethics.domain.enums;

/**
 * Type d'avis soumis au Comité d'Éthique.
 *
 * <p>Chaque type correspond à une catégorie de soumission dans le workflow CE.
 */
public enum ReviewType {

    /** Soumission initiale d'un nouveau protocole. */
    INITIAL("Initial"),

    /** Modification substantielle d'un protocole approuvé. */
    AMENDMENT("Amendement"),

    /** Rapport annuel de suivi de l'étude. */
    ANNUAL_REPORT("Rapport annuel"),

    /** Notification d'une déviation au protocole. */
    DEVIATION("Déviation"),

    /** Notification d'un événement indésirable grave. */
    SAE("Événement indésirable grave");

    private final String label;

    /**
     * Crée une valeur d'enum avec son libellé français.
     *
     * @param label libellé lisible en français
     */
    ReviewType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type d'avis.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

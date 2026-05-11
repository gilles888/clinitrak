package be.clinitrak.pharmacy.domain.enums;

/**
 * Types d'alertes pharmacie générées automatiquement par le service d'alerte.
 *
 * <p>Chaque type d'alerte correspond à un événement nécessitant une action
 * du pharmacien responsable de l'étude.
 */
public enum AlertType {

    /** Stock d'un médicament en dessous du seuil minimal configuré. */
    LOW_STOCK("Stock faible"),

    /** Médicament expirant dans moins de 30 jours. */
    EXPIRY_30("Péremption J-30"),

    /** Médicament expirant dans moins de 7 jours — critique. */
    EXPIRY_7("Péremption J-7"),

    /** Stock placé en quarantaine suite à un problème qualité. */
    QUARANTINE("Quarantaine");

    private final String label;

    AlertType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type d'alerte.
     *
     * @return libellé français
     */
    public String getLabel() {
        return label;
    }
}

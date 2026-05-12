package be.clinitrak.admin.domain.enums;

/**
 * Module fonctionnel disponible dans la plateforme CliniTrak.
 *
 * <p>Chaque tenant peut activer un sous-ensemble de modules selon son abonnement.
 */
public enum ModuleType {

    /** Module de gestion des études cliniques. */
    STUDIES("Études"),

    /** Module du Comité d'Éthique. */
    ETHICS("Éthique"),

    /** Module du Centre de Thérapie Cellulaire. */
    CTC("CTC"),

    /** Module de gestion de la pharmacie. */
    PHARMACY("Pharmacie"),

    /** Module de portail d'échanges externes. */
    EXCHANGE("Exchange"),

    /** Module de facturation. */
    BILLING("Facturation"),

    /** Module de gestion électronique de documents. */
    DOCUMENTS("Documents");

    private final String label;

    ModuleType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du module.
     *
     * @return libellé du module
     */
    public String getLabel() {
        return label;
    }
}

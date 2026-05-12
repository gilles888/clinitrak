package be.clinitrak.admin.domain.enums;

/**
 * Type d'abonnement d'un tenant.
 *
 * <p>Détermine les fonctionnalités disponibles et les limites du tenant.
 */
public enum SubscriptionType {

    /** Abonnement basique : fonctionnalités limitées. */
    BASIC("Basique"),

    /** Abonnement professionnel : fonctionnalités étendues. */
    PROFESSIONAL("Professionnel"),

    /** Abonnement entreprise : toutes les fonctionnalités, limites élevées. */
    ENTERPRISE("Entreprise");

    private final String label;

    SubscriptionType(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du type d'abonnement.
     *
     * @return libellé du type
     */
    public String getLabel() {
        return label;
    }
}

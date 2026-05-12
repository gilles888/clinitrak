package be.clinitrak.admin.domain.enums;

/**
 * Statut d'un tenant dans la plateforme CliniTrak.
 *
 * <p>Contrôle l'accès des utilisateurs du tenant à la plateforme.
 */
public enum TenantStatus {

    /** Tenant actif : tous les accès sont autorisés. */
    ACTIVE("Actif"),

    /** Tenant inactif : accès désactivés (ex: fin de contrat). */
    INACTIVE("Inactif"),

    /** Tenant suspendu : accès temporairement bloqués (ex: non-paiement). */
    SUSPENDED("Suspendu");

    private final String label;

    TenantStatus(String label) {
        this.label = label;
    }

    /**
     * Retourne le libellé français du statut.
     *
     * @return libellé du statut
     */
    public String getLabel() {
        return label;
    }
}

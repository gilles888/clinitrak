package be.clinitrak.auth.tenant;

/**
 * Contexte de tenant stocké dans un {@link ThreadLocal}.
 *
 * <p>Ce pattern permet à n'importe quelle couche de l'application (service, repository, aspect)
 * d'accéder au tenant courant sans le passer explicitement en paramètre.
 *
 * <p><strong>Important :</strong> Le thread local DOIT être nettoyé en fin de requête
 * (dans le finally du {@link JwtAuthenticationFilter} ou du {@link TenantFilter}).
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {}

    /**
     * Positionne le tenant courant pour ce thread.
     *
     * @param tenantId identifiant du tenant (UUID sous forme de String)
     */
    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    /**
     * Retourne le tenant courant pour ce thread.
     *
     * @return identifiant du tenant ou null si aucun tenant n'est positionné
     */
    public static String getTenantId() {
        return CURRENT_TENANT.get();
    }

    /**
     * Indique si un tenant est positionné pour ce thread.
     *
     * @return true si un tenant est présent
     */
    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    /**
     * Supprime le tenant du ThreadLocal pour éviter les fuites mémoire.
     *
     * <p>Doit être appelé dans un bloc {@code finally} après traitement de la requête.
     */
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

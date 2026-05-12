package be.clinitrak.admin.tenant;

/**
 * Contexte de tenant thread-local pour le admin-service.
 *
 * <p>Stocke l'identifiant du tenant courant pendant la durée d'une requête HTTP.
 * Initialisé par {@code JwtAuthenticationFilter} après validation du JWT.
 *
 * <p><strong>Important :</strong> le contexte doit être nettoyé après chaque requête
 * pour éviter les fuites entre threads (notamment avec les pools de threads).
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
        // Classe utilitaire — pas d'instanciation
    }

    /**
     * Positionne l'identifiant du tenant courant pour ce thread.
     *
     * @param tenantId identifiant du tenant (slug ou UUID string)
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * Retourne l'identifiant du tenant courant pour ce thread.
     *
     * @return identifiant du tenant, ou {@code null} si non défini
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * Nettoie le contexte tenant pour ce thread.
     *
     * <p>Doit être appelé dans un bloc {@code finally} à la fin de chaque requête.
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}

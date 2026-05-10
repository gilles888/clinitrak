# CliniTrak — Stratégie Multi-Tenant

*Dernière mise à jour : 2026-05-09 (Session 3)*

## Vue d'ensemble

CliniTrak implémente le multi-tenant par **Row-Level Security applicative** :
toutes les bases de données sont partagées, mais chaque ligne porte un `tenant_id` (UUID)
qui est systématiquement utilisé dans tous les prédicats SQL.

Ce choix (shared database, shared schema) offre un bon compromis entre :
- Isolation des données (filtrée au niveau applicatif + index sur `tenant_id`)
- Coût opérationnel réduit (une seule instance de chaque service)
- Scalabilité horizontale possible si nécessaire

---

## Résolution du tenant

Le tenant est identifié à chaque requête via deux mécanismes, par ordre de priorité :

### 1. Header `X-Tenant-ID` (priorité 1)

Format : **slug** du tenant (ex: `"saintluc"`)

```
GET /api/v1/studies HTTP/1.1
Authorization: Bearer eyJhbGciOi...
X-Tenant-ID: saintluc
```

Utilisé principalement :
- Par les appels inter-services via Feign
- Par le frontend Angular (via `tenantInterceptor`)
- En développement / tests API (Postman, curl)

### 2. Sous-domaine (priorité 2)

Format : `{slug}.clinitrak.be` → extrait le slug depuis le hostname

```
Host: saintluc.clinitrak.be → tenantId = "saintluc"
Host: hopital-erasme.clinitrak.be → tenantId = "hopital-erasme"
```

Le `TenantResolver` extrait le premier segment du domaine avant `.clinitrak.be`.

---

## TenantContext (ThreadLocal)

Chaque thread de traitement de requête dispose de son propre contexte tenant,
géré via `ThreadLocal`. Il est initialisé dans le filtre et nettoyé dans le `finally`.

```java
// Initialisation dans TenantFilter (avant traitement de la requête)
TenantContext.setTenantId("uuid-du-tenant");

// Utilisation dans tous les services
String tenantId = TenantContext.getTenantId();

// Nettoyage garanti dans le finally du filter
TenantContext.clear();
```

### Implémentation

```java
public class TenantContext {
    private static final ThreadLocal<String> TENANT_ID = new ThreadLocal<>();

    public static void setTenantId(String tenantId) { TENANT_ID.set(tenantId); }
    public static String getTenantId() { return TENANT_ID.get(); }
    public static void clear() { TENANT_ID.remove(); }
}
```

**Important** : `TENANT_ID.remove()` (pas `set(null)`) pour éviter les memory leaks
dans les pools de threads.

---

## Pattern dans les repositories

Toutes les méthodes de repository filtrent systématiquement sur `tenantId` ET `deleted = false` :

```java
// Recherche par ID — vérifie appartenance au tenant
Optional<EthicsReview> findByIdAndTenantIdAndDeletedFalse(UUID id, UUID tenantId);

// Liste filtrée par tenant
List<EthicsReview> findByTenantIdAndDeletedFalseOrderBySubmissionDateDesc(UUID tenantId);

// Recherche multicritères avec Specification
Page<ClinicalStudy> findAll(Specification<ClinicalStudy> spec, Pageable pageable);
// → le Specification inclut toujours un prédicat tenantId = :tenantId
```

**Règle absolue** : aucune requête sans filtre `tenantId`. Tout accès cross-tenant
doit être explicitement autorisé via `ROLE_SUPER_ADMIN`.

---

## Propagation via Feign

Le `FeignConfig` propage automatiquement les headers de sécurité aux appels inter-services :

```java
@Configuration
public class FeignConfig {
    @Bean
    public RequestInterceptor tenantPropagationInterceptor() {
        return template -> {
            // Propagation du JWT Bearer
            HttpServletRequest request = getCurrentRequest();
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authHeader != null) {
                template.header(HttpHeaders.AUTHORIZATION, authHeader);
            }
            // Propagation du X-Tenant-ID depuis TenantContext
            String tenantId = TenantContext.getTenantId();
            if (tenantId != null) {
                template.header("X-Tenant-ID", tenantId);
            }
        };
    }
}
```

---

## Tenants de référence

| Slug | Nom | Domaine | ID (UUID) |
|------|-----|---------|-----------|
| `saintluc` | Cliniques Universitaires Saint-Luc | `saintluc.clinitrak.be` | `00000000-0000-0000-0000-000000000001` |

---

## Ajout d'un nouveau tenant

1. **Base de données** (auth-service DB) :
   ```sql
   INSERT INTO tenants (id, slug, name, domain, active)
   VALUES (gen_random_uuid(), 'nouveau-hopital', 'Hôpital XYZ', 'xhopital.clinitrak.be', true);
   ```

2. **Rôles tenant-spécifiques** (si besoin) :
   ```sql
   INSERT INTO roles (id, name, tenant_id)
   VALUES (gen_random_uuid(), 'ROLE_LOCAL_ADMIN', '<tenant-uuid>');
   ```

3. **DNS** : Créer l'enregistrement CNAME `nouveau-hopital.clinitrak.be → clinitrak.be`

4. **Flush du cache Redis** :
   ```bash
   redis-cli DEL "tenants::all"
   # Ou via l'admin-service (endpoint /api/v1/admin/cache/evict)
   ```
   Le cache `tenants` a un TTL de **1 heure** (`CacheConfig` dans auth-service).

5. **Créer le premier utilisateur** via `POST /api/v1/auth/register` avec le slug du nouveau tenant.

---

## Considérations de sécurité

- Le `tenantId` dans le JWT payload est utilisé pour double-vérification.
  Si le `X-Tenant-ID` du header ne correspond pas au `tenantId` du JWT, la requête est rejetée.
- `ROLE_SUPER_ADMIN` peut accéder à tous les tenants (cross-tenant queries).
- Les APIs admin (`/api/v1/admin/**`) requièrent `ROLE_SUPER_ADMIN` ou `ROLE_ADMIN_TENANT`.
- Les soft-deletes (`deleted = true`) sont toujours filtrés, même pour les SUPER_ADMIN.

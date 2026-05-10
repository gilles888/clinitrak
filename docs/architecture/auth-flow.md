# CliniTrak — Flux d'authentification

*Dernière mise à jour : 2026-05-09 (Session 3)*

## Séquence Login

```
Client                    auth-service                    PostgreSQL          Redis
  │                           │                               │                │
  │  POST /api/v1/auth/login  │                               │                │
  │  X-Tenant-ID: saintluc    │                               │                │
  │  { email, password }      │                               │                │
  │──────────────────────────>│                               │                │
  │                           │── SELECT tenant WHERE slug    │                │
  │                           │──────────────────────────────>│                │
  │                           │<── tenant (ou 404)            │                │
  │                           │                               │                │
  │                           │── SELECT user WHERE email     │                │
  │                           │   AND tenant_id               │                │
  │                           │──────────────────────────────>│                │
  │                           │<── user (ou 401)              │                │
  │                           │                               │                │
  │                           │── BCrypt.verify(password)     │                │
  │                           │   [si échec → failed_login_attempts++]        │
  │                           │                               │                │
  │                           │── INSERT refresh_token        │                │
  │                           │──────────────────────────────>│                │
  │                           │                               │                │
  │                           │── CACHE user (15min)          │                │
  │                           │──────────────────────────────────────────────>│
  │                           │                               │                │
  │<── 200 { accessToken,     │                               │                │
  │         refreshToken }    │                               │                │
```

**Endpoint** : `POST /api/v1/auth/login`
**Body** : `{ "email": "user@saintluc.be", "password": "...", "tenantSlug": "saintluc" }`
(ou via header `X-Tenant-ID: saintluc`)

---

## Utilisation du JWT

### Headers requis sur chaque requête protégée

```http
Authorization: Bearer <accessToken>
X-Tenant-ID: saintluc
```

### Contenu du JWT payload

```json
{
  "sub": "user@saintluc.be",
  "userId": "uuid-de-l-utilisateur",
  "tenantId": "uuid-du-tenant",
  "tenantSlug": "saintluc",
  "roles": ["ROLE_INVESTIGATOR"],
  "iat": 1746784200,
  "exp": 1746785100
}
```

### Validation JWT (dans chaque microservice)

Chaque microservice valide le JWT **localement** via `JwtAuthenticationFilter` :
1. Extraire le token du header `Authorization: Bearer ...`
2. Vérifier la signature (HMAC-SHA256 avec le secret partagé)
3. Vérifier l'expiration (`exp`)
4. Vérifier que le token n'est pas dans la blacklist Redis (`blacklisted-tokens`)
5. Initialiser le `SecurityContext` + `TenantContext`

**Pas d'appel réseau** vers auth-service pour chaque requête (performance).

---

## Refresh Token

### Caractéristiques

| Propriété | Valeur |
|-----------|--------|
| Durée de vie | **7 jours** |
| Rotation | Automatique : nouveau pair de tokens à chaque refresh |
| Stockage | Table `refresh_tokens` en base |
| Format | UUID v4 opaque (pas un JWT) |
| Révocation | Immédiate au logout |

### Séquence Refresh

```
Client                    auth-service
  │                           │
  │  POST /api/v1/auth/refresh │
  │  { "refreshToken": "..." } │
  │──────────────────────────>│
  │                           │── Vérifier token en base (non révoqué, non expiré)
  │                           │── Révoquer l'ancien token (revoked = true)
  │                           │── Créer nouveau pair (accessToken + refreshToken)
  │                           │── INSERT nouveau refresh_token en base
  │<── 200 { accessToken,     │
  │         refreshToken }    │
```

**Endpoint** : `POST /api/v1/auth/refresh`

---

## Logout

```
Client                    auth-service                    Redis
  │                           │                             │
  │  POST /api/v1/auth/logout  │                             │
  │  Authorization: Bearer ... │                             │
  │──────────────────────────>│                             │
  │                           │── Révoquer refresh_token    │
  │                           │── Blacklister le JWT        │
  │                           │   (TTL = durée restante)    │
  │                           │────────────────────────────>│
  │<── 204 No Content         │                             │
```

**Endpoint** : `POST /api/v1/auth/logout`
**Headers** : `Authorization: Bearer <accessToken>` + `X-Tenant-ID: <slug>`

---

## Propagation inter-services (Feign)

Le `FeignConfig` transmet automatiquement depuis la requête HTTP courante :

```java
// Ce que FeignConfig injecte dans chaque appel Feign sortant :
Authorization: Bearer <token>   // depuis RequestContextHolder
X-Tenant-ID: <tenantSlug>       // depuis TenantContext (ThreadLocal)
```

Ainsi, quand `ethics-service` appelle `study-service` via Feign,
le JWT et le tenant sont transparents pour study-service.

---

## Lockout (protection brute force)

| Paramètre | Valeur |
|-----------|--------|
| Tentatives avant lockout | **5** |
| Durée du lockout | **30 minutes** |
| Compteur | `users.failed_login_attempts` |
| Expiration | `users.locked_until` |

Après 5 tentatives échouées :
1. `account_locked = true`, `locked_until = NOW() + 30min`
2. Les tentatives suivantes retournent immédiatement `401` sans vérifier le mot de passe
3. Après expiration, `failed_login_attempts` est remis à 0 au prochain login réussi

**Déverrouillage manuel** : via `POST /api/v1/admin/users/{id}/unlock` (ROLE_ADMIN_TENANT)

---

## Rôles RBAC disponibles

| Rôle | Description | Niveau |
|------|-------------|--------|
| `ROLE_SUPER_ADMIN` | Administration globale (tous tenants) | Global |
| `ROLE_ADMIN_TENANT` | Administration d'un tenant | Tenant |
| `ROLE_CE_COORDINATOR` | Coordinateur Comité d'Éthique | Tenant |
| `ROLE_CE_SECRETARY` | Secrétaire CE | Tenant |
| `ROLE_CE_MEMBER` | Membre CE (lecture) | Tenant |
| `ROLE_INVESTIGATOR` | Investigateur clinique | Tenant |
| `ROLE_RESEARCH_NURSE` | Infirmière de recherche | Tenant |
| `ROLE_PHARMACIST` | Pharmacien | Tenant |
| `ROLE_CTC_OPERATOR` | Opérateur CTC | Tenant |
| `ROLE_DATA_MANAGER` | Gestionnaire de données | Tenant |
| `ROLE_EXTERNAL` | Accès externe restreint | Tenant |

---

## Considérations de sécurité

- **BCrypt force 12** pour le hachage des mots de passe (résistant aux GPU)
- **Jamais** de données sensibles dans les logs (mots de passe, tokens)
- Les `AuditLog` tracent toutes les tentatives de login (succès et échecs)
- Le secret JWT doit faire au moins 32 bytes (256 bits). Utiliser `openssl rand -base64 64`
- En production, utiliser RS256 (asymétrique) à la place de HS256

# Admin Service API

**Port** : 8091
**Base URL** : `/api/v1/admin`
**Auth** : Bearer JWT interne (généré par `auth-service`)
**Tenant** : Header `X-Tenant-ID` non requis (opère sur tous les tenants, vision super-admin)

## Contexte fonctionnel

Le service Admin est l'interface d'administration globale de la plateforme CliniTrak.
Il est réservé exclusivement aux `ROLE_SUPER_ADMIN` et permet de gérer les tenants
(hôpitaux/institutions), inviter des utilisateurs, consulter les logs d'audit cross-tenant,
et surveiller la santé du système.

---

## Enums de référence

| Enum | Valeurs |
|------|---------|
| `TenantStatus` | `ACTIVE`, `INACTIVE`, `SUSPENDED` |
| `SubscriptionType` | `BASIC`, `PROFESSIONAL`, `ENTERPRISE` |
| `ModuleType` | `STUDIES`, `ETHICS`, `CTC`, `PHARMACY`, `EXCHANGE`, `BILLING`, `DOCUMENTS` |

---

## Endpoints — Gestion des tenants

### POST /api/v1/admin/tenants

**Description** : Crée un nouveau tenant (institution hospitalière). Provisionne automatiquement
les modules actifs selon le type de souscription.
**Accès** : `ROLE_SUPER_ADMIN`
**Request Body** :
```json
{
  "name": "string",
  "slug": "string (unique, format kebab-case)",
  "domain": "string (optionnel, ex: saint-luc.be)",
  "activeModules": ["STUDIES", "ETHICS", "CTC"],
  "subscriptionType": "BASIC | PROFESSIONAL | ENTERPRISE"
}
```
**Response 201** :
```json
{
  "id": "uuid",
  "name": "string",
  "slug": "string",
  "domain": "string | null",
  "status": "ACTIVE",
  "activeModules": ["STUDIES", "ETHICS", "CTC"],
  "subscriptionType": "PROFESSIONAL",
  "configuration": {
    "ceNumberFormat": "YYYY/NNNN",
    "timezone": "Europe/Brussels",
    "defaultLanguage": "fr",
    "maxUsers": 50,
    "storageQuotaGb": 100
  },
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```
**Erreurs** :
- `400` : Validation échouée (slug invalide, modules inexistants)
- `409` : Slug ou domaine déjà utilisé

---

### GET /api/v1/admin/tenants

**Description** : Liste tous les tenants de la plateforme.
**Accès** : `ROLE_SUPER_ADMIN`
**Response 200** : `TenantResponse[]` (tous statuts)

---

### PUT /api/v1/admin/tenants/{id}/configuration

**Description** : Met à jour la configuration d'un tenant (fuseau horaire, quotas, langue,
format de numérotation CE, etc.).
**Accès** : `ROLE_SUPER_ADMIN`
**Path Params** :
- `id` : UUID du tenant

**Request Body** :
```json
{
  "ceNumberFormat": "string (optionnel, ex: YYYY/NNNN)",
  "timezone": "string (optionnel, ex: Europe/Brussels)",
  "defaultLanguage": "string (optionnel, ex: fr)",
  "maxUsers": "integer (optionnel)",
  "storageQuotaGb": "integer (optionnel)"
}
```
**Response 200** : `TenantResponse` avec la configuration mise à jour
**Erreurs** :
- `400` : Valeurs de configuration invalides
- `404` : Tenant introuvable

---

### GET /api/v1/admin/tenants/{id}/statistics

**Description** : Retourne les statistiques d'utilisation d'un tenant (utilisateurs, études,
modules actifs).
**Accès** : `ROLE_SUPER_ADMIN`
**Response 200** :
```json
{
  "tenantId": "uuid",
  "tenantName": "string",
  "userCount": 42,
  "studyCount": 15,
  "activeStudies": 8,
  "moduleUsage": {
    "STUDIES": true,
    "ETHICS": true,
    "CTC": false,
    "PHARMACY": true
  }
}
```
**Erreurs** :
- `404` : Tenant introuvable

---

## Endpoints — Gestion des utilisateurs

### POST /api/v1/admin/tenants/{id}/users/invite

**Description** : Invite un utilisateur dans un tenant. Génère un lien d'invitation envoyé
par email (via MailHog en développement). L'utilisateur doit finaliser son inscription via
le lien reçu.
**Accès** : `ROLE_SUPER_ADMIN`
**Path Params** :
- `id` : UUID du tenant cible

**Request Body** :
```json
{
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "ROLE_STUDY_COORDINATOR | ROLE_CE_MEMBER | ROLE_CTC_MANAGER | ..."
}
```
**Response 200** :
```json
{
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "role": "string",
  "status": "INVITATION_SENT"
}
```
**Erreurs** :
- `400` : Email invalide ou rôle inexistant
- `404` : Tenant introuvable
- `409` : Email déjà enregistré dans ce tenant

---

## Endpoints — Audit logs

### GET /api/v1/admin/audit-logs

**Description** : Consulte les logs d'audit cross-tenant avec filtrage multicritères.
Les logs sont générés par l'`AuditAspect` de chaque service.
**Accès** : `ROLE_SUPER_ADMIN`
**Query Params** :

| Paramètre | Type | Optionnel | Description |
|-----------|------|-----------|-------------|
| `tenantId` | UUID | oui | Filtrer par tenant |
| `userId` | UUID | oui | Filtrer par utilisateur |
| `action` | string | oui | Filtrer par action (ex: `POST /api/v1/studies`) |
| `from` | ISO-8601 | oui | Date de début |
| `to` | ISO-8601 | oui | Date de fin |
| `page` | integer | oui | Numéro de page (défaut: 0) |
| `size` | integer | oui | Taille de page (défaut: 50) |

**Response 200** :
```json
[
  {
    "id": "uuid",
    "tenantId": "uuid",
    "userId": "uuid",
    "userEmail": "string",
    "action": "string",
    "resourceType": "string",
    "resourceId": "string | null",
    "httpMethod": "GET | POST | PUT | PATCH | DELETE",
    "statusCode": 200,
    "ipAddress": "string",
    "userAgent": "string",
    "durationMs": 42,
    "createdAt": "ISO-8601"
  }
]
```

---

### GET /api/v1/admin/audit-logs/export

**Description** : Exporte les logs d'audit au format Excel (XLSX). Supporte les mêmes
paramètres de filtrage que l'endpoint GET. Le fichier est généré via Apache POI 5.2.5.
**Accès** : `ROLE_SUPER_ADMIN`
**Query Params** : identiques à `GET /api/v1/admin/audit-logs`
**Response 200** :
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Content-Disposition: `attachment; filename="audit-logs-YYYY-MM-DD.xlsx"`

---

## Endpoints — Système

### GET /api/v1/admin/system/health

**Description** : Retourne l'état de santé global du système en interrogeant les
endpoints `/actuator/health` de chaque micro-service.
**Accès** : Public (ou `ROLE_SUPER_ADMIN`)
**Response 200** :
```json
{
  "status": "UP | DEGRADED | DOWN",
  "services": {
    "auth-service": "UP",
    "study-service": "UP",
    "ethics-service": "DOWN",
    "ctc-service": "UP",
    "pharmacy-service": "UP",
    "exchange-service": "UP"
  },
  "checkedAt": "ISO-8601"
}
```

---

## Variables d'environnement requises

| Variable | Description | Défaut dev |
|----------|-------------|------------|
| `ADMIN_DB_NAME` | Nom de la base PostgreSQL | `clinitrak_admin` |
| `JWT_SECRET` | Clé JWT interne partagée (min 256 bits) | `changeme-clinitrak-...` |
| `MAIL_HOST` | Serveur SMTP (invitations) | `mailhog` |
| `MAIL_PORT` | Port SMTP | `1025` |

---

## Notes d'architecture

- Le service Admin n'implémente pas de multi-tenancy au sens strict : il voit **tous** les
  tenants et n'utilise pas `TenantContext`. C'est une exception volontaire à la règle
  générale du projet.
- Les `SystemAuditLog` sont en **insertion seule** (pas de UPDATE, pas de DELETE) pour
  garantir l'intégrité de la piste d'audit.
- L'export Excel est généré à la volée en mémoire (streaming POI) sans stockage sur disque.
- Le `SystemHealthService` effectue des appels HTTP synchrones vers les autres services ;
  un timeout de 5 secondes par service est appliqué pour éviter les blocages.

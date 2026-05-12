# CliniTrak — Onboarding d'un nouvel hôpital (tenant)

Ce guide décrit la procédure complète pour intégrer une nouvelle institution hospitalière dans CliniTrak.

## Prérequis

- admin-service opérationnel (port 8091)
- auth-service opérationnel (port 8081)
- Token JWT d'un compte `ROLE_SUPER_ADMIN`

## Étape 1 : Création du tenant via admin-service

### Appel API

```http
POST /api/v1/admin/tenants
Authorization: Bearer <super-admin-token>
Content-Type: application/json

{
  "name": "Cliniques Universitaires Saint-Luc",
  "slug": "saint-luc",
  "domain": "saint-luc.clinitrak.be",
  "subscriptionType": "FULL",
  "activeModules": [
    "STUDIES", "ETHICS", "CTC", "PHARMACY", "EXCHANGE", "DOCUMENTS", "BILLING"
  ],
  "contactEmail": "it-recherche@saintluc.be",
  "settings": {
    "timezone": "Europe/Brussels",
    "locale": "fr-BE",
    "maxUsers": 150
  }
}
```

### Valeurs du champ `subscriptionType`

| Valeur | Description |
|--------|-------------|
| `TRIAL` | Accès limité 30 jours, modules réduits |
| `STANDARD` | Modules de base (études, éthique) |
| `FULL` | Tous les modules actifs |
| `ENTERPRISE` | Full + SLA garanti + support dédié |

### Valeurs du champ `activeModules`

`STUDIES`, `ETHICS`, `CTC`, `PHARMACY`, `EXCHANGE`, `DOCUMENTS`, `BILLING`, `BATCH`, `NOTIFICATIONS`

## Étape 2 : Configuration des modules actifs

Chaque module peut être activé ou désactivé individuellement après création :

```http
PATCH /api/v1/admin/tenants/{tenantId}/modules
Authorization: Bearer <super-admin-token>
Content-Type: application/json

{
  "activeModules": ["STUDIES", "ETHICS", "PHARMACY"]
}
```

Les modules désactivés retournent `403 Forbidden` pour tous les utilisateurs de ce tenant, quel que soit leur rôle.

## Étape 3 : Invitation du premier utilisateur admin

L'administrateur de l'institution (futur `ROLE_ADMIN`) doit être invité. Il recevra un email avec un lien d'activation.

```http
POST /api/v1/admin/users/invite
Authorization: Bearer <super-admin-token>
X-Tenant-ID: saint-luc
Content-Type: application/json

{
  "email": "admin.recherche@saintluc.be",
  "firstName": "Marie",
  "lastName": "Dupont",
  "role": "ROLE_ADMIN",
  "tenantId": "<uuid-du-tenant-créé>"
}
```

L'utilisateur invité reçoit un email (via notification-service ou directement via Spring Mail) contenant :
- Un lien d'activation valable 48 heures
- Les instructions de première connexion
- L'URL de l'application pour son tenant

## Étape 4 : Configuration SMTP spécifique au tenant

Chaque tenant peut avoir sa propre configuration email (expéditeur, SMTP relay, etc.) stockée dans le champ `settings` JSONB du tenant.

```http
PATCH /api/v1/admin/tenants/{tenantId}/settings
Authorization: Bearer <super-admin-token>
Content-Type: application/json

{
  "settings": {
    "smtp": {
      "fromAddress": "noreply@saintluc.be",
      "fromName": "CliniTrak - Saint-Luc",
      "replyTo": "recherche@saintluc.be"
    },
    "branding": {
      "logoUrl": "https://assets.saintluc.be/logo.png",
      "primaryColor": "#003087"
    }
  }
}
```

Si aucune configuration SMTP tenant n'est définie, le service de notification utilise la configuration globale de l'environnement (`MAIL_HOST`, `MAIL_PORT`, etc.).

## Étape 5 : Vérification du bon fonctionnement

### Test de connexion tenant

```bash
# Tenter un login avec le compte admin invité
curl -X POST http://localhost:8081/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: saint-luc" \
  -d '{"email":"admin.recherche@saintluc.be","password":"<mot-de-passe-initial>"}'
```

### Vérification des modules accessibles

```bash
TOKEN="<jwt-token-admin>"

# Vérifier l'accès au module études
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: saint-luc" \
     http://localhost:8082/api/v1/studies?page=0&size=5

# Vérifier le dashboard admin
curl -H "Authorization: Bearer $TOKEN" \
     -H "X-Tenant-ID: saint-luc" \
     http://localhost:8091/api/v1/admin/system/health
```

### Health check via admin-service

```http
GET /api/v1/admin/system/health
Authorization: Bearer <super-admin-token>
```

La réponse inclut le statut de chaque service et le nombre de tenants actifs.

## Checklist d'onboarding

```
[ ] Tenant créé avec slug et domain uniques
[ ] Modules actifs configurés selon contrat
[ ] Premier utilisateur admin invité
[ ] Email d'invitation reçu et lien d'activation testé
[ ] Connexion du compte admin vérifiée
[ ] Accès aux modules contractuels vérifiés
[ ] Configuration SMTP tenant définie (si applicable)
[ ] Branding tenant configuré (si applicable)
[ ] Documentation remise à l'équipe IT de l'institution
```

## Résolution de problèmes courants

### Erreur "Tenant not found"

Vérifier que le header `X-Tenant-ID` contient bien le **slug** (ex: `saint-luc`) et non l'UUID ou le nom complet.

### Email d'invitation non reçu

En développement, les emails sont interceptés par MailHog (UI : http://localhost:8025). En production, vérifier les logs du notification-service et la configuration SMTP.

### Utilisateur bloqué après 5 tentatives

Déverrouiller via l'admin-service :

```http
POST /api/v1/admin/users/{userId}/unlock
Authorization: Bearer <super-admin-token>
X-Tenant-ID: saint-luc
```

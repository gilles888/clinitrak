# Auth Service API

*Dernière mise à jour : 2026-05-09 (Session 1)*

**Base URL** : `http://localhost:8081` (dev) / `https://api.clinitrak.be` (prod)
**Préfixe** : `/api/v1/auth`
**Auth** : Endpoints publics (pas de Bearer requis)
**Tenant** : Header `X-Tenant-ID` **obligatoire** sur tous les endpoints

**Swagger UI** : `http://localhost:8081/swagger-ui.html`
**OpenAPI JSON** : `http://localhost:8081/v3/api-docs`

---

## POST /api/v1/auth/login

Authentifie un utilisateur et retourne les tokens JWT.

**Headers requis** :
```
X-Tenant-ID: saintluc
Content-Type: application/json
```

**Request Body** :
```json
{
  "email": "marie.dupont@saintluc.be",
  "password": "P@ssw0rd!"
}
```

**Response 200 — Succès** :
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "marie.dupont@saintluc.be",
    "firstName": "Marie",
    "lastName": "Dupont",
    "tenantId": "00000000-0000-0000-0000-000000000001",
    "roles": ["ROLE_INVESTIGATOR"]
  }
}
```

**Erreurs** :
| Code | Cause |
|------|-------|
| 400 | Tenant non résolu (X-Tenant-ID manquant ou invalide) |
| 401 | Credentials invalides |
| 401 | Compte verrouillé (5 tentatives échouées) |
| 422 | Email ou password vide / format invalide |

---

## POST /api/v1/auth/refresh

Échange un refresh token contre un nouveau couple de tokens (rotation).

**Headers requis** :
```
X-Tenant-ID: saintluc
Content-Type: application/json
```

**Request Body** :
```json
{
  "refreshToken": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response 200** : identique à `/login`

**Erreurs** :
| Code | Cause |
|------|-------|
| 401 | Refresh token inconnu, révoqué ou expiré |

---

## POST /api/v1/auth/logout

Révoque tous les refresh tokens actifs de l'utilisateur courant.

**Headers requis** :
```
Authorization: Bearer <access_token>
X-Tenant-ID: saintluc
```

**Response 204** : No Content (succès silencieux)

---

## POST /api/v1/auth/register

Crée un nouveau compte utilisateur dans le tenant courant.

**Headers requis** :
```
X-Tenant-ID: saintluc
Content-Type: application/json
```

**Request Body** :
```json
{
  "email": "jean.martin@saintluc.be",
  "password": "P@ssw0rd!",
  "firstName": "Jean",
  "lastName": "Martin",
  "roleName": "ROLE_INVESTIGATOR"
}
```

**Response 201 — Créé** :
```json
{
  "message": "Compte créé avec succès",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "email": "jean.martin@saintluc.be"
}
```

**Erreurs** :
| Code | Cause |
|------|-------|
| 400 | Données invalides (voir fieldErrors) |
| 400 | Tenant non résolu |
| 401 | Rôle demandé inconnu |
| 409 | Email déjà utilisé dans ce tenant |

---

## Payload JWT (access token)

```json
{
  "sub": "marie.dupont@saintluc.be",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "tenantId": "00000000-0000-0000-0000-000000000001",
  "tenantSlug": "saintluc",
  "roles": ["ROLE_INVESTIGATOR"],
  "iat": 1715270400,
  "exp": 1715271300
}
```

---

## Rôles disponibles

| Rôle | Description |
|------|-------------|
| `ROLE_SUPER_ADMIN` | Administrateur global de la plateforme |
| `ROLE_ADMIN_TENANT` | Administrateur d'un tenant |
| `ROLE_CE_SECRETARY` | Secrétariat Comité d'Éthique |
| `ROLE_CE_COORDINATOR` | Coordinateur CE |
| `ROLE_CTC_DESK` | Secrétariat CTC |
| `ROLE_CTC_CRA` | Clinical Research Associate |
| `ROLE_CTC_PM` | Project Manager CTC |
| `ROLE_CTC_COFI` | Coordinateur financier CTC |
| `ROLE_PHARMACIST` | Pharmacien responsable |
| `ROLE_INVESTIGATOR` | Investigateur principal / co-investigateur |
| `ROLE_EXTERNAL` | Collaborateur externe (lecture seule) |

---

## Format d'erreur (RFC 7807 ProblemDetail)

```json
{
  "type": "https://clinitrak.be/problems/authentication-error",
  "title": "Erreur d'authentification",
  "status": 401,
  "detail": "Identifiants invalides",
  "timestamp": "2026-05-09T10:30:00Z",
  "fieldErrors": {
    "email": "Format d'email invalide",
    "password": "Le mot de passe doit contenir entre 8 et 128 caractères"
  }
}
```

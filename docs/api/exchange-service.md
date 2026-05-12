# Exchange Service API

**Port** : 8086
**Base URL** : `/api/v1/exchange`
**Auth** : Voir détail par endpoint (JWT externe ou JWT interne)
**Tenant** : Header `X-Tenant-ID` requis sur les endpoints internes

## Contexte fonctionnel

Le service Exchange est le portail public destiné aux partenaires externes (sponsors, CROs,
investigateurs extérieurs). Il expose deux plans d'authentification distincts :

- **JWT externe** : généré par `ExchangeJwtService` via la clé `EXCHANGE_JWT_SECRET` — pour
  les utilisateurs externes enregistrés dans ce service
- **JWT interne** : généré par `auth-service` via la clé `JWT_SECRET` — pour le personnel
  interne CliniTrak qui consulte les demandes reçues

---

## Enums de référence

| Enum | Valeurs |
|------|---------|
| `ExternalUserRole` | `COMPANY`, `INVESTIGATOR`, `CE_REQUESTOR` |
| `TargetModule` | `CE`, `CTC` |
| `ExchangeRequestType` | `NEW_STUDY`, `AMENDMENT`, `EXTENSION`, `INFORMATION` |
| `ExchangeStatus` | `DRAFT`, `SUBMITTED`, `UNDER_REVIEW`, `ACCEPTED`, `REJECTED`, `MORE_INFO` |
| `SenderType` | `INTERNAL`, `EXTERNAL` |

---

## Endpoints — Authentification externe (publics)

### POST /api/v1/exchange/auth/register

**Description** : Inscription d'un utilisateur externe. Déclenche l'envoi d'un email de
vérification (via MailHog en développement).
**Accès** : Public (pas de JWT requis)
**Request Body** :
```json
{
  "email": "string",
  "password": "string",
  "firstName": "string",
  "lastName": "string",
  "organization": "string",
  "role": "COMPANY | INVESTIGATOR | CE_REQUESTOR"
}
```
**Response 201** :
```json
{
  "id": "uuid",
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "organization": "string",
  "role": "ExternalUserRole",
  "verifiedEmail": false,
  "createdAt": "ISO-8601"
}
```
**Erreurs** :
- `400` : Validation échouée (email invalide, mot de passe trop court)
- `409` : Email déjà enregistré

---

### POST /api/v1/exchange/auth/login

**Description** : Connexion d'un utilisateur externe. Retourne un JWT externe signé avec
`EXCHANGE_JWT_SECRET`. L'email doit avoir été vérifié au préalable.
**Accès** : Public
**Request Body** :
```json
{
  "email": "string",
  "password": "string"
}
```
**Response 200** :
```json
{
  "accessToken": "string",
  "tokenType": "Bearer",
  "user": {
    "id": "uuid",
    "email": "string",
    "firstName": "string",
    "lastName": "string",
    "organization": "string",
    "role": "ExternalUserRole",
    "verifiedEmail": true
  }
}
```
**Erreurs** :
- `401` : Identifiants incorrects ou email non vérifié
- `403` : Compte suspendu

---

### GET /api/v1/exchange/auth/verify/{token}

**Description** : Vérification de l'adresse email via le token reçu par mail.
**Accès** : Public
**Path Params** :
- `token` : UUID du token de vérification (envoyé par email)

**Response 200** :
```json
{
  "id": "uuid",
  "email": "string",
  "verifiedEmail": true
}
```
**Erreurs** :
- `404` : Token invalide ou expiré

---

## Endpoints — Demandes (JWT externe requis)

Le JWT externe doit être passé en header `Authorization: Bearer <token>`.
Le claim `externalUserId` est extrait du token pour identifier l'utilisateur.

### POST /api/v1/exchange/requests

**Description** : Crée une nouvelle demande en statut `DRAFT`.
**Accès** : JWT externe valide
**Request Body** :
```json
{
  "targetModule": "CE | CTC",
  "requestType": "NEW_STUDY | AMENDMENT | EXTENSION | INFORMATION",
  "title": "string",
  "description": "string"
}
```
**Response 201** :
```json
{
  "id": "uuid",
  "targetModule": "CE",
  "requestType": "NEW_STUDY",
  "title": "string",
  "description": "string",
  "status": "DRAFT",
  "submittedBy": {
    "id": "uuid",
    "firstName": "string",
    "lastName": "string",
    "organization": "string"
  },
  "linkedStudyId": null,
  "documents": [],
  "messages": [],
  "createdAt": "ISO-8601",
  "updatedAt": "ISO-8601"
}
```
**Erreurs** :
- `400` : Validation échouée
- `401` : JWT externe manquant ou invalide

---

### GET /api/v1/exchange/requests

**Description** : Liste toutes les demandes de l'utilisateur externe connecté.
**Accès** : JWT externe valide
**Response 200** : `ExchangeRequestResponse[]` (uniquement les demandes de l'utilisateur authentifié)

---

### GET /api/v1/exchange/requests/{id}

**Description** : Détail d'une demande avec ses documents et messages.
**Accès** : JWT externe valide (propriétaire de la demande)
**Response 200** : `ExchangeRequestResponse` avec les champs `documents` et `messages` remplis
**Erreurs** :
- `403` : La demande appartient à un autre utilisateur
- `404` : Demande introuvable

---

### POST /api/v1/exchange/requests/{id}/submit

**Description** : Soumet officiellement la demande (passage `DRAFT` → `SUBMITTED`).
**Accès** : JWT externe valide (propriétaire)
**Response 200** : `ExchangeRequestResponse` avec `status: "SUBMITTED"`
**Erreurs** :
- `400` : La demande n'est pas en statut `DRAFT`
- `403` : La demande appartient à un autre utilisateur
- `404` : Demande introuvable

---

### GET /api/v1/exchange/requests/{id}/messages

**Description** : Liste les messages de la messagerie associée à une demande.
**Accès** : JWT externe valide (propriétaire)
**Response 200** :
```json
[
  {
    "id": "uuid",
    "content": "string",
    "senderType": "INTERNAL | EXTERNAL",
    "senderId": "uuid",
    "attachmentPath": "string | null",
    "createdAt": "ISO-8601"
  }
]
```

---

### POST /api/v1/exchange/requests/{id}/messages

**Description** : Envoie un message dans la messagerie d'une demande.
**Accès** : JWT externe valide (propriétaire)
**Request Body** :
```json
{
  "content": "string",
  "attachmentPath": "string (optionnel)"
}
```
**Response 201** : `ExchangeMessageResponse`

---

## Endpoints — Internes (JWT interne requis)

Ces endpoints sont réservés au personnel CliniTrak authentifié via `auth-service`.

### GET /api/v1/exchange/internal/requests

**Description** : Liste toutes les demandes reçues pour le tenant courant.
**Accès** : `ROLE_CTC_MANAGER`, `ROLE_CE_MEMBER` (JWT interne + header `X-Tenant-ID`)
**Response 200** : `ExchangeRequestResponse[]` (toutes statuts, filtrées par tenant)

---

### PATCH /api/v1/exchange/internal/requests/{id}/status

**Description** : Met à jour le statut d'une demande (instruction côté interne).
**Accès** : `ROLE_CTC_MANAGER`, `ROLE_CE_MEMBER`
**Request Body** :
```json
{
  "status": "UNDER_REVIEW | ACCEPTED | REJECTED | MORE_INFO"
}
```
**Response 200** : `ExchangeRequestResponse` avec le nouveau statut
**Erreurs** :
- `400` : Transition de statut non autorisée
- `404` : Demande introuvable

---

### PATCH /api/v1/exchange/internal/requests/{id}/link-study

**Description** : Associe une étude CliniTrak à la demande externe (après acceptation).
**Accès** : `ROLE_CTC_MANAGER`, `ROLE_CE_MEMBER`
**Request Body** :
```json
{
  "studyId": "uuid"
}
```
**Response 200** : `ExchangeRequestResponse` avec `linkedStudyId` renseigné

---

## Architecture sécurité

```
ExternalUser --(EXCHANGE_JWT_SECRET)--> JWT externe --> ExchangeJwtFilter
InternalUser --(JWT_SECRET)-----------► JWT interne --> JwtAuthenticationFilter (partagé)
```

- Le claim `externalUserId` (UUID) identifie l'utilisateur externe dans le JWT externe
- Les deux types de JWT coexistent sans conflit grâce à des filtres séparés
- Le token externe n'est pas reconnu par les endpoints internes, et vice versa

---

## Variables d'environnement requises

| Variable | Description | Défaut dev |
|----------|-------------|------------|
| `EXCHANGE_DB_NAME` | Nom de la base PostgreSQL | `clinitrak_exchange` |
| `EXCHANGE_JWT_SECRET` | Clé secrète JWT externe (min 256 bits) | `changeme-exchange-secret-...` |
| `JWT_SECRET` | Clé JWT interne partagée | `changeme-clinitrak-...` |
| `MAIL_HOST` | Serveur SMTP | `mailhog` |
| `MAIL_PORT` | Port SMTP | `1025` |

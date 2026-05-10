# CliniTrak — API Study Service

*Dernière mise à jour : 2026-05-09 (Session 2)*

## Informations générales

| Propriété | Valeur |
|-----------|--------|
| Base URL (dev) | `http://localhost:8082/api/v1` |
| Via Gateway | `http://localhost:8080/api/v1` |
| Swagger UI | `http://localhost:8082/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8082/v3/api-docs` |
| Authentification | Bearer JWT (header `Authorization: Bearer <token>`) |
| Tenant | Header `X-Tenant-ID: <slug>` (ex: `saintluc`) |
| Format de réponse | `application/json` |

---

## Authentification et tenant

Tous les endpoints requièrent :
1. Un JWT valide (obtenu via auth-service) dans le header `Authorization`
2. L'identifiant du tenant dans le header `X-Tenant-ID`

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
X-Tenant-ID: saintluc
```

---

## Enums

### StudyType — Type d'étude

| Valeur | Label français |
|--------|----------------|
| `INTERVENTIONAL` | Interventionnelle |
| `OBSERVATIONAL` | Observationnelle |
| `EXPANDED_ACCESS` | Accès élargi |

### SponsorType — Type de promoteur

| Valeur | Label français |
|--------|----------------|
| `ACADEMIC` | Académique |
| `COMMERCIAL` | Commercial |
| `INSTITUTIONAL` | Institutionnel |

### StudyPhase — Phase de l'étude

| Valeur | Label français |
|--------|----------------|
| `PHASE_1` | Phase I |
| `PHASE_2` | Phase II |
| `PHASE_3` | Phase III |
| `PHASE_4` | Phase IV |
| `NA` | Non applicable |

### StudyStatus — Statut de l'étude

| Valeur | Label français |
|--------|----------------|
| `DRAFT` | Brouillon |
| `SUBMITTED` | Soumise |
| `APPROVED` | Approuvée |
| `ONGOING` | En cours |
| `SUSPENDED` | Suspendue |
| `CLOSED` | Clôturée |
| `WITHDRAWN` | Retirée |

### ContactType — Type de contact

| Valeur | Label français |
|--------|----------------|
| `PRINCIPAL_INVESTIGATOR` | Investigateur principal |
| `CO_INVESTIGATOR` | Co-investigateur |
| `STUDY_COORDINATOR` | Coordinateur d'étude |
| `SPONSOR_REPRESENTATIVE` | Représentant promoteur |
| `STATISTICIAN` | Statisticien |
| `DATA_MANAGER` | Data manager |
| `OTHER` | Autre |

### SubmissionType — Type de soumission

| Valeur | Label français |
|--------|----------------|
| `INITIAL` | Initiale |
| `AMENDMENT` | Amendement |
| `ANNUAL_REVIEW` | Revue annuelle |
| `SAFETY_REPORT` | Rapport de sécurité |
| `FINAL_REPORT` | Rapport final |

### SubmissionStatus — Statut de soumission

| Valeur | Label français |
|--------|----------------|
| `PENDING` | En attente |
| `UNDER_REVIEW` | En cours d'évaluation |
| `APPROVED` | Approuvée |
| `REJECTED` | Rejetée |
| `REVISION_REQUESTED` | Révision demandée |
| `WITHDRAWN` | Retirée |

### PatientStatus — Statut patient

| Valeur | Label français |
|--------|----------------|
| `SCREENED` | En screening |
| `ENROLLED` | Inclus |
| `ACTIVE` | Actif |
| `COMPLETED` | Complété |
| `WITHDRAWN` | Retiré |
| `SCREEN_FAILED` | Échec screening |
| `LOST_TO_FOLLOW_UP` | Perdu de vue |

---

## Format d'erreur (RFC 7807)

Toutes les erreurs suivent le standard Problem Details (RFC 7807) :

```json
{
  "type": "https://clinitrak.be/errors/validation-error",
  "title": "Validation échouée",
  "status": 400,
  "detail": "Le champ 'title' est obligatoire",
  "instance": "/api/v1/studies",
  "timestamp": "2026-05-09T10:30:00Z",
  "errors": [
    {
      "field": "title",
      "message": "ne doit pas être vide"
    }
  ]
}
```

### Codes d'erreur communs

| Code HTTP | Situation |
|-----------|-----------|
| 400 | Données invalides (`@Valid` échoué) |
| 401 | JWT absent, expiré ou invalide |
| 403 | Rôle insuffisant |
| 404 | Ressource introuvable |
| 409 | Conflit (ex: numéro EudraCT déjà utilisé) |
| 422 | Transition de statut impossible |
| 500 | Erreur serveur interne |

---

## Endpoints

### Vue d'ensemble

| # | Méthode | Path | Description |
|---|---------|------|-------------|
| 1 | GET | `/studies` | Recherche paginée |
| 2 | POST | `/studies` | Créer une étude |
| 3 | GET | `/studies/{id}` | Détail d'une étude |
| 4 | PUT | `/studies/{id}` | Modifier une étude |
| 5 | DELETE | `/studies/{id}` | Supprimer (soft) une étude |
| 6 | PATCH | `/studies/{id}/status` | Changer le statut |
| 7 | GET | `/studies/{id}/status-history` | Historique des statuts |
| 8 | GET | `/studies/{id}/contacts` | Contacts de l'étude |
| 9 | POST | `/studies/{id}/contacts` | Ajouter un contact |
| 10 | PUT | `/studies/{id}/contacts/{contactId}` | Modifier un contact |
| 11 | DELETE | `/studies/{id}/contacts/{contactId}` | Supprimer un contact |
| 12 | GET | `/studies/{id}/submissions` | Soumissions de l'étude |
| 13 | POST | `/studies/{id}/submissions` | Créer une soumission |
| 14 | PUT | `/studies/{id}/submissions/{submissionId}` | Modifier une soumission |
| 15 | GET | `/studies/{id}/patients` | Patients de l'étude |
| 16 | POST | `/studies/{id}/patients` | Ajouter un patient |
| 17 | PATCH | `/studies/{id}/patients/{patientId}/status` | Changer statut patient |
| 18 | DELETE | `/studies/{id}/patients/{patientId}` | Retirer un patient |
| 19 | GET | `/studies/statistics` | Statistiques globales |

---

### 1. GET /api/v1/studies

**Description** : Recherche paginée des études avec filtres multicritères

**Accès** : Tous les utilisateurs authentifiés

**Query Parameters** :

| Param | Type | Description |
|-------|------|-------------|
| titleKeyword | string | Recherche dans le titre (LIKE) |
| ethicsNumber | string | Numéro CE exact |
| eudractNumber | string | Numéro EudraCT exact |
| ctisNumber | string | Numéro CTIS exact |
| acronym | string | Acronyme (LIKE) |
| sponsorType | enum | `ACADEMIC`, `COMMERCIAL`, `INSTITUTIONAL` |
| status | enum | `DRAFT`, `SUBMITTED`, `APPROVED`, `ONGOING`, `SUSPENDED`, `CLOSED`, `WITHDRAWN` |
| therapeuticArea | string | Aire thérapeutique |
| phase | enum | `PHASE_1`, `PHASE_2`, `PHASE_3`, `PHASE_4`, `NA` |
| startDateFrom | date | Format ISO 8601 (YYYY-MM-DD) |
| startDateTo | date | Format ISO 8601 |
| isSponsorCusl | boolean | Études promoteur CUSL uniquement |
| page | int | Numéro de page (défaut: 0) |
| size | int | Taille de page (défaut: 20, max: 100) |
| sort | string | Ex: `title,asc` ou `createdAt,desc` |

**Response 200** :
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "studyNumber": "ST-2026-00001",
      "title": "Évaluation de l'efficacité du traitement X dans la maladie Y",
      "acronym": "EVAL-XY",
      "studyType": "INTERVENTIONAL",
      "sponsorType": "ACADEMIC",
      "phase": "PHASE_2",
      "currentStatus": "ONGOING",
      "therapeuticArea": "Oncologie",
      "principalInvestigator": "Dr. Jean Dupont",
      "startDate": "2025-01-15",
      "currentEnrollment": 45,
      "targetEnrollment": 120,
      "isSponsorCusl": true,
      "createdAt": "2026-01-10T09:00:00Z"
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 42,
  "totalPages": 3,
  "last": false
}
```

**Codes d'erreur** : 401, 403

---

### 2. POST /api/v1/studies

**Description** : Créer une nouvelle étude clinique

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Request Body** :
```json
{
  "title": "Évaluation de l'efficacité du traitement X dans la maladie Y",
  "acronym": "EVAL-XY",
  "studyType": "INTERVENTIONAL",
  "sponsorType": "ACADEMIC",
  "sponsor": "Cliniques Universitaires Saint-Luc",
  "principalInvestigator": "Dr. Jean Dupont",
  "therapeuticArea": "Oncologie",
  "phase": "PHASE_2",
  "ethicsNumber": "CE-2025-001",
  "eudractNumber": "2025-001234-56",
  "ctisNumber": null,
  "startDate": "2025-01-15",
  "endDate": "2027-12-31",
  "targetEnrollment": 120,
  "isSponsorCusl": true,
  "description": "Étude randomisée en double aveugle évaluant..."
}
```

**Response 201** :
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "studyNumber": "ST-2026-00001",
  "title": "Évaluation de l'efficacité du traitement X dans la maladie Y",
  "acronym": "EVAL-XY",
  "studyType": "INTERVENTIONAL",
  "sponsorType": "ACADEMIC",
  "sponsor": "Cliniques Universitaires Saint-Luc",
  "principalInvestigator": "Dr. Jean Dupont",
  "therapeuticArea": "Oncologie",
  "phase": "PHASE_2",
  "currentStatus": "DRAFT",
  "ethicsNumber": "CE-2025-001",
  "eudractNumber": "2025-001234-56",
  "ctisNumber": null,
  "startDate": "2025-01-15",
  "endDate": "2027-12-31",
  "approvalDate": null,
  "targetEnrollment": 120,
  "currentEnrollment": 0,
  "isSponsorCusl": true,
  "description": "Étude randomisée en double aveugle évaluant...",
  "tenantId": "00000000-0000-0000-0000-000000000001",
  "createdAt": "2026-05-09T10:00:00Z",
  "updatedAt": "2026-05-09T10:00:00Z"
}
```

**Codes d'erreur** : 400 (validation), 401, 403, 409 (numéro EudraCT/CTIS déjà utilisé)

---

### 3. GET /api/v1/studies/{id}

**Description** : Récupérer le détail complet d'une étude

**Accès** : Tous les utilisateurs authentifiés

**Path Parameters** :
| Param | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant de l'étude |

**Response 200** : Voir structure complète de l'endpoint POST (StudyResponse)

**Codes d'erreur** : 401, 403, 404

---

### 4. PUT /api/v1/studies/{id}

**Description** : Modifier une étude existante (remplacement complet)

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Path Parameters** :
| Param | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant de l'étude |

**Request Body** : Identique à POST /studies (StudyCreateRequest)

**Response 200** : StudyResponse mis à jour

**Codes d'erreur** : 400, 401, 403, 404, 409

---

### 5. DELETE /api/v1/studies/{id}

**Description** : Suppression logique (soft delete) d'une étude. Seules les études en statut `DRAFT` peuvent être supprimées.

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`

**Path Parameters** :
| Param | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant de l'étude |

**Response 204** : No Content

**Codes d'erreur** : 401, 403, 404, 422 (statut incompatible avec la suppression)

---

### 6. PATCH /api/v1/studies/{id}/status

**Description** : Changer le statut d'une étude. Les transitions autorisées sont :
`DRAFT → SUBMITTED`, `SUBMITTED → APPROVED | WITHDRAWN`, `APPROVED → ONGOING | WITHDRAWN`, `ONGOING → SUSPENDED | CLOSED`, `SUSPENDED → ONGOING | CLOSED`

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CE_COORDINATOR`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Path Parameters** :
| Param | Type | Description |
|-------|------|-------------|
| id | UUID | Identifiant de l'étude |

**Request Body** :
```json
{
  "newStatus": "ONGOING",
  "comment": "Approbation CE reçue le 2026-03-15, démarrage de l'inclusion"
}
```

**Response 200** :
```json
{
  "studyId": "550e8400-e29b-41d4-a716-446655440000",
  "previousStatus": "APPROVED",
  "newStatus": "ONGOING",
  "changedAt": "2026-05-09T10:30:00Z",
  "changedBy": "marie.martin@saintluc.be",
  "comment": "Approbation CE reçue le 2026-03-15, démarrage de l'inclusion"
}
```

**Codes d'erreur** : 400, 401, 403, 404, 422 (transition invalide)

---

### 7. GET /api/v1/studies/{id}/status-history

**Description** : Récupérer l'historique complet des changements de statut

**Accès** : Tous les utilisateurs authentifiés

**Response 200** :
```json
[
  {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "previousStatus": null,
    "newStatus": "DRAFT",
    "changedAt": "2026-01-10T09:00:00Z",
    "changedBy": "jean.dupont@saintluc.be",
    "comment": "Création de l'étude"
  },
  {
    "id": "660e8400-e29b-41d4-a716-446655440002",
    "previousStatus": "DRAFT",
    "newStatus": "SUBMITTED",
    "changedAt": "2026-02-01T14:00:00Z",
    "changedBy": "jean.dupont@saintluc.be",
    "comment": "Soumission au Comité d'Éthique"
  }
]
```

**Codes d'erreur** : 401, 403, 404

---

### 8. GET /api/v1/studies/{id}/contacts

**Description** : Récupérer la liste des contacts associés à une étude

**Accès** : Tous les utilisateurs authentifiés

**Response 200** :
```json
[
  {
    "id": "770e8400-e29b-41d4-a716-446655440001",
    "contactType": "PRINCIPAL_INVESTIGATOR",
    "firstName": "Jean",
    "lastName": "Dupont",
    "email": "jean.dupont@saintluc.be",
    "phone": "+32 2 764 12 34",
    "institution": "Cliniques Universitaires Saint-Luc",
    "department": "Service d'Oncologie"
  }
]
```

**Codes d'erreur** : 401, 403, 404

---

### 9. POST /api/v1/studies/{id}/contacts

**Description** : Ajouter un contact à une étude

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Request Body** :
```json
{
  "contactType": "CO_INVESTIGATOR",
  "firstName": "Marie",
  "lastName": "Martin",
  "email": "marie.martin@saintluc.be",
  "phone": "+32 2 764 56 78",
  "institution": "Cliniques Universitaires Saint-Luc",
  "department": "Service de Pharmacologie"
}
```

**Response 201** : ContactResponse avec `id` généré

**Codes d'erreur** : 400, 401, 403, 404

---

### 10. PUT /api/v1/studies/{id}/contacts/{contactId}

**Description** : Modifier un contact existant

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Response 200** : ContactResponse mis à jour

**Codes d'erreur** : 400, 401, 403, 404

---

### 11. DELETE /api/v1/studies/{id}/contacts/{contactId}

**Description** : Supprimer un contact d'une étude

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Response 204** : No Content

**Codes d'erreur** : 401, 403, 404

---

### 12. GET /api/v1/studies/{id}/submissions

**Description** : Récupérer la liste des soumissions (CE, autorités, etc.) d'une étude

**Accès** : Tous les utilisateurs authentifiés

**Response 200** :
```json
[
  {
    "id": "880e8400-e29b-41d4-a716-446655440001",
    "submissionType": "INITIAL",
    "submissionStatus": "APPROVED",
    "submittedAt": "2026-02-01",
    "reviewedAt": "2026-03-15",
    "reference": "CE-2026-REF-001",
    "comment": "Approbation sans réserve"
  }
]
```

**Codes d'erreur** : 401, 403, 404

---

### 13. POST /api/v1/studies/{id}/submissions

**Description** : Créer une nouvelle soumission pour une étude

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CE_COORDINATOR`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`

**Request Body** :
```json
{
  "submissionType": "AMENDMENT",
  "submittedAt": "2026-05-09",
  "reference": "CE-2026-AMEND-001",
  "comment": "Amendement numéro 1 — modification du critère d'inclusion 3"
}
```

**Response 201** : SubmissionResponse avec statut initial `PENDING`

**Codes d'erreur** : 400, 401, 403, 404

---

### 14. PUT /api/v1/studies/{id}/submissions/{submissionId}

**Description** : Modifier une soumission (statut, date de revue, commentaire)

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CE_COORDINATOR`, `ROLE_CE_SECRETARY`

**Request Body** :
```json
{
  "submissionStatus": "APPROVED",
  "reviewedAt": "2026-06-01",
  "comment": "Approuvé par le CE en séance plénière"
}
```

**Response 200** : SubmissionResponse mis à jour

**Codes d'erreur** : 400, 401, 403, 404

---

### 15. GET /api/v1/studies/{id}/patients

**Description** : Récupérer la liste pseudonymisée des patients inclus dans l'étude

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`, `ROLE_INVESTIGATOR`

**Response 200** :
```json
[
  {
    "id": "990e8400-e29b-41d4-a716-446655440001",
    "patientCode": "EVAL-XY-001",
    "patientStatus": "ACTIVE",
    "enrolledAt": "2026-02-15",
    "completedAt": null,
    "withdrawnAt": null,
    "screeningNumber": "SCR-001"
  }
]
```

**Note RGPD** : Aucune donnée identifiante n'est retournée. Seul le `patientCode` (pseudonyme) est exposé. Voir section RGPD ci-dessous.

**Codes d'erreur** : 401, 403, 404

---

### 16. POST /api/v1/studies/{id}/patients

**Description** : Inclure un patient dans une étude (enregistrement pseudonymisé)

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`, `ROLE_INVESTIGATOR`

**Request Body** :
```json
{
  "patientCode": "EVAL-XY-046",
  "screeningNumber": "SCR-046",
  "enrolledAt": "2026-05-09",
  "patientStatus": "SCREENED"
}
```

**Response 201** : PatientResponse

**Codes d'erreur** : 400, 401, 403, 404, 409 (patientCode déjà utilisé dans cette étude)

---

### 17. PATCH /api/v1/studies/{id}/patients/{patientId}/status

**Description** : Mettre à jour le statut d'un patient dans l'étude

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_CRA`, `ROLE_CTC_PM`, `ROLE_INVESTIGATOR`

**Request Body** :
```json
{
  "patientStatus": "ENROLLED",
  "comment": "Critères d'inclusion vérifiés, consentement signé"
}
```

**Response 200** : PatientResponse mis à jour

**Codes d'erreur** : 400, 401, 403, 404, 422 (transition de statut invalide)

---

### 18. DELETE /api/v1/studies/{id}/patients/{patientId}

**Description** : Retirer un patient de l'étude (soft delete). Seuls les patients en statut `SCREENED` ou `SCREEN_FAILED` peuvent être supprimés.

**Accès** : `ROLE_SUPER_ADMIN`, `ROLE_ADMIN_TENANT`, `ROLE_CTC_PM`

**Response 204** : No Content

**Codes d'erreur** : 401, 403, 404, 422

---

### 19. GET /api/v1/studies/statistics

**Description** : Statistiques globales sur les études du tenant (utilisé par le dashboard)

**Accès** : Tous les utilisateurs authentifiés

**Response 200** :
```json
{
  "totalStudies": 42,
  "byStatus": {
    "DRAFT": 5,
    "SUBMITTED": 3,
    "APPROVED": 8,
    "ONGOING": 20,
    "SUSPENDED": 2,
    "CLOSED": 4,
    "WITHDRAWN": 0
  },
  "byType": {
    "INTERVENTIONAL": 30,
    "OBSERVATIONAL": 10,
    "EXPANDED_ACCESS": 2
  },
  "byPhase": {
    "PHASE_1": 5,
    "PHASE_2": 15,
    "PHASE_3": 12,
    "PHASE_4": 3,
    "NA": 7
  },
  "totalPatients": 1247,
  "activePatients": 856,
  "sponsorCuslCount": 28
}
```

**Codes d'erreur** : 401, 403

---

## Note RGPD — Données patients

Le study-service respecte le RGPD article 89 relatif au traitement à des fins de recherche :

- **Aucune donnée identifiante** n'est stockée dans `clinitrak_study` (pas de nom, prénom, date de naissance, numéro de registre national)
- Seul le **code pseudonyme** (`patientCode`) est stocké — attribué par l'investigateur selon la procédure de pseudonymisation de l'institution
- La **table de correspondance** pseudonyme ↔ identité réelle est gérée **exclusivement** par l'investigateur, en dehors de CliniTrak
- Le `screeningNumber` est un numéro de screening attribué séquentiellement, sans lien avec l'identité
- Toute demande d'accès, rectification ou effacement doit passer par le DPO de l'institution

---

## Numérotation des études

Format : `ST-{YYYY}-{NNNNN}`

Exemple : `ST-2026-00001`

- `YYYY` : année de création
- `NNNNN` : séquence sur 5 chiffres, unique par tenant et par année
- Généré automatiquement si non fourni à la création
- Unique par tenant (contrainte DB)

# CliniTrak — API Ethics Service

*Dernière mise à jour : 2026-05-09 (Session 3)*

## Vue d'ensemble

Le **ethics-service** gère l'ensemble du workflow du Comité d'Éthique (CE) :
soumissions d'avis, réunions, rapports annuels, génération de correspondance et de PDF.

| Propriété | Valeur |
|-----------|--------|
| Port | **8083** |
| Base URL | `http://localhost:8083` (dev) / `https://api.clinitrak.be/ethics` (prod via gateway) |
| Base de données | `clinitrak_ethics` |
| Swagger UI | `http://localhost:8083/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8083/v3/api-docs` |
| Actuator Health | `http://localhost:8083/actuator/health` |

---

## Authentification & Headers

Tous les endpoints (sauf `/actuator/**`) requièrent :

```
Authorization: Bearer <accessToken>
X-Tenant-ID: <slug>           (ex: "saintluc")
Content-Type: application/json
```

Le JWT est validé **localement** (même secret partagé que auth-service).
Le `tenantId` est extrait du JWT payload pour isoler les données.

---

## Enums

### `ReviewType` — Type de soumission CE

| Valeur | Label FR | Description |
|--------|----------|-------------|
| `INITIAL` | Demande initiale | Première soumission pour une étude |
| `AMENDMENT` | Amendement | Modification substantielle du protocole |
| `ANNUAL_REVIEW` | Rapport annuel | Rapport annuel de suivi |
| `SAFETY_REPORT` | Rapport de sécurité | Notification d'événement indésirable grave |
| `FINAL_REPORT` | Rapport final | Clôture de l'étude |

### `ReviewDecision` — Décision du CE

| Valeur | Label FR | Description |
|--------|----------|-------------|
| `PENDING` | En attente | Soumis, pas encore examiné |
| `FAVORABLE` | Favorable | Approuvé sans réserve |
| `FAVORABLE_WITH_CONDITIONS` | Favorable sous conditions | Approuvé avec demandes de modifications |
| `UNFAVORABLE` | Défavorable | Refusé |
| `ADDITIONAL_INFO_REQUESTED` | Informations complémentaires demandées | En attente de clarifications |
| `WITHDRAWN` | Retiré | Retiré par le demandeur |

### `MeetingType` — Type de réunion CE

| Valeur | Label FR |
|--------|----------|
| `ORDINARY` | Réunion ordinaire |
| `EXTRAORDINARY` | Réunion extraordinaire |
| `WRITTEN_PROCEDURE` | Procédure écrite |

### `MeetingStatus` — Statut de réunion

| Valeur | Label FR |
|--------|----------|
| `PLANNED` | Planifiée |
| `IN_PROGRESS` | En cours |
| `COMPLETED` | Terminée |
| `CANCELLED` | Annulée |

### `AnnualReportStatus` — Statut rapport annuel

| Valeur | Label FR |
|--------|----------|
| `PENDING` | En attente |
| `SUBMITTED` | Soumis |
| `VALIDATED` | Validé |
| `OVERDUE` | En retard |

### `TemplateType` — Type de template de correspondance

| Valeur | Label FR |
|--------|----------|
| `FAVORABLE_OPINION` | Avis favorable |
| `FAVORABLE_OPINION_WITH_CONDITIONS` | Avis favorable sous conditions |
| `UNFAVORABLE_OPINION` | Avis défavorable |
| `ADDITIONAL_INFO_REQUEST` | Demande d'informations complémentaires |
| `ANNUAL_REMINDER` | Rappel rapport annuel |
| `AMENDMENT_ACKNOWLEDGMENT` | Accusé de réception amendement |

---

## Section : Génération du numéro CE

### Format

```
YYYY/NNNN
```

- `YYYY` : année courante (ex: 2026)
- `NNNN` : séquence à 4 chiffres zero-padded (0001 → 9999, repart à 0001 chaque année)

Exemples : `2026/0001`, `2026/0042`, `2027/0001`

### Mécanisme (verrou pessimiste)

La table `ethics_sequences` contient une ligne par année et par tenant.
Le `SequenceGeneratorService` :

1. Ouvre une transaction avec niveau d'isolation **SERIALIZABLE**
2. Exécute un `SELECT ... FOR UPDATE` (verrou pessimiste `PESSIMISTIC_WRITE`)
3. Incrémente le compteur
4. Formate le numéro : `String.format("%d/%04d", year, nextVal)`
5. Commit → le verrou est libéré

Ce mécanisme garantit l'**unicité absolue** du numéro CE, même sous charge concurrente,
sans risque de collision ou de trou dans la séquence.

---

## Section : Moteur de templates

Les templates de correspondance sont stockés en base de données (table `correspondence_templates`)
au format HTML avec variables Thymeleaf.

### Variables disponibles dans les templates

| Variable | Type | Description |
|----------|------|-------------|
| `studyTitle` | String | Titre complet de l'étude |
| `ethicsNumber` | String | Numéro CE (ex: 2026/0042) |
| `investigatorName` | String | Nom de l'investigateur principal |
| `studyAcronym` | String | Acronyme de l'étude |
| `submissionDate` | LocalDate | Date de soumission |
| `decisionDate` | LocalDate | Date de la décision |
| `rapporteurName` | String | Nom du rapporteur |
| `conditions` | String | Conditions (si favorable sous conditions) |
| `tenantName` | String | Nom de l'institution (ex: Saint-Luc) |
| `meetingDate` | LocalDate | Date de la réunion CE |
| `currentDate` | LocalDate | Date du jour |

### Exemple de template HTML

```html
<p>Bruxelles, le <span th:text="${currentDate}"></span></p>
<p>Objet : Avis favorable — Étude <span th:text="${studyAcronym}"></span></p>
<p>Le Comité d'Éthique a examiné votre dossier
   <strong th:text="${ethicsNumber}"></strong> lors de sa réunion
   du <span th:text="${meetingDate}"></span>.</p>
```

Le `TemplateEngineService` utilise `StringTemplateResolver` de Thymeleaf pour rendre
les templates stockés en base (pas de fichiers `.html` sur disque).

---

## Section : Génération PDF

### Technologie

- **Flying Saucer** (org.xhtmlrenderer) : rendu HTML/CSS → PDF
- **OpenPDF** (com.github.librepdf) : backend PDF

### Pipeline

```
Template HTML (Thymeleaf) → HTML rendu → Flying Saucer → PDF bytes
```

Le PDF est retourné en réponse HTTP avec :
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="avis_CE_2026-0042.pdf"
```

### Limitations CSS

Flying Saucer supporte CSS 2.1. Eviter Flexbox/Grid dans les templates PDF.
Utiliser des tableaux HTML pour la mise en page.

---

## Section : Feign Client study-service

### `StudyServiceClient`

```java
@FeignClient(name = "study-service", url = "${study-service.url}")
public interface StudyServiceClient {
    @GetMapping("/api/v1/studies/{id}")
    StudyDto getStudyById(@PathVariable UUID id);
}
```

### `FeignConfig`

Propage automatiquement depuis la requête HTTP courante :
- `Authorization: Bearer <token>`
- `X-Tenant-ID: <tenantSlug>`

**URL** : `${STUDY_SERVICE_URL}` → `http://clinitrak-study:8082` (Docker) / `http://localhost:8082` (dev local)

### DTO retourné (`StudyDto`)

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "studyNumber": "ST-2026-00001",
  "title": "Évaluation de l'efficacité de...",
  "acronym": "EVAL-X",
  "principalInvestigator": "Dr. Martin",
  "currentStatus": "ACTIVE",
  "tenantId": "uuid-du-tenant"
}
```

---

## Section : MailHog (dev)

En développement, tous les emails sont interceptés par **MailHog**.

| | |
|--|--|
| SMTP (Spring Mail) | `localhost:1025` |
| Web UI (consulter les emails) | `http://localhost:8025` |
| Image Docker | `mailhog/mailhog:latest` |

Pour consulter les emails envoyés (rappels rapports annuels, correspondances CE) :
ouvrir `http://localhost:8025` dans le navigateur.

---

## Endpoints

### 1. POST /api/v1/ethics/reviews

**Description** : Soumet un nouvel avis CE pour une étude. Génère automatiquement le numéro CE (format YYYY/NNNN).
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "studyId": "550e8400-e29b-41d4-a716-446655440000",
  "reviewType": "INITIAL",
  "submissionDate": "2026-05-09",
  "rapporteurName": "Dr. Martin",
  "comments": "Dossier complet - examen en réunion ordinaire"
}
```

**Response 201** :
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "ethicsNumber": "2026/0001",
  "studyId": "550e8400-e29b-41d4-a716-446655440000",
  "studyTitle": "Évaluation de l'efficacité de...",
  "reviewType": "INITIAL",
  "reviewTypeLabel": "Demande initiale",
  "decision": "PENDING",
  "decisionLabel": "En attente",
  "submissionDate": "2026-05-09",
  "rapporteurName": "Dr. Martin",
  "comments": "Dossier complet - examen en réunion ordinaire",
  "tenantId": "uuid-du-tenant",
  "createdAt": "2026-05-09T10:30:00Z",
  "updatedAt": "2026-05-09T10:30:00Z"
}
```

**Erreurs** : 400 (validation), 401 (non auth), 403 (rôle insuffisant), 404 (étude introuvable)

---

### 2. GET /api/v1/ethics/reviews

**Description** : Liste tous les avis CE du tenant, avec filtres optionnels.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Query Parameters** :
| Paramètre | Type | Description |
|-----------|------|-------------|
| `reviewType` | ReviewType | Filtrer par type (optionnel) |
| `decision` | ReviewDecision | Filtrer par décision (optionnel) |
| `page` | int | Page (défaut: 0) |
| `size` | int | Taille page (défaut: 20) |
| `sort` | string | Ex: `submissionDate,desc` |

**Response 200** :
```json
{
  "content": [ { "id": "...", "ethicsNumber": "2026/0001", "decision": "PENDING", ... } ],
  "totalElements": 42,
  "totalPages": 3,
  "size": 20,
  "number": 0
}
```

**Erreurs** : 401, 403

---

### 3. GET /api/v1/ethics/reviews/{id}

**Description** : Détail d'un avis CE par son identifiant.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Path Variable** : `id` (UUID)

**Response 200** : objet `EthicsReviewResponse` complet (même structure que POST 201)

**Erreurs** : 401, 403, 404 (avis introuvable ou appartient à un autre tenant)

---

### 4. PUT /api/v1/ethics/reviews/{id}/decision

**Description** : Enregistre la décision du CE sur un avis. Met à jour `decision`, `decisionDate`, `decisionComments`.
**Accès** : ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Path Variable** : `id` (UUID)

**Request Body** :
```json
{
  "decision": "FAVORABLE",
  "decisionDate": "2026-05-15",
  "decisionComments": "Protocole conforme aux bonnes pratiques cliniques",
  "meetingId": "770e8400-e29b-41d4-a716-446655440002"
}
```

**Response 200** : `EthicsReviewResponse` mis à jour avec `decision`, `decisionLabel`, `decisionDate`

**Erreurs** : 400 (décision invalide), 401, 403, 404, 409 (décision déjà enregistrée)

---

### 5. GET /api/v1/ethics/reviews/study/{studyId}

**Description** : Liste tous les avis CE d'une étude spécifique.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_INVESTIGATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Path Variable** : `studyId` (UUID)

**Response 200** : liste de `EthicsReviewResponse`, triée par `submissionDate DESC`

**Erreurs** : 401, 403, 404 (étude introuvable)

---

### 6. POST /api/v1/ethics/meetings

**Description** : Crée une nouvelle réunion CE.
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "meetingDate": "2026-06-10",
  "meetingTime": "14:00",
  "meetingType": "ORDINARY",
  "location": "Salle de conférence A, Tour hospitalière",
  "notes": "Ordre du jour à confirmer"
}
```

**Response 201** :
```json
{
  "id": "880e8400-e29b-41d4-a716-446655440003",
  "meetingDate": "2026-06-10",
  "meetingTime": "14:00",
  "meetingType": "ORDINARY",
  "meetingTypeLabel": "Réunion ordinaire",
  "status": "PLANNED",
  "statusLabel": "Planifiée",
  "location": "Salle de conférence A, Tour hospitalière",
  "notes": "Ordre du jour à confirmer",
  "agendaItems": [],
  "createdAt": "2026-05-09T10:00:00Z"
}
```

**Erreurs** : 400, 401, 403

---

### 7. GET /api/v1/ethics/meetings

**Description** : Liste les réunions CE du tenant, avec filtres optionnels.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Query Parameters** :
| Paramètre | Type | Description |
|-----------|------|-------------|
| `status` | MeetingStatus | Filtrer par statut (optionnel) |
| `from` | LocalDate | Date de début (optionnel) |
| `to` | LocalDate | Date de fin (optionnel) |

**Response 200** : liste de `MeetingResponse`

**Erreurs** : 401, 403

---

### 8. GET /api/v1/ethics/meetings/{id}

**Description** : Détail d'une réunion CE avec son ordre du jour.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Response 200** : `MeetingResponse` avec liste `agendaItems` complète

**Erreurs** : 401, 403, 404

---

### 9. PUT /api/v1/ethics/meetings/{id}/status

**Description** : Change le statut d'une réunion (ex: PLANNED → IN_PROGRESS → COMPLETED).
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "status": "COMPLETED",
  "notes": "Réunion terminée. 5 dossiers examinés."
}
```

**Response 200** : `MeetingResponse` avec nouveau statut

**Erreurs** : 400 (transition invalide), 401, 403, 404

---

### 10. POST /api/v1/ethics/meetings/{id}/agenda

**Description** : Ajoute un point à l'ordre du jour d'une réunion.
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "reviewId": "660e8400-e29b-41d4-a716-446655440001",
  "order": 1,
  "estimatedDurationMinutes": 30,
  "notes": "Première soumission, dossier complet"
}
```

**Response 201** :
```json
{
  "id": "990e8400-e29b-41d4-a716-446655440004",
  "meetingId": "880e8400-e29b-41d4-a716-446655440003",
  "reviewId": "660e8400-e29b-41d4-a716-446655440001",
  "ethicsNumber": "2026/0001",
  "studyTitle": "Évaluation de l'efficacité de...",
  "order": 1,
  "estimatedDurationMinutes": 30,
  "notes": "Première soumission, dossier complet"
}
```

**Erreurs** : 400, 401, 403, 404 (réunion ou avis introuvable), 409 (avis déjà dans l'ordre du jour)

---

### 11. GET /api/v1/ethics/annual-reports

**Description** : Liste les rapports annuels du tenant avec leurs statuts.
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_INVESTIGATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Query Parameters** :
| Paramètre | Type | Description |
|-----------|------|-------------|
| `status` | AnnualReportStatus | Filtrer par statut |
| `dueBefore` | LocalDate | Échéance avant cette date |

**Response 200** :
```json
[
  {
    "id": "aa0e8400-e29b-41d4-a716-446655440005",
    "studyId": "550e8400-e29b-41d4-a716-446655440000",
    "studyTitle": "Évaluation de l'efficacité de...",
    "ethicsNumber": "2026/0001",
    "reportYear": 2025,
    "dueDate": "2026-06-09",
    "status": "PENDING",
    "statusLabel": "En attente",
    "daysUntilDue": 31
  }
]
```

**Erreurs** : 401, 403

---

### 12. PUT /api/v1/ethics/annual-reports/{id}/submit

**Description** : Marque un rapport annuel comme soumis.
**Accès** : ROLE_INVESTIGATOR, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "submissionDate": "2026-05-09",
  "comments": "Rapport soumis par voie électronique"
}
```

**Response 200** : `AnnualReportResponse` avec `status: "SUBMITTED"`

**Erreurs** : 400, 401, 403, 404, 409 (déjà soumis)

---

### 13. GET /api/v1/ethics/templates

**Description** : Liste tous les templates de correspondance du tenant.
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Response 200** :
```json
[
  {
    "id": "bb0e8400-e29b-41d4-a716-446655440006",
    "templateType": "FAVORABLE_OPINION",
    "templateTypeLabel": "Avis favorable",
    "name": "Avis favorable standard",
    "language": "FR",
    "active": true,
    "createdAt": "2026-05-09T10:00:00Z"
  }
]
```

**Erreurs** : 401, 403

---

### 14. POST /api/v1/ethics/correspondence/generate

**Description** : Génère une lettre de correspondance CE depuis un template Thymeleaf, la stocke et renvoie les métadonnées.
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Request Body** :
```json
{
  "reviewId": "660e8400-e29b-41d4-a716-446655440001",
  "templateId": "bb0e8400-e29b-41d4-a716-446655440006",
  "additionalVariables": {
    "conditions": "Fournir le protocole amendé dans les 30 jours"
  }
}
```

**Response 201** :
```json
{
  "id": "cc0e8400-e29b-41d4-a716-446655440007",
  "reviewId": "660e8400-e29b-41d4-a716-446655440001",
  "templateId": "bb0e8400-e29b-41d4-a716-446655440006",
  "templateType": "FAVORABLE_OPINION",
  "generatedAt": "2026-05-09T14:22:00Z",
  "pdfAvailable": true
}
```

**Erreurs** : 400, 401, 403, 404 (review ou template introuvable), 500 (erreur génération PDF)

---

### 15. GET /api/v1/ethics/correspondence/{id}/pdf

**Description** : Télécharge le PDF d'une correspondance CE générée.
**Accès** : ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Path Variable** : `id` (UUID de la correspondance)

**Response 200** :
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="avis_CE_2026-0001.pdf"
<binary PDF data>
```

**Erreurs** : 401, 403, 404

---

### 16. GET /api/v1/ethics/dashboard

**Description** : Retourne les KPIs du tableau de bord CE (statistiques agrégées pour le tenant).
**Accès** : ROLE_CE_MEMBER, ROLE_CE_SECRETARY, ROLE_CE_COORDINATOR, ROLE_ADMIN_TENANT, ROLE_SUPER_ADMIN
**Headers** : `Authorization: Bearer <token>`, `X-Tenant-ID: <slug>`

**Response 200** :
```json
{
  "totalReviews": 42,
  "pendingReviews": 8,
  "favorableReviews": 28,
  "unfavorableReviews": 3,
  "additionalInfoRequested": 3,
  "upcomingMeetings": 2,
  "nextMeeting": {
    "id": "880e8400-e29b-41d4-a716-446655440003",
    "meetingDate": "2026-06-10",
    "meetingType": "ORDINARY",
    "agendaItemCount": 5
  },
  "annualReportsDueSoon": 3,
  "recentReviews": [
    {
      "id": "...",
      "ethicsNumber": "2026/0008",
      "studyTitle": "...",
      "decision": "PENDING",
      "submissionDate": "2026-05-08"
    }
  ]
}
```

**Erreurs** : 401, 403

---

## Codes d'erreur communs

| Code HTTP | Signification |
|-----------|---------------|
| 400 | Données de requête invalides (contrainte @Valid échouée) |
| 401 | Token JWT absent, expiré ou invalide |
| 403 | Rôle insuffisant pour cette opération |
| 404 | Ressource introuvable (ou appartient à un autre tenant) |
| 409 | Conflit (ex: avis déjà en ordre du jour, rapport déjà soumis) |
| 500 | Erreur interne (ex: échec génération PDF) |

Toutes les erreurs suivent le format **RFC 7807** (`ProblemDetail`) :
```json
{
  "type": "https://clinitrak.be/errors/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "EthicsReview with id 'xxx' not found for tenant 'saintluc'",
  "instance": "/api/v1/ethics/reviews/xxx"
}
```

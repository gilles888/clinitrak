# CliniTrak — Schéma de base de données

*Dernière mise à jour : 2026-05-09 (Session 1 — V1 + V2 Liquibase)*

## Base de données auth-service

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_auth` (dev)
**Migrations** : Liquibase (`auth-service/src/main/resources/db/changelog/`)

### Diagramme ERD (textuel)

```
tenants (1) ──────────── (*) users
    │                         │
    │                    (*) user_roles (N:N)
    │                         │
    └── (*) roles ────────────┘
              │
         (*) role_permissions (N:N)
              │
         (*) permissions

users (1) ──── (*) refresh_tokens
users (1) ──── (*) audit_logs (soft ref)
```

### Tables

#### `tenants`
| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| slug | VARCHAR(63) | NOT NULL, UNIQUE | Ex: "saintluc" |
| name | VARCHAR(255) | NOT NULL | Nom complet |
| domain | VARCHAR(255) | | Domaine personnalisé |
| db_schema | VARCHAR(63) | | Schéma PostgreSQL dédié |
| contact_email | VARCHAR(255) | | Email de contact |
| active | BOOLEAN | NOT NULL, DEFAULT TRUE | Actif/inactif |
| settings | JSONB | | Paramètres JSON |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |
| updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Contrainte** : `slug` doit matcher `^[a-z0-9][a-z0-9\-]{1,61}[a-z0-9]$`

#### `users`
| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| email | VARCHAR(255) | NOT NULL | Email (unique par tenant) |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt-12 |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| tenant_id | UUID | FK tenants(id), NOT NULL | |
| enabled | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| account_locked | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| last_login_at | TIMESTAMPTZ | | |
| failed_login_attempts | INT | NOT NULL, DEFAULT 0, >= 0 | |
| locked_until | TIMESTAMPTZ | | Expiration du lockout |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| version | BIGINT | NOT NULL | |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | |

**Contrainte unique** : `(email, tenant_id)`
**Index** : `email`, `tenant_id`

#### `roles`
| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(100) | Ex: "ROLE_INVESTIGATOR" |
| description | VARCHAR(500) | |
| tenant_id | UUID | NULL = rôle global |

**Contrainte unique** : `(name, tenant_id)`

#### `permissions`
| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| name | VARCHAR(100) | Ex: "STUDY:READ" (format RESOURCE:ACTION) |
| description | VARCHAR(500) | |
| module | VARCHAR(50) | Ex: "STUDY", "ETHICS" |

**Contrainte unique** : `name`

#### `refresh_tokens`
| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| token | VARCHAR(512) | Valeur opaque (UUID v4), UNIQUE |
| user_id | UUID | FK users(id) |
| tenant_id | UUID | |
| expires_at | TIMESTAMPTZ | |
| revoked | BOOLEAN | |
| user_agent | VARCHAR(512) | |
| ip_address | VARCHAR(45) | IPv4 ou IPv6 |

**Index** : `token`, `user_id`, `expires_at WHERE revoked = FALSE`

#### `audit_logs` (insert-only)
| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| user_id | UUID | Référence souple (pas de FK) |
| user_email | VARCHAR(255) | Dénormalisé |
| tenant_id | UUID | Référence souple |
| action | VARCHAR(100) | Ex: "AuthController.login" |
| resource_type | VARCHAR(100) | Ex: "User", "Study" |
| resource_id | VARCHAR(255) | |
| details | JSONB | |
| ip_address | VARCHAR(45) | |
| user_agent | VARCHAR(512) | |
| http_status | INT | |
| outcome | VARCHAR(20) | SUCCESS ou FAILURE |
| created_at | TIMESTAMPTZ | |

**Index** : `user_id`, `tenant_id`, `created_at DESC`, `action`
**Règle** : aucune UPDATE autorisée sur cette table

### Tables de liaison

| Table | Relation |
|-------|----------|
| `user_roles` | users (N:N) roles |
| `role_permissions` | roles (N:N) permissions |

### Données de référence (V2__seed_data.sql)

- **1 tenant** : Saint-Luc (`id: 00000000-0000-0000-0000-000000000001`)
- **11 rôles système** (tenant_id = NULL) : SUPER_ADMIN → EXTERNAL
- **23 permissions** réparties en 7 modules : STUDY, ETHICS, CTC, PHARMACY, BILLING, DOCUMENT, ADMIN
- Attributions RBAC complètes par rôle

### Triggers

- `trg_tenants_updated_at` : met à jour `updated_at` automatiquement
- `trg_users_updated_at` : idem
- `trg_refresh_tokens_updated_at` : idem

### Extensions PostgreSQL

- `pgcrypto` : pour `gen_random_uuid()`
- `uuid-ossp` : UUID generation

---

## Base de données study-service

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_study`
**Port étude** : 8082
**Migrations** : Liquibase (`study-service/src/main/resources/db/changelog/`)

### Tables

#### `clinical_studies`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_number | VARCHAR(50) | NOT NULL | Format ST-YYYY-NNNNN |
| ethics_number | VARCHAR(100) | NULLABLE | Numéro Comité d'Éthique |
| eudract_number | VARCHAR(100) | NULLABLE | Numéro EudraCT |
| ctis_number | VARCHAR(100) | NULLABLE | Numéro CTIS |
| title | VARCHAR(500) | NOT NULL | Titre complet de l'étude |
| acronym | VARCHAR(50) | NULLABLE | Acronyme de l'étude |
| study_type | VARCHAR(30) | NOT NULL | INTERVENTIONAL / OBSERVATIONAL / EXPANDED_ACCESS |
| sponsor_type | VARCHAR(30) | NOT NULL | ACADEMIC / COMMERCIAL / INSTITUTIONAL |
| sponsor | VARCHAR(255) | | Nom du promoteur |
| principal_investigator | VARCHAR(255) | | Nom de l'investigateur principal |
| therapeutic_area | VARCHAR(100) | | Aire thérapeutique |
| phase | VARCHAR(20) | | PHASE_1 / PHASE_2 / PHASE_3 / PHASE_4 / NA |
| current_status | VARCHAR(30) | NOT NULL, DEFAULT DRAFT | Statut courant |
| start_date | DATE | | Date de début prévue |
| end_date | DATE | | Date de fin prévue |
| approval_date | DATE | | Date d'approbation CE |
| target_enrollment | INT | | Nombre cible de patients |
| current_enrollment | INT | DEFAULT 0 | Nombre actuel de patients inclus |
| is_sponsor_cusl | BOOLEAN | DEFAULT FALSE | Promoteur CUSL (institution) |
| description | TEXT | | Description / résumé du protocole |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |
| updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |
| created_by | VARCHAR(255) | | Email du créateur |
| updated_by | VARCHAR(255) | | Email du dernier modificateur |

**Contrainte unique** : `(study_number, tenant_id)`, `(eudract_number, tenant_id)` (si non null), `(ctis_number, tenant_id)` (si non null)
**Index** : `tenant_id`, `current_status`, `study_number`, `therapeutic_area`

#### `study_status_history`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | |
| study_id | UUID | FK clinical_studies(id), NOT NULL | |
| tenant_id | UUID | NOT NULL | |
| previous_status | VARCHAR(30) | NULLABLE | NULL pour la création |
| new_status | VARCHAR(30) | NOT NULL | |
| changed_at | TIMESTAMPTZ | NOT NULL | |
| changed_by | VARCHAR(255) | | Email de l'utilisateur |
| comment | TEXT | | Commentaire optionnel |

**Index** : `study_id`, `changed_at DESC`
**Règle** : insert-only (aucune UPDATE autorisée)

#### `study_contacts`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | |
| study_id | UUID | FK clinical_studies(id), NOT NULL | |
| tenant_id | UUID | NOT NULL | |
| contact_type | VARCHAR(50) | NOT NULL | PRINCIPAL_INVESTIGATOR / CO_INVESTIGATOR / etc. |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| email | VARCHAR(255) | | |
| phone | VARCHAR(50) | | |
| institution | VARCHAR(255) | | |
| department | VARCHAR(255) | | |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Index** : `study_id`, `tenant_id`

#### `submissions`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | |
| study_id | UUID | FK clinical_studies(id), NOT NULL | |
| tenant_id | UUID | NOT NULL | |
| submission_type | VARCHAR(30) | NOT NULL | INITIAL / AMENDMENT / ANNUAL_REVIEW / etc. |
| submission_status | VARCHAR(30) | NOT NULL, DEFAULT PENDING | |
| submitted_at | DATE | | Date de soumission |
| reviewed_at | DATE | | Date de décision |
| reference | VARCHAR(100) | | Référence de la soumission |
| comment | TEXT | | Commentaire / décision |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Index** : `study_id`, `tenant_id`, `submission_status`

#### `study_patients`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | |
| study_id | UUID | FK clinical_studies(id), NOT NULL | |
| tenant_id | UUID | NOT NULL | |
| patient_code | VARCHAR(100) | NOT NULL | Code pseudonyme (ex: EVAL-XY-001) |
| screening_number | VARCHAR(50) | | Numéro de screening |
| patient_status | VARCHAR(30) | NOT NULL, DEFAULT SCREENED | |
| enrolled_at | DATE | | Date d'inclusion |
| completed_at | DATE | | Date de completion |
| withdrawn_at | DATE | | Date de retrait |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| updated_at | TIMESTAMPTZ | NOT NULL | |

**Contrainte unique** : `(patient_code, study_id)` — un code pseudonyme est unique par étude
**Index** : `study_id`, `tenant_id`, `patient_status`

### Convention de numérotation

Format : `ST-{YYYY}-{NNNNN}` (ex: `ST-2026-00001`)

Généré automatiquement si non fourni à la création. La séquence est gérée par une séquence PostgreSQL par tenant et par année. Le numéro est unique par tenant.

### Données RGPD — table study_patients

- Aucune donnée identifiante (pas de nom, prénom, date de naissance, numéro de registre national)
- Seul le code pseudonyme (`patient_code`) est stocké
- Conforme au RGPD article 89 (traitement à des fins de recherche scientifique)
- La table de correspondance pseudonyme ↔ identité est gérée exclusivement par l'investigateur

### Diagramme ERD (textuel)

```
clinical_studies (1) ──── (*) study_status_history
clinical_studies (1) ──── (*) study_contacts
clinical_studies (1) ──── (*) submissions
clinical_studies (1) ──── (*) study_patients
```

---

## Base de données ethics-service

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_ethics`
**Port service** : 8083
**Migrations** : Liquibase (`ethics-service/src/main/resources/db/changelog/`)

### Diagramme ERD (textuel)

```
ethics_sequences (1 par tenant+année) ← utilisée par SequenceGeneratorService

ethics_reviews (1) ──── (*) meeting_agenda_items
ethics_reviews (1) ──── (*) annual_reports
ethics_reviews (1) ──── (*) correspondence

meetings (1) ──── (*) meeting_agenda_items
meeting_agenda_items → ethics_reviews (FK)

correspondence_templates (1) ──── (*) correspondence
```

### Table `ethics_sequences` (verrou pessimiste)

Table dédiée à la génération thread-safe des numéros CE (format `YYYY/NNNN`).

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL | Isolation multi-tenant |
| year | INT | NOT NULL | Année de la séquence (ex: 2026) |
| last_sequence | INT | NOT NULL, DEFAULT 0 | Dernier numéro attribué |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |
| updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Contrainte unique** : `(tenant_id, year)` — une seule séquence par tenant et par année

**Fonctionnement du verrou pessimiste** :
Le `SequenceGeneratorService` ouvre une transaction avec niveau d'isolation `SERIALIZABLE`
et exécute `SELECT ... FOR UPDATE` (`@Lock(LockModeType.PESSIMISTIC_WRITE)` JPA).
Cela garantit qu'une seule transaction à la fois peut incrémenter la séquence,
éliminant tout risque de doublons même sous forte concurrence.

### Table `ethics_reviews` (table principale)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service (pas de FK cross-service) |
| ethics_number | VARCHAR(20) | NOT NULL, UNIQUE par tenant | Format YYYY/NNNN (ex: 2026/0042) |
| review_type | VARCHAR(30) | NOT NULL | INITIAL, AMENDMENT, ANNUAL_REVIEW, etc. |
| decision | VARCHAR(40) | NOT NULL, DEFAULT 'PENDING' | PENDING, FAVORABLE, UNFAVORABLE, etc. |
| submission_date | DATE | NOT NULL | Date de soumission du dossier |
| decision_date | DATE | NULLABLE | Date de la décision CE |
| rapporteur_name | VARCHAR(255) | NULLABLE | Nom du rapporteur désigné |
| comments | TEXT | NULLABLE | Commentaires de soumission |
| decision_comments | TEXT | NULLABLE | Commentaires de décision |
| meeting_id | UUID | FK meetings(id), NULLABLE | Réunion lors de laquelle la décision a été prise |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |
| updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |
| created_by | VARCHAR(255) | | Email du créateur |
| updated_by | VARCHAR(255) | | Email du dernier modificateur |

**Contrainte unique** : `(ethics_number, tenant_id)`
**Index** : `tenant_id`, `study_id`, `decision`, `submission_date DESC`

### Table `meetings`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| tenant_id | UUID | NOT NULL, INDEX |
| meeting_date | DATE | NOT NULL |
| meeting_time | TIME | NULLABLE |
| meeting_type | VARCHAR(30) | ORDINARY, EXTRAORDINARY, WRITTEN_PROCEDURE |
| status | VARCHAR(20) | PLANNED, IN_PROGRESS, COMPLETED, CANCELLED |
| location | VARCHAR(500) | NULLABLE |
| notes | TEXT | NULLABLE |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE |
| created_at / updated_at | TIMESTAMPTZ | Auto |

### Table `meeting_agenda_items`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| meeting_id | UUID | FK meetings(id), NOT NULL |
| review_id | UUID | FK ethics_reviews(id), NOT NULL |
| tenant_id | UUID | NOT NULL |
| order | INT | NOT NULL — ordre dans l'ordre du jour |
| estimated_duration_minutes | INT | NULLABLE |
| notes | TEXT | NULLABLE |
| created_at | TIMESTAMPTZ | Auto |

**Contrainte unique** : `(meeting_id, review_id)` — un avis ne peut figurer qu'une fois par réunion

### Table `annual_reports`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| tenant_id | UUID | NOT NULL |
| study_id | UUID | NOT NULL — référence study-service |
| review_id | UUID | FK ethics_reviews(id) — avis CE initial associé |
| ethics_number | VARCHAR(20) | Numéro CE (dénormalisé) |
| report_year | INT | NOT NULL — année du rapport |
| due_date | DATE | NOT NULL — date d'échéance |
| status | VARCHAR(20) | PENDING, SUBMITTED, VALIDATED, OVERDUE |
| submission_date | DATE | NULLABLE |
| submission_comments | TEXT | NULLABLE |
| reminder_60_sent | BOOLEAN | DEFAULT FALSE — rappel J-60 envoyé |
| reminder_30_sent | BOOLEAN | DEFAULT FALSE — rappel J-30 envoyé |
| reminder_0_sent | BOOLEAN | DEFAULT FALSE — rappel J-0 envoyé |
| deleted | BOOLEAN | DEFAULT FALSE |
| created_at / updated_at | TIMESTAMPTZ | Auto |

**Index** : `tenant_id`, `due_date`, `status`

### Table `correspondence_templates`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| tenant_id | UUID | NOT NULL (ou NULL = template global) |
| template_type | VARCHAR(40) | FAVORABLE_OPINION, UNFAVORABLE_OPINION, etc. |
| name | VARCHAR(255) | NOT NULL — nom du template |
| language | VARCHAR(5) | DEFAULT 'FR' |
| html_content | TEXT | NOT NULL — template HTML avec variables Thymeleaf |
| active | BOOLEAN | DEFAULT TRUE |
| created_at / updated_at | TIMESTAMPTZ | Auto |

**Note** : Les templates HTML sont stockés directement en base de données.
Le `TemplateEngineService` utilise `StringTemplateResolver` de Thymeleaf pour les rendre
(sans accès fichier disque). La migration Liquibase V2 insère 4 templates FR initiaux.

### Table `correspondence`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| tenant_id | UUID | NOT NULL |
| review_id | UUID | FK ethics_reviews(id), NOT NULL |
| template_id | UUID | FK correspondence_templates(id), NOT NULL |
| rendered_html | TEXT | NOT NULL — HTML rendu (Thymeleaf) |
| pdf_bytes | BYTEA | NULLABLE — PDF généré (Flying Saucer) |
| generated_at | TIMESTAMPTZ | NOT NULL |
| generated_by | VARCHAR(255) | Email du générateur |
| created_at | TIMESTAMPTZ | Auto |

**Index** : `tenant_id`, `review_id`

---

---

## Base de données ctc-service (clinitrak_ctc)

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_ctc`
**Port service** : 8084
**Migrations** : Liquibase (`ctc-service/src/main/resources/db/changelog/`)

### Diagramme ERD (textuel)

```
ctc_desk_requests (1) ──── (*) [liée à clinical_studies via study_id cross-service]
ctc_monitoring_visits (1) ──── (*) [liée à clinical_studies via study_id cross-service]
ctc_financial_contracts (1) ──── (*) [liée à clinical_studies via study_id cross-service]
ctc_statistics_requests (1) ──── (*) [liée à clinical_studies via study_id cross-service]
ctc_sponsor_studies (1) ──── (*) ctc_sponsor_cra_assignments
ctc_quality_events (1) ──── (*) [liée à clinical_studies via study_id cross-service]
```

### Table `ctc_desk_requests` (16 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service (pas de FK cross-service) |
| desk_type | VARCHAR(20) | NOT NULL | ACADEMIC / COMMERCIAL |
| requestor_name | VARCHAR(255) | NOT NULL | Nom du demandeur |
| requestor_email | VARCHAR(255) | NOT NULL | Email du demandeur |
| requestor_organization | VARCHAR(255) | | Organisation du demandeur |
| request_type | VARCHAR(30) | NOT NULL | NEW_STUDY / AMENDMENT / EXTENSION / CLOSURE |
| status | VARCHAR(30) | NOT NULL, DEFAULT 'PENDING' | PENDING / ASSIGNED / IN_PROGRESS / COMPLETED / REJECTED |
| priority | VARCHAR(20) | NOT NULL, DEFAULT 'MEDIUM' | LOW / MEDIUM / HIGH / URGENT |
| assigned_to | VARCHAR(255) | | Responsable assigné |
| deadline | DATE | | Date limite de traitement |
| notes | TEXT | | Notes libres |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `study_id`, `status`, `priority`

### Table `ctc_monitoring_visits` (11 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| visit_date | DATE | NOT NULL | Date de la visite de monitoring |
| visit_type | VARCHAR(30) | NOT NULL | INITIATION / ROUTINE / CLOSE_OUT |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PLANNED' | PLANNED / COMPLETED / CANCELLED |
| monitor_name | VARCHAR(255) | | Nom du moniteur |
| correction_deadline | DATE | | Date limite de correction des findings |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Note** : La colonne `version` (optimistic locking) est héritée de BaseEntity — non comptée dans les 11 colonnes listées.
**Index** : `tenant_id`, `study_id`, `visit_date`, `status`

### Table `ctc_financial_contracts` (12 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| contract_type | VARCHAR(30) | NOT NULL | CONVENTION / AMENDMENT / SPONSOR |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | DRAFT / ACTIVE / COMPLETED / CANCELLED |
| contract_date | DATE | NOT NULL | Date de signature du contrat |
| amount | NUMERIC(15,2) | NOT NULL | Montant du contrat |
| currency | VARCHAR(10) | NOT NULL, DEFAULT 'EUR' | Devise (ex: EUR, USD) |
| billing_schedule | VARCHAR(30) | | Calendrier de facturation |
| payment_terms | TEXT | | Conditions de paiement |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `study_id`, `status`, `contract_date`

### Table `ctc_statistics_requests` (11 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| requestor_name | VARCHAR(255) | NOT NULL | Nom du demandeur |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | Statut de la demande statistique |
| deadline | DATE | | Date limite souhaitée |
| analysis_type | VARCHAR(30) | NOT NULL | DESCRIPTIVE / INFERENTIAL / SURVIVAL / LONGITUDINAL |
| data_format | VARCHAR(30) | | Format de données attendu |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `study_id`, `status`

### Table `ctc_sponsor_studies` (10 colonnes) + table `ctc_sponsor_cra_assignments`

#### `ctc_sponsor_studies`

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL, UNIQUE par tenant | Référence vers study-service |
| project_manager_id | UUID | | Responsable de projet assigné |
| budget_total | NUMERIC(15,2) | | Budget total alloué |
| budget_consumed | NUMERIC(15,2) | DEFAULT 0 | Budget consommé à date |
| regulatory_status | VARCHAR(30) | NOT NULL | IN_PREPARATION / SUBMITTED / APPROVED / ONGOING / COMPLETED |
| milestones | JSONB | | Jalons flexibles (format libre) |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Note** : Le champ `milestones` est de type JSONB pour permettre une structure flexible sans migration de schéma.
**Contrainte unique** : `(study_id, tenant_id)`
**Index** : `tenant_id`, `study_id`, `regulatory_status`

#### `ctc_sponsor_cra_assignments`

| Colonne | Type | Description |
|---------|------|-------------|
| id | UUID | PK |
| sponsor_study_id | UUID | FK ctc_sponsor_studies(id), NOT NULL |
| cra_user_id | UUID | Identifiant du CRA (référence souple vers auth-service) |
| cra_name | VARCHAR(255) | Nom dénormalisé du CRA |
| assigned_at | DATE | Date d'assignation |
| created_at | TIMESTAMPTZ | Auto |

**Index** : `sponsor_study_id`

### Table `ctc_quality_events` (13 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| event_type | VARCHAR(30) | NOT NULL | DEVIATION / SAE / CAPA / AUDIT |
| event_date | DATE | NOT NULL | Date de survenue de l'événement |
| severity | VARCHAR(20) | NOT NULL | LOW / MEDIUM / HIGH / CRITICAL |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'OPEN' | OPEN / IN_PROGRESS / CLOSED / CANCELLED |
| description | TEXT | NOT NULL | Description détaillée |
| corrective_action | TEXT | | Action corrective mise en place |
| closure_date | DATE | | Date de clôture |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `study_id`, `severity`, `status`, `event_date DESC`

---

---

## Base de données pharmacy-service (clinitrak_pharmacy)

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_pharmacy`
**Port service** : 8085
**Migrations** : Liquibase (`pharmacy-service/src/main/resources/db/changelog/`)

### Diagramme ERD (textuel)

```
pharmacy_drugs (1) ──── (*) pharmacy_stocks
pharmacy_drugs (1) ──── (*) pharmacy_dispensations
pharmacy_drugs (1) ──── (*) pharmacy_billing
pharmacy_drugs (1) ──── (*) pharmacy_emergency_unblinding (via patientCode cross-service)
```

### Table `pharmacy_drugs` (16 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service (pas de FK cross-service) |
| drug_name | VARCHAR(255) | NOT NULL | Nom commercial du médicament |
| inn | VARCHAR(255) | NULLABLE | Dénomination commune internationale |
| dosage | VARCHAR(100) | NULLABLE | Ex: 500mg, 10mg/ml |
| form | VARCHAR(30) | NOT NULL | TABLET / CAPSULE / INJECTION / SOLUTION / CREAM / OTHER |
| manufacturer | VARCHAR(255) | NULLABLE | Nom du fabricant |
| batch_number | VARCHAR(100) | NULLABLE | Numéro de lot |
| expiry_date | DATE | NULLABLE | Date de péremption |
| storage_conditions | VARCHAR(500) | NULLABLE | Conditions de conservation |
| category | VARCHAR(20) | NOT NULL | IMP / NIMP / PLACEBO |
| regulatory_status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | PENDING / APPROVED / EXPIRED / RECALLED |
| randomization_code_encrypted | VARCHAR(512) | NULLABLE | randomizationCode chiffré AES-256 (clé PHARMACY_ENCRYPTION_KEY) |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Note chiffrement** : `randomization_code_encrypted` contient le code de randomisation chiffré via AES/ECB/PKCS5Padding. La clé de 32 bytes est fournie par la variable d'environnement `PHARMACY_ENCRYPTION_KEY`. Le déchiffrement n'est effectué qu'en cas de levée d'aveugle d'urgence approuvée.
**Index** : `tenant_id`, `study_id`, `category`, `regulatory_status`

### Table `pharmacy_stocks` (16 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| drug_id | UUID | FK pharmacy_drugs(id), NOT NULL | Médicament concerné |
| study_id | UUID | NOT NULL | Référence vers study-service |
| quantity | INT | NOT NULL, >= 0 | Quantité en stock |
| unit | VARCHAR(50) | NOT NULL | Unité (comprimés, flacons, etc.) |
| received_date | DATE | NOT NULL | Date de réception |
| expiry_date | DATE | NOT NULL | Date de péremption du lot |
| batch_number | VARCHAR(100) | NOT NULL | Numéro de lot reçu |
| location | VARCHAR(255) | NULLABLE | Emplacement de stockage (armoire, réfrigérateur…) |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'QUARANTINE' | QUARANTINE → AVAILABLE → DISPENSED / RETURNED / DESTROYED |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Cycle de vie des statuts** : Tout stock réceptionné démarre en `QUARANTINE`. Après libération qualité, il passe à `AVAILABLE`. Il peut ensuite être `DISPENSED` (dispensé à un patient), `RETURNED` (retourné), ou `DESTROYED`.
**Index** : `tenant_id`, `drug_id`, `study_id`, `status`, `expiry_date`

### Table `pharmacy_dispensations` (16 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| drug_id | UUID | FK pharmacy_drugs(id), NOT NULL | Médicament dispensé |
| stock_id | UUID | FK pharmacy_stocks(id), NOT NULL | Lot de stock utilisé |
| study_id | UUID | NOT NULL | Référence vers study-service |
| patient_code | VARCHAR(100) | NOT NULL | Code pseudonyme patient (RGPD) |
| dispensation_date | DATE | NOT NULL | Date de dispensation |
| pharmacist_id | UUID | NOT NULL | Identifiant du pharmacien dispensateur |
| prescriber_id | UUID | NOT NULL | Identifiant du médecin prescripteur |
| quantity | INT | NOT NULL, > 0 | Quantité dispensée |
| prescription | VARCHAR(500) | NULLABLE | Référence ordonnance |
| visit_number | VARCHAR(50) | NULLABLE | Numéro de visite de l'essai |
| notes | TEXT | NULLABLE | Notes libres |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Décrémentation automatique** : Lors de la création d'une dispensation, le `DispensationService` décrémente automatiquement la `quantity` du stock correspondant dans `pharmacy_stocks`. Si le stock est épuisé après dispensation, le statut passe à `DISPENSED`.
**Index** : `tenant_id`, `drug_id`, `study_id`, `patient_code`, `dispensation_date DESC`

### Table `pharmacy_billing` (12 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| drug_id | UUID | FK pharmacy_drugs(id), NOT NULL | Médicament facturé |
| dispensation_id | UUID | FK pharmacy_dispensations(id), NULLABLE | Dispensation associée |
| amount | NUMERIC(15,2) | NOT NULL | Montant facturé |
| currency | VARCHAR(10) | NOT NULL, DEFAULT 'EUR' | Devise |
| billing_status | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | DRAFT / SENT / PAID / DISPUTED / CANCELLED |
| invoice_reference | VARCHAR(100) | NULLABLE | Référence de la facture |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `study_id`, `billing_status`

### Table `pharmacy_emergency_unblinding` (13 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant |
| study_id | UUID | NOT NULL | Référence vers study-service |
| drug_id | UUID | FK pharmacy_drugs(id), NOT NULL | Médicament concerné |
| patient_code | VARCHAR(100) | NOT NULL | Code pseudonyme patient |
| requested_by | UUID | NOT NULL | Identifiant du demandeur |
| approved_by | UUID | NULLABLE | Identifiant de l'approbateur (NULL jusqu'à approbation) |
| reason | TEXT | NOT NULL | Justification médicale de la levée d'aveugle |
| treatment | VARCHAR(512) | NULLABLE | Traitement révélé après déchiffrement AES (NULL jusqu'à approve) |
| approved_at | TIMESTAMPTZ | NULLABLE | Horodatage de l'approbation |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Processus en 2 étapes** :
1. `POST /emergency-unblinding` : crée l'enregistrement avec `treatment = NULL` et `approved_by = NULL`
2. `PATCH /emergency-unblinding/{id}/approve` : l'`EmergencyUnblindingService` déchiffre `randomization_code_encrypted` via AES, renseigne `treatment` et `approved_by`, et horodate `approved_at`

**Index** : `tenant_id`, `study_id`, `patient_code`, `approved_at`

---

## Base de données exchange-service (clinitrak_exchange)

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_exchange`
**Port service** : 8086
**Migrations** : Liquibase (`exchange-service/src/main/resources/db/changelog/`)

### Table `exchange_external_users` (10 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email de l'utilisateur externe |
| password_hash | VARCHAR(255) | NOT NULL | BCrypt-12 |
| first_name | VARCHAR(100) | NOT NULL | |
| last_name | VARCHAR(100) | NOT NULL | |
| organization | VARCHAR(255) | | Organisation externe |
| role | VARCHAR(30) | NOT NULL | COMPANY / INVESTIGATOR / CE_REQUESTOR |
| verified_email | BOOLEAN | NOT NULL, DEFAULT FALSE | Vérification email requise avant login |
| email_verification_token | VARCHAR(512) | NULLABLE | Token généré à l'inscription, NULLé après vérification |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `email`, `email_verification_token`

### Table `exchange_requests` (13 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Isolation multi-tenant (côté interne) |
| external_user_id | UUID | FK exchange_external_users(id), NOT NULL | Propriétaire de la demande |
| target_module | VARCHAR(10) | NOT NULL | CE / CTC |
| request_type | VARCHAR(30) | NOT NULL | NEW_STUDY / AMENDMENT / EXTENSION / INFORMATION |
| title | VARCHAR(255) | NOT NULL | Titre de la demande |
| description | TEXT | | Description détaillée |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'DRAFT' | DRAFT / SUBMITTED / UNDER_REVIEW / ACCEPTED / REJECTED / MORE_INFO |
| study_id | UUID | NULLABLE | Lien vers study-service après acceptation (pas de FK cross-service) |
| submitted_at | TIMESTAMPTZ | NULLABLE | Horodatage de la soumission |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| version | BIGINT | NOT NULL | Optimistic locking |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Index** : `tenant_id`, `external_user_id`, `status`, `submitted_at DESC`

### Table `exchange_documents` (9 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| request_id | UUID | FK exchange_requests(id), NOT NULL | Demande associée |
| file_name | VARCHAR(255) | NOT NULL | Nom du fichier |
| file_path | VARCHAR(1000) | NOT NULL | Chemin de stockage (MinIO ou filesystem) |
| file_size | BIGINT | | Taille en octets |
| content_type | VARCHAR(100) | | MIME type |
| uploaded_by | VARCHAR(255) | | Email ou identifiant de l'uploadeur |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |

**Index** : `request_id`

### Table `exchange_messages` (10 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| request_id | UUID | FK exchange_requests(id), NOT NULL | Demande associée |
| sender_type | VARCHAR(10) | NOT NULL | INTERNAL / EXTERNAL |
| sender_id | VARCHAR(255) | NOT NULL | UUID ou email de l'expéditeur |
| sender_name | VARCHAR(255) | | Nom affiché de l'expéditeur |
| content | TEXT | NOT NULL | Corps du message |
| attachment_path | VARCHAR(1000) | NULLABLE | Pièce jointe optionnelle |
| read_at | TIMESTAMPTZ | NULLABLE | NULL = non lu |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at | TIMESTAMPTZ | NOT NULL | Auto |

**Index** : `request_id`, `sender_type`, `read_at WHERE read_at IS NULL`

---

## Base de données admin-service (clinitrak_admin)

**SGBD** : PostgreSQL 16
**Nom** : `clinitrak_admin`
**Port service** : 8091
**Migrations** : Liquibase (`admin-service/src/main/resources/db/changelog/`)

### Table `admin_tenants` (11 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| name | VARCHAR(255) | NOT NULL | Nom complet du tenant |
| slug | VARCHAR(63) | NOT NULL, UNIQUE | Identifiant URL (ex: saintluc) |
| domain | VARCHAR(255) | NULLABLE | Domaine personnalisé |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE / INACTIVE / SUSPENDED |
| subscription_type | VARCHAR(20) | NOT NULL | BASIC / PROFESSIONAL / ENTERPRISE |
| configuration | JSONB | | Paramètres flexibles (timezone, ceNumberFormat, defaultLanguage, maxUsers, storageQuotaGb) |
| max_users | INT | | Nombre maximum d'utilisateurs autorisés |
| storage_quota_gb | INT | | Quota de stockage en Go |
| deleted | BOOLEAN | NOT NULL, DEFAULT FALSE | Suppression logique |
| created_at / updated_at | TIMESTAMPTZ | NOT NULL | Auto (trigger) |

**Contrainte unique** : `slug`
**Index** : `slug`, `status`, `subscription_type`

### Table `admin_tenant_active_modules` (2 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| tenant_id | UUID | FK admin_tenants(id), NOT NULL | Tenant concerné |
| module_type | VARCHAR(30) | NOT NULL | STUDIES / ETHICS / CTC / PHARMACY / EXCHANGE / BILLING / DOCUMENTS |

**Clé primaire composée** : `(tenant_id, module_type)`
**Index** : `tenant_id`

### Table `admin_audit_logs` (11 colonnes)

| Colonne | Type | Contraintes | Description |
|---------|------|-------------|-------------|
| id | UUID | PK, DEFAULT gen_random_uuid() | Identifiant unique |
| tenant_id | UUID | NOT NULL, INDEX | Tenant concerné (référence souple) |
| user_id | UUID | NULLABLE | Utilisateur concerné (référence souple) |
| user_email | VARCHAR(255) | | Email dénormalisé |
| action | VARCHAR(100) | NOT NULL | Action effectuée (ex: TENANT_CREATED, USER_INVITED) |
| resource_type | VARCHAR(100) | | Type de ressource (ex: Tenant, User) |
| resource_id | VARCHAR(255) | | Identifiant de la ressource |
| details | JSONB | | Détails supplémentaires |
| ip_address | VARCHAR(45) | | IPv4 ou IPv6 |
| performed_by | VARCHAR(255) | | Email du super-admin ayant effectué l'action |
| created_at | TIMESTAMPTZ | NOT NULL | Auto — insert-only, pas de soft-delete |

**Index** : `tenant_id`, `user_id`, `action`, `tenant_id + created_at DESC` (index composite pour export)
**Règle** : insert-only — aucune UPDATE ni DELETE autorisée sur cette table

---

## Convention pour les nouveaux services

Chaque nouveau service doit avoir sa propre base de données :
- Dev : `clinitrak_<module>` (ex: `clinitrak_study`)
- Toutes les tables héritent des colonnes `BaseEntity` :
  `id (UUID), created_at, updated_at, created_by, updated_by, deleted, version`

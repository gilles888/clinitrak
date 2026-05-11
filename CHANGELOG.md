# CliniTrak — Changelog

Toutes les modifications notables du projet sont documentées ici.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [Unreleased]

*(Prochaines modifications en cours)*

---

## [0.5.0] — 2026-05-11

### Session 5 — pharmacy-service complet + module Angular Pharmacie

**Agents impliqués** : backend, frontend, devops (parallèle)

#### Ajouté — pharmacy-service (backend)

- `pharmacy-service/pom.xml` : Apache POI 5.2.5, Flying Saucer 9.4.0, Spring Mail
- **Enums** (6) : DrugCategory, DrugRegulatoryStatus, StockStatus, DrugForm, BillingStatus, AlertType
- **Entités JPA** (5) : InvestigationalDrug (randomizationCode AES-256), DrugStock, Dispensation, PharmacyBilling, EmergencyUnblinding
- **Repositories** (5) : findLowStock @Query, requêtes expiry date
- **Services** (7) : EncryptionService (AES), DrugService, DrugStockService (import CSV/XLSX Apache POI), DispensationService (décrémente stock), EmergencyUnblindingService (double validation + déchiffrement), PharmacyAlertService (@Scheduled J-7/J-30/stock), PdfReportService (Flying Saucer), PharmacyDashboardService
- **Controllers** (4) : DrugController, StockController (+ import multipart), DispensationController, DashboardController (dashboard + alerts + PDF + unblinding)
- **Liquibase** : V1__init_pharmacy.sql (5 tables, 8 index, 5 triggers)
- **Tests** : DrugServiceTest, DispensationServiceTest, PharmacyDrugControllerTest

#### Ajouté — clinitrak-frontend (Angular)

- `core/models/pharmacy.model.ts` : 6 enums + 7 interfaces + severity maps + options dropdown
- `core/services/pharmacy.service.ts` : 11 méthodes HTTP
- `features/pharmacy/pharmacy.routes.ts` : 7 routes lazy-loaded
- `pharmacy-dashboard/` : 6 KPIs + alertes urgentes
- `drug-stock/` : p-table + coloration péremption CSS + dialog statut
- `dispensation-form/` : ReactiveForm + historique patient
- `stock-receipt/` : saisie manuelle + import fichier natif HTML5
- `expiry-alert/` : alertes groupées p-message + p-badge par type
- `emergency-unblinding/` : dialog 2 étapes + confirmation sécurisée + révélation traitement
- `pharmacy-report/` : téléchargement PDF blob

#### Ajouté — DevOps

- `pharmacy-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8085)
- `docker-compose.yml` : service `clinitrak-pharmacy` avec PHARMACY_ENCRYPTION_KEY
- `docs/api/pharmacy-service.md` : 12 endpoints + enums + fonctionnalités spéciales
- `docs/database/schema.md` : 5 tables pharmacy-service
- `.env.example` : PHARMACY_DB_NAME, PHARMACY_ENCRYPTION_KEY, LOW_STOCK_THRESHOLD

---

## [0.4.0] — 2026-05-10

### Session 4 — ctc-service complet + module Angular CTC

**Agents impliqués** : backend, frontend, devops (parallèle)

#### Ajouté — ctc-service (backend)

- `ctc-service/pom.xml` : dépendances complètes (jjwt, MapStruct, Feign, TestContainers)
- **Enums** (17) : DeskType, RequestType, RequestStatus, Priority, VisitType, VisitStatus, ContractType, ContractStatus, Currency, BillingSchedule, AnalysisType, DataFormat, StatisticsStatus, EventType, Severity, EventStatus, RegulatoryStatus
- **Entités JPA** (6) : TrialDeskRequest, MonitoringVisit, FinancialContract, StatisticsRequest, SponsorCUSLStudy (budgets + milestones JSONB), QualityEvent
- **Repositories** (6) : avec requêtes JPQL filtrées par tenantId et deleted=false
- **DTOs records** (14) : create/response/assign/statusUpdate par entité
- **CtcMapper** (MapStruct) : mapping entités ↔ DTOs avec labels enum
- **Services** (8) : TrialDeskRequestService, MonitoringVisitService, FinancialContractService, StatisticsRequestService, SponsorStudyService, QualityEventService, CTCDashboardService, StudyTimelineService
- **Controllers** (5) : TrialDeskRequestController, MonitoringVisitController, FinancialContractController, StatisticsRequestController, CTCDashboardController (dashboard + sponsor + quality + timeline)
- **Feign Client** : StudyServiceClient vers study-service (JWT + X-Tenant-ID)
- **Liquibase** : V1__init_ctc.sql (6 tables + 1 table de jointure, index, triggers)
- **Tests** : TrialDeskRequestServiceTest (Mockito), QualityEventRepositoryTest (TestContainers), TrialDeskRequestControllerTest (MockMvc)

#### Ajouté — clinitrak-frontend (Angular)

- `core/models/ctc.model.ts` : 16 enums + 8 interfaces + severity maps + dropdown options
- `core/services/ctc.service.ts` : 16 méthodes HTTP
- `features/ctc/ctc.routes.ts` : 9 routes lazy-loaded
- `features/ctc/ctc-dashboard/` : KPIs + demandes récentes
- `features/ctc/desk-request-list/` : vue kanban par statut + dialogs création/assignation
- `features/ctc/study-timeline/` : PrimeNG p-timeline avec icônes et couleurs par sévérité
- `features/ctc/monitoring-plan/` : p-table + filtres signaux computed
- `features/ctc/financial-dashboard/` : p-chart doughnut + bar + tableau contrats
- `features/ctc/quality-tracker/` : KPIs alertes + table + bannière critique
- `features/ctc/sponsor-study-dashboard/` : barre progression budget + statut réglementaire
- `features/ctc/statistics-request-form/` : tableau + dialog formulaire
- `features/ctc/ctc-study-detail/` : p-tabView 5 onglets CTC par étude

#### Ajouté — DevOps

- `ctc-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8084)
- `docker-compose.yml` : service `clinitrak-ctc` avec healthcheck
- `docs/api/ctc-service.md` : documentation 15 endpoints
- `docs/database/schema.md` : 6 tables ctc-service documentées
- `.env.example` : variable CTC_DB_NAME ajoutée

---

## [0.3.0] — 2026-05-09

### Session 3 — ethics-service complet + module Angular CE

**Agents impliqués** : backend, frontend, devops (parallèle)

#### Ajouté — ethics-service (backend)

- `ethics-service/pom.xml` : + Feign Client, Thymeleaf, Flying Saucer PDF, Spring Mail
- **Enums** (6) : `ReviewType`, `ReviewDecision`, `MeetingType`, `MeetingStatus`, `AnnualReportStatus`, `TemplateType`
- **Entités JPA** (7) : `EthicsReview`, `Meeting`, `MeetingAgendaItem`, `AnnualReport`, `CorrespondenceTemplate`, `Correspondence`, `EthicsSequence`
  - `EthicsSequence` : table dédiée avec verrou pessimiste (PESSIMISTIC_WRITE) pour numérotation CE thread-safe
  - `EthicsReview` : numérotation auto format YYYY/NNNN (ex: 2026/0042)
- **Repositories** (7) avec méthodes dédiées pour reminders, verrou séquence
- **DTOs records** (11) : create/response pour reviews, meetings, agenda items, annual reports, templates, correspondence
- **Feign Client** : `StudyServiceClient` → GET /api/v1/studies/{id} + propagation JWT + X-Tenant-ID
- **`SequenceGeneratorService`** : isolation SERIALIZABLE + PESSIMISTIC_WRITE pour générer YYYY/NNNN sans collision
- **`TemplateEngineService`** : Thymeleaf StringTemplateResolver pour templates HTML stockés en base
- **`PdfGenerationService`** : Flying Saucer + OpenPDF (HTML → PDF)
- **`AnnualReportService`** : @Scheduled rappels automatiques J-60/J-30/J-0
- **Controllers** (5) : EthicsReview, Meeting, AnnualReport, Correspondence, Dashboard (16 endpoints)
- **Liquibase** : V1 (7 tables, triggers, index) + V2 (4 templates correspondance initiaux en FR)
- **Tests** : EthicsReviewServiceTest (Mockito), SequenceGeneratorServiceTest (TestContainers), EthicsReviewControllerTest (MockMvc)
- `ethics-service/Dockerfile` multi-stage

#### Ajouté — clinitrak-frontend (Angular)

- `core/models/ethics.model.ts` : 6 enums + 10 interfaces + severity mappings + options dropdowns
- `core/services/ethics.service.ts` : 16 méthodes Observable (toutes les API)
- `features/ethics/ethics.routes.ts` : 8 routes lazy-loaded
- `features/ethics/ethics-dashboard/` : KPIs + 5 derniers avis + prochaine réunion
- `features/ethics/ethics-study-list/` : p-table avis + dialog décision inline
- `features/ethics/ethics-review-form/` : p-stepper création/décision
- `features/ethics/meeting-calendar/` : FullCalendar (dayGrid + list + interaction)
- `features/ethics/meeting-detail/` : ordre du jour, présences, changement statut
- `features/ethics/correspondence-generator/` : sélection template + génération + téléchargement PDF
- `features/ethics/annual-report-tracker/` : p-table avec indicateurs visuels J-60/J-30/J-0

#### Ajouté — DevOps

- `ethics-service/Dockerfile` multi-stage (port 8083)
- `docker-compose.yml` : service `clinitrak-ethics` + `mailhog` (intercepteur email dev, UI port 8025)
- `.env.example` : variables ETHICS_DB_NAME, STUDY_SERVICE_URL, MAIL_*
- `docs/api/ethics-service.md` : documentation complète 16 endpoints + enums + PDF + Feign
- `docs/database/schema.md` : section ethics-service (7 tables)
- `docs/architecture/multi-tenant.md` : documentation stratégie multi-tenant
- `docs/architecture/auth-flow.md` : documentation flux JWT + propagation Feign

---

## [0.2.0] — 2026-05-09

### Session 2 — study-service complet + module Angular études

**Agents impliqués** : backend, frontend, devops (parallèle)

#### Ajouté — study-service (backend)

- `study-service/pom.xml` : dépendances complètes (jjwt, MapStruct, TestContainers, etc.)
- **Enums** (8) : `StudyType`, `SponsorType`, `StudyPhase`, `StudyStatus`, `ContactType`, `SubmissionType`, `SubmissionStatus`, `PatientStatus` — chacun avec label français
- **Entités JPA** (5) : `ClinicalStudy`, `StudyStatusHistory`, `StudyContact`, `Submission`, `Patient`
  - `ClinicalStudy` : numérotation auto ST-YYYY-NNNNN, 20+ champs, multi-tenant
  - `Patient` : pseudonymisé RGPD (patientCode uniquement, contrainte unique par étude)
- **Repositories** (5) : `ClinicalStudyRepository` (JpaSpecificationExecutor), + 4 autres
- **DTOs records** (14) : create/update/response/summary pour études, contacts, soumissions, patients
- **`ClinicalStudySpecification`** : 12 prédicats null-safe pour recherche multicritères
- **`StudyMapper`** (MapStruct) : avec méthodes default pour labels des enums
- **`StudyService`** : CRUD complet, génération studyNumber, statistiques, soft delete
- **`StudyController`** : 19 endpoints REST avec @PreAuthorize, @Valid, Swagger
- Sécurité : JWT validation locale (jjwt 0.12.x), TenantContext multi-tenant
- **Liquibase** : `V1__init_study.sql` (5 tables, triggers, index, séquence)
- **Tests** : `StudyServiceTest` (6 Mockito), `StudyRepositoryTest` (4 TestContainers), `StudyControllerTest` (5 MockMvc)

#### Ajouté — clinitrak-frontend (Angular)

- `core/models/study.model.ts` : tous les enums + interfaces + options dropdown
- `core/services/study.service.ts` : 15 méthodes HTTP
- `shared/components/study-status-badge/` : composant badge réutilisable (p-tag + severity mapping)
- `features/studies/studies.routes.ts` : 4 routes lazy-loaded
- `features/studies/study-list/` : recherche multicritères + p-table lazy + pagination
- `features/studies/study-detail/` : p-tabView 6 onglets (Général, Contacts, Soumissions, Patients, Historique, Documents)
- `features/studies/study-form/` : p-stepper 3 étapes (Identification → Détails → Confirmation)
- `dashboard.component.ts` : chargement des vraies statistiques depuis study-service

#### Ajouté — DevOps

- `study-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8082)
- `docker-compose.yml` : ajout service `clinitrak-study` avec healthcheck
- `scripts/init-db.sql` : création de toutes les bases de données (9 services)
- `docs/api/study-service.md` : documentation complète des 19 endpoints
- `docs/database/schema.md` : documentation des 5 tables study-service
- `.env.example` : variable STUDY_DB_NAME ajoutée

---

## [0.1.0] — 2026-05-09

### Session 1 — Initialisation complète du projet

**Agents impliqués** : backend, frontend, devops (session initiale unifiée)

#### Ajouté — Infrastructure Maven

- Structure Maven multi-modules : `clinitrak-parent` avec 12 modules enfants
  (`gateway`, `auth-service`, `study-service`, `ethics-service`, `ctc-service`,
  `pharmacy-service`, `exchange-service`, `billing-service`, `document-service`,
  `batch-service`, `notification-service`, `admin-service`)
- Parent POM avec Spring Boot 3.3.4 BOM, Java 21, gestion centralisée des versions :
  jjwt 0.12.5, MapStruct 1.5.5.Final, springdoc 2.5.0, testcontainers 1.20.1
- Profils Maven : `dev` (défaut), `staging`, `prod`
- Annotation processor Maven configuré (Lombok avant MapStruct — ordre critique)

#### Ajouté — auth-service (complet)

- `AuthServiceApplication.java` : point d'entrée avec `@EnableJpaAuditing`, `@EnableAsync`
- **Entités JPA** :
  - `BaseEntity` : UUID PK, timestamps Spring Data Auditing, soft-delete, version
  - `Tenant` : slug unique, domain, settings JSONB, contrainte de format slug
  - `User` : email unique par tenant, BCrypt, lockout (5 tentatives, 30min)
  - `Role` : 11 rôles système + rôles tenant-spécifiques possibles
  - `Permission` : format `RESOURCE:ACTION` (23 permissions initiales)
  - `RefreshToken` : rotation automatique, révocation, IP + UserAgent tracés
  - `AuditLog` : insert-only, persistance async via AOP
- **Repositories JPA** : 5 interfaces avec requêtes custom JPQL
- **DTOs immuables (records Java)** : `LoginRequest`, `LoginResponse`, `RegisterRequest`, `RefreshTokenRequest`
- **Sécurité** :
  - `JwtService` : génération + validation JWT (jjwt 0.12.x, HS256)
  - `JwtAuthenticationFilter` : `OncePerRequestFilter`, retry refresh sur 401
  - `UserDetailsServiceImpl` : username format `email:tenantId`
  - `SecurityConfig` : Spring Security 6, stateless, CORS, BCrypt-12, `@EnableMethodSecurity`
- **Multi-tenant** :
  - `TenantContext` : ThreadLocal + nettoyage garanti dans finally
  - `TenantFilter` : résolution X-Tenant-ID header (priorité 1) ou sous-domaine
  - `TenantResolver` : extraction du slug depuis le hostname
- **Configuration** :
  - `OpenApiConfig` : Springdoc OpenAPI 2.x avec Bearer JWT
  - `RedisConfig` : 4 caches TTL (tenants 1h, users 15min, roles 1h, blacklisted-tokens 15min)
- **Service** : `AuthService` avec login/refresh (rotation)/logout/register
  - Lockout automatique après 5 tentatives
  - Extraction IP réelle (X-Forwarded-For)
- **AOP** : `AuditAspect` @Around tous les @RestController, persistance @Async non-transactionnelle
- **Exceptions** : `GlobalExceptionHandler` RFC 7807 (AuthException, AccessDenied, Validation, fallback 500)
- **Controller** : `AuthController` — POST /api/v1/auth/{login,refresh,logout,register}
- **application.yml** : configuration complète dev/staging/prod avec propriétés CliniTrak custom
- **Liquibase** :
  - `V1__init.sql` : tables (tenants, users, roles, permissions, user_roles, role_permissions, refresh_tokens, audit_logs), indexes, triggers `updated_at`, contraintes
  - `V2__seed_data.sql` : tenant Saint-Luc, 11 rôles système, 23 permissions, attributions RBAC complètes
- `Dockerfile` multi-stage (builder Maven 3.9 + JRE Alpine 21, user non-root, JVM container-aware)

#### Ajouté — Module stubs (prêts à développer)

- `gateway/pom.xml` : Spring Cloud Gateway + Resilience4j + Redis reactive
- 10 modules stubs avec dépendances communes préconfigurées

#### Ajouté — Frontend Angular 20

- `package.json` : Angular 20, PrimeNG 17.18, Primeflex, Primeicons, Tailwind 3.4
- `tsconfig.json` : strict mode, paths aliases (`@core/*`, `@shared/*`, `@features/*`, `@env/*`)
- `tailwind.config.js` : préfixe `tw-`, palette CliniTrak, dark mode class
- **Core** :
  - `user.model.ts` : interfaces `CurrentUser`, `AuthTokens`, `AuthState`, enum `SystemRole`
  - `auth.store.ts` : store signals (`authState`, `currentUser`, `isAuthenticated`, `hasRole`, `hasAnyRole`, mutateurs)
  - `AuthService` : login/refresh/logout, restauration depuis localStorage, décodage JWT sans librairie
  - `authInterceptor` : Bearer token + retry refresh sur 401 (fonctionnel)
  - `tenantInterceptor` : X-Tenant-ID depuis env ou sous-domaine (fonctionnel)
  - `authGuard` : redirection /auth/login + returnUrl (fonctionnel)
  - `roleGuard` : vérification data.roles (fonctionnel)
- **Layout** :
  - `MainLayoutComponent` : shell flex (sidebar + (topbar + main))
  - `SidebarComponent` : navigation collapsible, filtrage RBAC via signals, avatar initiales
  - `TopbarComponent` : breadcrumb, notification bell, user menu + logout
- **Features** :
  - `LoginComponent` : PrimeNG 17 (InputText, Password, Button, Message), validation réactive, signals
  - `DashboardComponent` : widgets statistiques skeleton par rôle
  - Routes lazy-loaded pour tous les modules (auth, dashboard, studies, ethics, ctc, pharmacy, billing, documents, admin)
- `styles.css` : imports Tailwind + PrimeNG lara-light-blue + overrides PrimeNG + utilitaires CliniTrak
- `app.config.ts` : `ApplicationConfig` standalone, `provideAppInitializer` pour restauration session
- `proxy.conf.json` : proxy `/api` → `http://localhost:8080`

#### Ajouté — DevOps

- `docker-compose.yml` : postgres 16 + redis 7 + minio + clinitrak-auth + clinitrak-gateway
  - Healthchecks sur tous les services
  - Réseau interne isolé `clinitrak-network`
  - Volumes persistants nommés
- `.env.example` : template complet des variables d'environnement
- `.gitignore` : protections (.env, target/, node_modules/, .idea/)
- `scripts/init-db.sql` : script d'initialisation Docker

#### Ajouté — Gouvernance Claude

- `CLAUDE.md` : guide d'orchestration complet (architecture, agents, conventions, état projet)
- `CHANGELOG.md` : ce fichier
- `.claude/agents/backend.md` : sous-agent Spring Boot / Java
- `.claude/agents/frontend.md` : sous-agent Angular / TypeScript
- `.claude/agents/devops.md` : sous-agent Infrastructure / Docker
- `.claude/settings.json` : configuration Claude Code (permissions, hooks)

---

## Convention d'entrée CHANGELOG

Chaque session doit ajouter une entrée avec :
```markdown
## [version] — YYYY-MM-DD

### Session N — Titre de la session

**Agents impliqués** : backend | frontend | devops (préciser lesquels)

#### Ajouté
- ...

#### Modifié
- ...

#### Corrigé
- ...

#### Supprimé
- ...
```

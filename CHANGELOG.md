# CliniTrak — Changelog

Toutes les modifications notables du projet sont documentées ici.
Format inspiré de [Keep a Changelog](https://keepachangelog.com/fr/1.0.0/).

---

## [Unreleased]

*(Prochaines modifications en cours)*

---

## [0.9.1] — 2026-05-12

### Session 10 — Déploiement production effectif + corrections

**Agents impliqués** : backend, devops

#### Corrigé — Conflits de ports

- study-service : port 8082 → **8092** (8082 occupé par arsbotanica sur vmi2936009)
- ethics-service : port 8083 → **8093** (8083 occupé par CareTrack sur vmi2936009)
- `gateway/src/main/resources/application.yml` : routes study et ethics mises à jour
- `ethics-service/src/main/resources/application.yml` : `SERVER_PORT` et `STUDY_SERVICE_URL` mis à jour
- `ctc-service/src/main/resources/application.yml` : `STUDY_SERVICE_URL` mis à jour (8082 → 8092)
- `pharmacy-service/src/main/resources/application.yml` : `STUDY_SERVICE_URL` mis à jour

#### Corrigé — Mail health indicator (HTTP 503 en production)

- `ethics-service/src/main/resources/application.yml` : `management.health.mail.enabled: false`
- `notification-service/src/main/resources/application.yml` : `management.health.mail.enabled: false`
- Sans SMTP disponible en prod, Spring Boot signalait le service DOWN — corrigé

#### Corrigé — Spring Batch 5.x `@Configuration` obligatoire

- `batch-service/.../WeeklyReportJob.java` : `@Component` → `@Configuration("weeklyReportJobConfiguration")`
- `batch-service/.../MonthlyBillingJob.java` : `@Component` → `@Configuration("monthlyBillingJobConfiguration")`
- `@Component` + `@Bean(name=...)` = bean dupliqué → `BeanDefinitionOverrideException`

#### Ajouté — `fix-deploy.sh` (remplace `deploy.sh` pour le bare-metal)

- Variable `COMMON_DB` centralisée : passe `-DDB_HOST -DDB_PORT=5433 -DDB_USER -DDB_USERNAME -DDB_PASSWORD` à chaque service
- Support `TARGET=<service>` pour déploiement individuel
- Support `SKIP_FRONTEND=1` / `SKIP_BACKEND=1`
- Utilise `pg_isready -h localhost -p 5433` pour vérifier PostgreSQL (robuste en contexte sudo)
- Notification service : variable SMTP (`-DSMTP_HOST`) distincte de `MAIL_HOST`

#### Corrigé — Frontend Angular 20

- `clinitrak-frontend/angular.json` : créé (absent du repo), workspace Angular 20 standard
- `clinitrak-frontend/package.json` : TypeScript `~5.5.0` → `~5.8.0` (requis par Angular 20)
- `clinitrak-frontend/package.json` : ajout `@angular/cdk` et `chart.js` (peer deps PrimeNG 17)
- `clinitrak-frontend/src/styles.css` : déplacement des `@import` avant `@tailwind` (spec CSS)
- Corrections PrimeNG 17 API dans 12+ composants :
  - `primeng/textarea` → `primeng/inputtextarea`
  - `primeng/datepicker` → `primeng/calendar`
  - `severity="warn"` → `severity="warning"`
  - Types `severity` : `string` → union type PrimeNG 17

#### Ajouté — Documentation

- `docs/DEPLOYMENT.md` : réécriture complète avec vrais ports, commandes exactes, pièges connus, procédure accès utilisateurs
- `CLAUDE.md` : correction des ports production (8092/8093)

#### Production — Statut au 2026-05-12

Tous les services déployés et opérationnels sur `clinitrak.gilmotech.be` :

| Service | Port | Statut |
|---------|------|--------|
| gateway | 8080 | ✅ UP |
| auth | 8081 | ✅ UP |
| study | 8092 | ✅ UP |
| ethics | 8093 | ✅ UP |
| ctc | 8084 | ✅ UP |
| pharmacy | 8085 | ✅ UP |
| exchange | 8086 | ✅ UP |
| document | 8088 | ✅ UP |
| batch | 8089 | ✅ UP |
| notification | 8090 | ✅ UP |
| admin | 8091 | ✅ UP |

---

## [0.9.0] — 2026-05-12

### Session 9 — Infrastructure de déploiement bare-metal (production)

**Agents impliqués** : devops

#### Ajouté — Scripts de déploiement

- `scripts/setup-server.sh` : initialisation serveur une seule fois (sudo requis)
  - Installation Redis 7 via apt + activation systemd
  - Téléchargement et installation du binaire MinIO + service systemd
  - Création des 10 bases PostgreSQL (port 5433) avec user `clinitrak`
  - Création des répertoires `/home/claude-worker/clinitrak/jars/` et `/var/log/clinitrak/`
  - Enregistrement des services systemd CliniTrak

- `scripts/deploy.sh` : déploiement principal (source .env.prod && sudo -E)
  - Validation des 6 secrets obligatoires avec longueur minimale
  - Correction des permissions (chown claude-worker) avant chaque build
  - Build Maven par service (`-pl <module> -am -DskipTests`) + copie JAR vers `jars/`
  - Installation des fichiers systemd avec substitution des `PLACEHOLDER_*` via `sed`
  - Redémarrage ordonné des 11 services avec health check 45s
  - Build Angular avec `--legacy-peer-deps` obligatoire
  - Synchronisation automatique de la config Nginx
  - Cibles : `all`, `backend`, `frontend`, ou tout service individuel (`auth`, `gateway`, etc.)

- `scripts/check-health.sh` : monitoring rapide
  - Actuator `/health` pour les 11 services (ports 8080-8091)
  - Statut systemd des 11 services
  - Vérification Redis (`redis-cli ping`), PostgreSQL (`pg_isready -p 5433`), MinIO

#### Ajouté — Fichiers systemd (11 services)

- `scripts/systemd/clinitrak-gateway.service` (port 8080)
- `scripts/systemd/clinitrak-auth.service` (port 8081)
- `scripts/systemd/clinitrak-study.service` (port 8082)
- `scripts/systemd/clinitrak-ethics.service` (port 8084)
- `scripts/systemd/clinitrak-ctc.service` (port 8085)
- `scripts/systemd/clinitrak-pharmacy.service` (port 8086)
- `scripts/systemd/clinitrak-exchange.service` (port 8087)
- `scripts/systemd/clinitrak-document.service` (port 8088)
- `scripts/systemd/clinitrak-batch.service` (port 8089)
- `scripts/systemd/clinitrak-notification.service` (port 8090)
- `scripts/systemd/clinitrak-admin.service` (port 8091)

  Tous : `User=claude-worker`, `MaxRAMPercentage=20.0`, `Restart=always`, `RestartSec=15`,
  logs vers `/var/log/clinitrak/<service>.log`, valeurs sensibles en `PLACEHOLDER_*`.

#### Ajouté — Config Nginx

- `scripts/nginx/clinitrak.gilmotech.be` : vhost complet
  - Redirect HTTP → HTTPS
  - SSL TLS 1.2/1.3 (Let's Encrypt)
  - Proxy `/api/` → gateway port 8080 avec support SSE (buffering off)
  - Proxy `/swagger-ui/` et `/v3/api-docs`
  - Proxy `/minio/` → console MinIO port 9001
  - SPA fallback `try_files $uri $uri/ /index.html`
  - Headers de sécurité (X-Frame-Options, X-Content-Type-Options, etc.)
  - Gzip

#### Ajouté — Template secrets

- `.env.prod` : template de production (hors git — ajouté au `.gitignore`)

#### Modifié

- `.gitignore` : ajout de `.env.prod` et `*.env.prod.local`
- `docs/DEPLOYMENT.md` : réécriture complète en mode bare-metal
  - Tableau de mapping services/ports/bases/fichiers systemd
  - Procédure en 4 étapes (setup, nginx+ssl, premier déploiement, déploiements partiels)
  - Variables obligatoires avec commandes de génération
  - Gestion systemd, ordre de démarrage des dépendances
  - Pièges spécifiques bare-metal (placeholder Spring, permissions root, npm legacy-peer-deps)
  - Section troubleshooting complète

---

## [0.8.1] — 2026-05-12

### Session 7 (suite) — Corrections d'intégration frontend/backend

**Agents impliqués** : orchestrateur (corrections directes)

#### Corrigé — notification-service

- `NotificationController` : ajout endpoints manquants `GET /unread-count` et `PATCH /read-all`
- `NotificationService` : ajout méthodes `countUnread(UUID)` et `markAllAsRead(UUID)`

#### Corrigé — document-service

- `DocumentController` : ajout de l'alias `/api/v1/documents/{id}/download-url` (en plus de `/download`) pour aligner avec le service Angular

#### Corrigé — Frontend Angular

- `notification.service.ts` : connexion SSE via `?userId=UUID` (au lieu de `?token=JWT`) — alignement avec le backend ; import `currentUser` depuis auth.store
- `tenant.interceptor.ts` : priorité 1 sur `localStorage[ct_tenant_override]` pour le TenantSwitcher SUPER_ADMIN du topbar

#### Documentation

- `docs/DEPLOYMENT.md` : section "Serveur cible" ajoutée (vmi2936009, IP, DNS, nginx config, build frontend, pièges CareTrack)

---

## [0.8.0] — 2026-05-11

### Session 8 — document-service, notification-service, batch-service, gateway complets

**Agents impliqués** : backend

#### Ajouté — document-service (port 8088)

- `DocumentServiceApplication.java` : point d'entrée
- `domain/entity/Document.java` : entité JPA avec versioning (parentDocumentId, version counter)
- `domain/enums/DocumentType.java` : PDF, XLSX, DOCX, RTF, IMAGE
- `domain/enums/ModuleSource.java` : STUDY, ETHICS, CTC, PHARMACY, EXCHANGE
- `domain/repository/DocumentRepository.java` : requêtes JPQL multi-critères et historique de versions
- `dto/DocumentUploadRequest.java`, `DocumentResponse.java`, `PdfGenerationRequest.java` : records Java
- `service/DocumentStorageService.java` : upload/download/delete MinIO avec URLs présignées 15min
- `service/DocumentService.java` : upload avec versioning automatique, soft-delete, génération PDF via Thymeleaf + Flying Saucer
- `mapper/DocumentMapper.java` : MapStruct Document → DocumentResponse
- `controller/DocumentController.java` : 6 endpoints (upload multipart, download, list, delete, generate-pdf, versions)
- `config/MinioConfig.java` : initialisation bucket automatique au démarrage
- `config/SecurityConfig.java`, `JpaConfig.java`, `OpenApiConfig.java`
- `exception/GlobalExceptionHandler.java`, `DocumentNotFoundException.java`, `StorageException.java`
- `resources/application.yml` : port 8088, multipart 50MB, MinIO, Thymeleaf
- `resources/db/changelog/V1__init_document.sql` : table documents + 5 index
- `resources/templates/pdf/default.html` : template PDF de base
- `pom.xml` mis à jour : MinIO 8.5.11, Flying Saucer, Thymeleaf, JWT
- Test d'intégration TestContainers : persistance, soft-delete, recherche multi-critères

#### Ajouté — notification-service (port 8090)

- `NotificationServiceApplication.java` : point d'entrée
- `domain/entity/Notification.java` : entité JPA avec read/emailSent tracking
- `domain/enums/NotificationType.java` : 8 types de notifications
- `domain/repository/NotificationRepository.java` : recherche par userId, comptage non-lus
- `dto/SendNotificationRequest.java`, `NotificationResponse.java`, `SseEvent.java` : records Java
- `service/EmailService.java` : Spring Mail + Thymeleaf, `@Retryable` 3 tentatives (2s backoff), `@Recover` fallback
- `service/NotificationService.java` : envoi email async, persistance, push SSE
- `sse/SseEmitterRegistry.java` : registre ConcurrentHashMap par userId, cleanup @Scheduled 5min, heartbeat à la connexion
- `controller/NotificationController.java` : 4 endpoints (send, my, read, stream SSE)
- `config/RetryConfig.java` : `@EnableRetry`, `@EnableAsync`, `@EnableScheduling`
- `resources/application.yml` : port 8090, SMTP MailHog dev
- `resources/db/changelog/V1__init_notification.sql` : table notifications + 5 index
- Templates email Thymeleaf : email-base.html, study-status-change.html, submission-received.html, stock-alert.html, annual-report-due.html
- `pom.xml` mis à jour : Spring Mail, Spring Retry, spring-aspects
- Test d'intégration TestContainers : persistance, recherche par userId, comptage non-lus

#### Ajouté — batch-service (port 8089)

- `BatchServiceApplication.java` : point d'entrée
- `job/NightlyReminderJob.java` : job rappels nocturnes (2h00) via NotificationClient
- `job/WeeklyReportJob.java` : job rapport hebdomadaire (lundi 6h00) via PharmacyClient + NotificationClient
- `job/MonthlyBillingJob.java` : job facturation mensuelle (1er du mois 3h00)
- `scheduler/BatchScheduler.java` : `@Scheduled` pour les 3 jobs, `launchJob()` réutilisable
- `feign/NotificationClient.java` : POST `/api/v1/notifications/send`
- `feign/PharmacyClient.java` : GET `/api/v1/pharmacy/medications/alerts`
- `controller/BatchController.java` : trigger manuel, historique JobExplorer, statut par job
- `config/BatchConfig.java` : `@EnableBatchProcessing`, `@EnableScheduling`
- `config/FeignConfig.java` : `@EnableFeignClients`, logger BASIC, ErrorDecoder
- `resources/application.yml` : port 8089, `spring.batch.jdbc.initialize-schema: always`, job auto-démarrage désactivé
- `pom.xml` mis à jour : Spring Batch, spring-batch-test, OpenFeign
- Test d'intégration TestContainers : vérification configuration des 3 jobs

#### Ajouté — gateway (port 8080)

- `GatewayApplication.java` : point d'entrée
- `filter/GlobalJwtFilter.java` : GlobalFilter ordre -100, paths publics allowlist, validation JWT jjwt 0.12.x, propagation claims (X-User-Email, X-User-Id, X-User-Roles, X-Tenant-ID), 401 RFC 7807
- `filter/GatewayLoggingFilter.java` : GlobalFilter ordre -99, log méthode + path + tenant + user + durée ms
- `config/RateLimiterConfig.java` : KeyResolver par userId (authentifié) ou IP (anonyme)
- `resources/application.yml` : 10 routes complètes, CORS global, Redis rate limiter, httpclient timeouts
- `pom.xml` mis à jour : JWT jjwt 0.12.x
- `Dockerfile` : multi-stage Maven + JRE Alpine
- `docker-compose.yml` : gateway enrichi avec JWT_SECRET + 10 variables *_SERVICE_HOST

---

## [0.7.0] — 2026-05-11

### Session 7 — Infrastructure complète + CI/CD + documentation

**Agents impliqués** : devops

#### Ajouté — Dockerfiles (3 nouveaux services)

- `document-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8088)
- `notification-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8090)
- `batch-service/Dockerfile` : multi-stage Maven + JRE Alpine (port 8089)

#### Ajouté — docker-compose.yml (3 nouveaux services)

- `clinitrak-document` (port 8088) : variables MINIO_ENDPOINT, MINIO_ACCESS_KEY/SECRET_KEY, depends_on postgres + minio
- `clinitrak-notification` (port 8090) : variables MAIL_HOST/PORT, depends_on postgres + mailhog
- `clinitrak-batch` (port 8089) : variables NOTIFICATION_SERVICE_URL, PHARMACY_SERVICE_URL, depends_on postgres + clinitrak-notification

#### Vérifié (aucune modification requise)

- `scripts/init-db.sql` : bases `clinitrak_document`, `clinitrak_notification`, `clinitrak_batch` déjà créées avec GRANT depuis session 2

#### Ajouté — CI/CD GitHub Actions

- `.github/workflows/build-and-test.yml` : build Maven + tests JUnit (ubuntu-latest, Java 21 Temurin) + build Angular prod + tests ChromeHeadless
- `.github/workflows/docker-build.yml` : build et push GHCR pour 11 services (strategy matrix), déclenché sur push main et tags v*
- `.github/workflows/deploy-staging.yml` : pipeline de déploiement staging (commenté, prêt à connecter à l'infrastructure réelle), déclenché sur push develop

#### Ajouté — Documentation

- `docs/DEPLOYMENT.md` : guide production complet (prérequis, variables obligatoires, ordre de démarrage, health checks, migrations Liquibase, troubleshooting)
- `docs/TENANT-SETUP.md` : guide onboarding hôpital (création tenant, modules, invitation admin, SMTP, checklist, résolution problèmes)
- `docs/ADR/ADR-001-multi-tenant.md` : décision RLS applicatif vs schémas séparés vs bases séparées
- `docs/ADR/ADR-002-jwt-exchange.md` : décision JWT distinct pour utilisateurs externes (isolation sécurité)
- `docs/ADR/ADR-003-minio-storage.md` : décision MinIO vs filesystem local vs PostgreSQL bytea

#### Modifié

- `.env.example` : ajout variables DOCUMENT_DB_NAME, NOTIFICATION_DB_NAME, BATCH_DB_NAME, NOTIFICATION_SERVICE_URL, PHARMACY_SERVICE_URL (MINIO_ACCESS/SECRET_KEY déjà présentes)

---

## [0.6.1] — 2026-05-11

### Session 6b — Consolidation DevOps + documentation API complète

**Agents impliqués** : devops

#### Vérifié (aucune modification requise)

- `exchange-service/Dockerfile` : conforme au standard (port 8086, multi-stage, user non-root)
- `admin-service/Dockerfile` : conforme au standard (port 8091, multi-stage, user non-root)
- `docker-compose.yml` : services `clinitrak-exchange` et `clinitrak-admin` déjà présents et corrects
- `scripts/init-db.sql` : bases `clinitrak_exchange` et `clinitrak_admin` déjà créées avec GRANT
- `.env.example` : variables `EXCHANGE_JWT_SECRET` et `ADMIN_DB_NAME` déjà présentes

#### Mis à jour

- `docs/api/exchange-service.md` : réécriture complète au format standard (endpoints détaillés,
  request/response JSON, codes d'erreur, architecture double-JWT, tableau des variables d'env)
- `docs/api/admin-service.md` : réécriture complète au format standard (endpoints détaillés,
  tableau query params audit-logs, export Excel, health check, notes d'architecture)

---

## [0.6.0] — 2026-05-11

### Session 6 — exchange-service + admin-service complets

**Agents impliqués** : backend, frontend, devops (parallèle)

#### Ajouté — exchange-service (backend, port 8086)

- `exchange-service/pom.xml` : Spring Mail, jjwt, MapStruct
- **Enums** (5) : ExternalUserRole, TargetModule, ExchangeRequestType, ExchangeStatus, SenderType
- **Entités JPA** (4) : ExternalUser (BCrypt + email verification), ExchangeRequest, ExchangeDocument, ExchangeMessage
- **Services** (3) : ExternalUserService (register+login+verify), ExchangeRequestService (CRUD+submit+link), ExchangeMessageService
- **JWT externe** : ExchangeJwtService avec clé EXCHANGE_JWT_SECRET distincte + ExchangeJwtFilter
- **Controllers** (2) : ExchangeAuthController (public), ExchangeRequestController (externe+interne)
- **Liquibase** : V1__init_exchange.sql (4 tables, 4 index, 4 triggers)

#### Ajouté — admin-service (backend, port 8091)

- `admin-service/pom.xml` : Apache POI 5.2.5, jjwt, MapStruct
- **Enums** (3) : TenantStatus, SubscriptionType, ModuleType
- **Entités JPA** (2) : AdminTenant (JSONB config, Set<ModuleType>), SystemAuditLog (insert-only)
- **Services** (4) : TenantService, UserInviteService, AuditLogService (export Excel POI), SystemHealthService
- **Controllers** (4) : TenantController, UserAdminController, AuditLogController (export xlsx), SystemController
- **Liquibase** : V1__init_admin.sql (3 tables, 5 index)

#### Ajouté — clinitrak-frontend (Angular)

- `core/models/exchange.model.ts` + `core/models/admin.model.ts`
- `core/services/exchange.service.ts` + `core/services/admin.service.ts`
- Module exchange (6 composants) : LandingPage (layout public), ExternalRegistration, ExchangeLogin, RequestWizard (p-steps 6 étapes), RequestTracking (p-timeline), Messaging
- Module admin (5 composants) : SystemDashboard (p-knob, p-progressBar), TenantList, TenantConfig, UserManagement (p-pickList), AuditLogViewer (export Excel)

#### Ajouté — DevOps

- `exchange-service/Dockerfile` (port 8086), `admin-service/Dockerfile` (port 8091)
- `docker-compose.yml` : services `clinitrak-exchange` et `clinitrak-admin`
- `docs/api/exchange-service.md`, `docs/api/admin-service.md`
- `docs/database/schema.md` : tables exchange + admin
- `.env.example` : EXCHANGE_DB_NAME, EXCHANGE_JWT_SECRET, ADMIN_DB_NAME

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

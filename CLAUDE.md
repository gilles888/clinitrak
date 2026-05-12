# CliniTrak — Guide d'orchestration Claude

## Identité du projet

**CliniTrak** est une plateforme générique de gestion de la recherche clinique hospitalière,
refonte de l'application "Claire" des Cliniques Universitaires Saint-Luc (Bruxelles).

- **Dossier racine** : `/home/claude-worker/clinitrak/`
- **Démarré le** : 2026-05-09
- **Stack** : Spring Boot 3.3.4 / Java 21 / Angular 20 / PostgreSQL 16 / Redis / MinIO

---

## Règle fondamentale — Architecture multi-agents

**Toujours utiliser les 3 sous-agents spécialisés pour tout travail sur ce projet.**
Ne jamais travailler directement sur le code sans passer par le sous-agent approprié.

| Domaine | Sous-agent | Périmètre |
|---------|-----------|-----------|
| Angular, PrimeNG, Tailwind, UX | `frontend` | `clinitrak-frontend/` |
| Spring Boot, Java, SQL, API | `backend` | `auth-service/`, `*-service/`, `gateway/` |
| Docker, CI/CD, infra, sécurité | `devops` | `docker-compose.yml`, `Dockerfile`, scripts |

### Quand lancer les agents en parallèle

Lancer **en parallèle** (même message, plusieurs appels Agent) quand :
- La tâche touche plusieurs domaines simultanément (ex : nouvelle feature end-to-end)
- Les travaux sont indépendants (ex : fix CSS frontend + fix Java backend sans dépendance)
- Un refactoring touche les 3 couches

Lancer **séquentiellement** quand :
- Le backend doit définir l'API avant que le frontend la consomme
- La migration DB doit précéder les changements de code
- Le DevOps dépend de la sortie du build backend

### Workflow standard pour une nouvelle feature

```
1. [PARALLEL] backend-agent  → Crée/modifie l'API (endpoint + DTO + service + Liquibase)
              devops-agent   → Prépare la config Docker/env si nécessaire
2. [SEQUENTIAL] frontend-agent → Implémente le composant Angular qui consomme la nouvelle API
3. [PARALLEL] backend-agent  → Tests JUnit/TestContainers
              frontend-agent → Tests unitaires Angular
4. Mettre à jour CHANGELOG.md
```

---

## Architecture technique complète

### Backend — Maven multi-modules

```
clinitrak/
├── pom.xml                    # Parent : Spring Boot 3.3.4, Java 21
├── gateway/                   # Spring Cloud Gateway (port 8080)
├── auth-service/              # Auth JWT multi-tenant (port 8081) ← COMPLET
├── study-service/             # Gestion des études cliniques (port 8092) ← COMPLET
├── ethics-service/            # Comité d'Éthique (port 8093) ← COMPLET
├── ctc-service/               # Centre de Thérapie Cellulaire (port 8084) ← COMPLET
├── pharmacy-service/          # Pharmacie (port 8085) ← COMPLET
├── exchange-service/          # Portail échanges externes (port 8086) ← COMPLET
├── billing-service/           # Facturation (port 8087) — À développer
├── document-service/          # GED MinIO (port 8088) ← COMPLET
├── batch-service/             # Jobs Spring Batch (port 8089) ← COMPLET
├── notification-service/      # Email/SMS (port 8090) ← COMPLET
└── admin-service/             # Administration (port 8091) ← COMPLET
```

**Package Java** : `be.clinitrak.<module>` (ex: `be.clinitrak.auth`)

**Conventions Java** :
- DTOs = **records Java immuables** avec `@Valid`
- Entités = extends `BaseEntity` (UUID, timestamps, soft-delete, version)
- Mappers = **MapStruct** (`@Mapper(componentModel = "spring")`)
- Exceptions = `GlobalExceptionHandler` avec `ProblemDetail` (RFC 7807)
- JWT = **jjwt 0.12.x** (`Jwts.builder()...signWith(key)...`)
- Multi-tenant = `TenantContext` (ThreadLocal) + header `X-Tenant-ID`
- Tous les endpoints : préfixe `/api/v1/<resource>`
- Liquibase : `V{n}__description.sql` dans `db/changelog/`

### Frontend — Angular 20

```
clinitrak-frontend/src/app/
├── core/
│   ├── interceptors/   # authInterceptor, tenantInterceptor (fonctionnels)
│   ├── guards/         # authGuard, roleGuard (fonctionnels)
│   ├── services/       # AuthService, ...
│   ├── store/          # auth.store.ts (signals Angular)
│   └── models/         # Interfaces TypeScript
├── shared/             # Composants/pipes/directives réutilisables
├── features/           # Modules fonctionnels lazy-loadés
│   ├── auth/           # Login, MFA, reset
│   ├── dashboard/
│   ├── studies/
│   ├── ethics/
│   ├── ctc/
│   ├── pharmacy/
│   ├── billing/
│   ├── documents/
│   └── admin/
└── layout/
    ├── main-layout/    # Shell principal (sidebar + topbar)
    ├── sidebar/        # Navigation par module + filtrage RBAC
    └── topbar/         # User menu + notifications
```

**Conventions Angular** :
- Composants = **standalone** (pas de NgModule)
- État = **signals** Angular (`signal()`, `computed()`)
- Injection = `inject()` (pas de constructeur)
- Guards/intercepteurs = **fonctionnels**
- Templates = nouvelle syntaxe de contrôle (`@if`, `@for`, `@switch`)
- CSS = **Tailwind** (préfixe `tw-`) + **PrimeNG 17**
- Toutes les routes = **lazy-loaded** (`loadComponent` / `loadChildren`)

### Sécurité & Multi-tenant

- **RBAC** : 11 rôles système (`ROLE_SUPER_ADMIN` → `ROLE_EXTERNAL`)
- **JWT** : Access 15min + Refresh 7j (rotation à chaque refresh)
- **Tenant** résolu via header `X-Tenant-ID` (slug) ou sous-domaine
- **BCrypt** force 12 pour les mots de passe
- **AuditAspect** : trace automatiquement tous les appels @RestController (async)
- **Lockout** : 5 tentatives → verrouillage 30min

### Infrastructure (dev — Docker Compose)

```yaml
Services Docker Compose :
  postgres  : PostgreSQL 16 (port 5432)
  redis     : Redis 7 (port 6379)
  minio     : MinIO (port 9000, console 9001)
  mailhog            : Mail dev interceptor (SMTP 1025, UI 8025)
  clinitrak-auth    : Auth Service (port 8081) ✅ Complet
  clinitrak-study   : Study Service (port 8082) ✅ Complet
  clinitrak-ethics   : Ethics Service (port 8083) ✅ Complet
  clinitrak-ctc      : CTC Service (port 8084) ✅ Complet
  clinitrak-pharmacy : Pharmacy Service (port 8085) ✅ Complet
  clinitrak-exchange : Exchange Portal (port 8086) ✅ Complet
  clinitrak-document : Document Service (port 8088) ✅ Complet
  clinitrak-notification : Notification Service (port 8090) ✅ Complet
  clinitrak-batch    : Batch Service (port 8089) ✅ Complet
  clinitrak-admin    : Admin Service (port 8091) ✅ Complet
  clinitrak-gateway : API Gateway (port 8080) ✅ Complet
```

### Infrastructure (production — bare-metal vmi2936009)

Serveur : `45.88.223.242` — Ubuntu, systemd, PostgreSQL 16 port **5433**

```
Ports production :
  8080 → clinitrak-gateway    (systemd) ✅
  8081 → clinitrak-auth       (systemd) ✅
  8092 → clinitrak-study      (systemd) ✅  ← port 8082 occupé par arsbotanica
  8093 → clinitrak-ethics     (systemd) ✅  ← port 8083 occupé par CareTrack
  8084 → clinitrak-ctc        (systemd) ✅
  8085 → clinitrak-pharmacy   (systemd) ✅
  8086 → clinitrak-exchange   (systemd) ✅
  8088 → clinitrak-document   (systemd) ✅
  8089 → clinitrak-batch      (systemd) ✅
  8090 → clinitrak-notification (systemd) ✅
  8091 → clinitrak-admin      (systemd) ✅
  5433 → PostgreSQL 16        (natif, partagé avec CareTrack et arsbotanica)
  6379 → Redis 7              (apt, service systemd)
  9000 → MinIO API            (binaire, service systemd)
  9001 → MinIO Console        (binaire, service systemd)
```

**Déploiement** : `sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && bash /home/claude-worker/clinitrak/fix-deploy.sh'`
**Health check** : `sudo systemctl status 'clinitrak-*' --no-pager | grep Active`
**Logs** : `tail -f /var/log/clinitrak/<service>.log`

---

## État actuel du projet

### Complété (2026-05-09 — Session 1)

- [x] Structure Maven multi-modules (13 modules)
- [x] Parent POM complet (Spring Boot 3.3.4, Java 21, toutes les dépendances)
- [x] **auth-service** complet :
  - Entités JPA : `Tenant`, `User`, `Role`, `Permission`, `RefreshToken`, `AuditLog`
  - Multi-tenant : `TenantContext`, `TenantFilter`, `TenantResolver`
  - JWT : `JwtService` (jjwt 0.12.x), `JwtAuthenticationFilter`
  - Sécurité : `SecurityConfig` (Spring Security 6), `UserDetailsServiceImpl`
  - Cache : `RedisConfig` (4 caches avec TTL)
  - API : `AuthController` (login, refresh, logout, register)
  - AOP : `AuditAspect` (async, tous les controllers)
  - Exceptions : `GlobalExceptionHandler` (RFC 7807)
  - Liquibase : `V1__init.sql` + `V2__seed_data.sql` (11 rôles, 23 permissions)
- [x] **study-service** complet (CRUD, recherche multicritères, 19 endpoints, tests)
- [x] **ethics-service** complet (Feign, séquence CE, templates Thymeleaf, PDF, 16 endpoints)
- [x] **ctc-service** complet (Feign, 6 entités, 17 enums, dashboard, timeline, 15 endpoints)
- [x] **pharmacy-service** complet (AES-256, Apache POI, Flying Saucer, alertes @Scheduled, levée d'aveugle, 12 endpoints)
- [x] **exchange-service** complet (JWT externe distinct, register+verify email, CRUD demandes, messagerie, endpoints internes)
- [x] **admin-service** complet (gestion tenants, invitation utilisateurs, audit logs, export Excel, health check)
- [x] **Angular 20** structure complète :
  - Auth store (signals), guards, intercepteurs
  - Layout (sidebar collapsible + topbar)
  - Page login (PrimeNG 17 + Tailwind)
  - Dashboard skeleton
  - Toutes les routes lazy-loaded
- [x] Docker Compose (postgres, redis, minio, mailhog, auth, study, ethics, ctc, pharmacy, exchange, document, notification, batch, admin, gateway)
- [x] `.env.example`, `.gitignore`, `Dockerfile` multi-stage (un par service — tous les services)
- [x] `scripts/init-db.sql` : toutes les bases créées (auth, study, ethics, ctc, pharmacy, exchange, billing, document, batch, notification, admin)
- [x] `docs/api/` : documentation complète exchange-service et admin-service (format standard)
- [x] **CI/CD GitHub Actions** :
  - `.github/workflows/build-and-test.yml` : build + tests backend et frontend
  - `.github/workflows/docker-build.yml` : build et push GHCR (11 services, matrix strategy)
  - `.github/workflows/deploy-staging.yml` : pipeline staging (prêt à connecter à l'infra)
- [x] **Documentation architecture** :
  - `docs/DEPLOYMENT.md` : guide production complet
  - `docs/TENANT-SETUP.md` : guide onboarding hôpital
  - `docs/ADR/ADR-001-multi-tenant.md` : décision RLS applicatif
  - `docs/ADR/ADR-002-jwt-exchange.md` : décision JWT distinct externe
  - `docs/ADR/ADR-003-minio-storage.md` : décision MinIO vs filesystem
- [x] **Infrastructure bare-metal production** (vmi2936009 — session 9) :
  - `scripts/setup-server.sh` : initialisation Redis, MinIO, PostgreSQL, systemd
  - `scripts/deploy.sh` : déploiement multi-services avec substitution secrets
  - `scripts/check-health.sh` : monitoring des 11 services + infra
  - `scripts/systemd/clinitrak-*.service` : 11 fichiers systemd avec PLACEHOLDER_*
  - `scripts/nginx/clinitrak.gilmotech.be` : vhost Nginx complet (SSL, SPA, SSE, MinIO)
  - `.env.prod` : template secrets production (hors git)

### À développer (par priorité)

1. **Priorité HAUTE** :
   - `billing-service` : implémentation complète
   - Frontend : modules documents, notifications (intégration SSE)

2. **Priorité MOYENNE** :
   - Frontend : module billing
   - Connexion batch-service → study-service (vrais rappels rapport annuel)
   - Tests E2E (Cypress ou Playwright)

3. **Priorité BASSE** :
   - CI/CD : connecter deploy-staging.yml à une vraie infrastructure

---

## Règles de développement permanentes

### Qualité du code
- **JavaDoc** sur toutes les classes et méthodes publiques
- **TSDoc** sur tous les services et composants Angular
- **Zéro magic string** : constantes ou enums
- **Validation @Valid** sur tous les DTOs d'entrée
- **Tests** : toute nouvelle feature doit avoir au moins un test d'intégration (TestContainers)

### Conventions Git
- Branches : `feature/<module>/<description>`, `fix/<module>/<description>`
- Commits : `feat(auth): ...`, `fix(frontend): ...`, `chore(devops): ...`
- **Mettre à jour CHANGELOG.md** à chaque session avant de terminer

### Sécurité
- Ne jamais logger de mots de passe, tokens ou données personnelles
- Ne jamais committer `.env` (seul `.env.example` va en git)
- Toujours vérifier `TenantContext.getTenantId()` dans les services multi-tenant

---

## Commandes utiles

```bash
# Démarrage rapide (dev)
cd /home/claude-worker/clinitrak
docker compose up -d postgres redis minio

# Build et démarrage auth-service
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Frontend
cd clinitrak-frontend
npm install
npm start   # → http://localhost:4200

# Vérification Liquibase seule
mvn liquibase:status -Dspring.profiles.active=dev

# Build complet (tous les modules)
cd /home/claude-worker/clinitrak
mvn clean install -DskipTests

# Tests avec TestContainers
mvn test -pl auth-service
```

---

## Historique des sessions

Voir [CHANGELOG.md](./CHANGELOG.md) pour l'historique détaillé de toutes les modifications.

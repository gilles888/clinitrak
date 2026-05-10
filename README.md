# CliniTrak

Plateforme de gestion de la recherche clinique hospitalière.
Refonte de l'application "Claire" des Cliniques Universitaires Saint-Luc (Bruxelles).

## Démarrage rapide

```bash
# 1. Cloner et configurer l'environnement
cp .env.example .env
# Éditer .env (JWT_SECRET obligatoire en production)

# 2. Démarrer l'infrastructure
docker compose up -d postgres redis minio

# 3. Démarrer le service d'authentification
cd auth-service
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Démarrer le frontend
cd clinitrak-frontend
npm install && npm start
# → http://localhost:4200
```

**Documentation complète** → [`docs/deployment/local.md`](docs/deployment/local.md)

## Architecture

| Couche | Technologie | Version |
|--------|-------------|---------|
| Backend | Spring Boot | 3.3.4 |
| Langage | Java | 21 |
| Frontend | Angular | 20 |
| UI Components | PrimeNG | 17 |
| CSS | Tailwind | 3.4 |
| Base de données | PostgreSQL | 16 |
| Cache | Redis | 7 |
| Stockage objets | MinIO | Latest |
| Auth | JWT (jjwt) | 0.12.5 |
| Migrations DB | Liquibase | via Spring Boot |

## Modules

| Module | Port | Statut |
|--------|------|--------|
| gateway | 8080 | 🔧 En cours |
| auth-service | 8081 | ✅ Complet |
| study-service | 8082 | ⏳ À développer |
| ethics-service | 8083 | ⏳ À développer |
| ctc-service | 8084 | ⏳ À développer |
| pharmacy-service | 8085 | ⏳ À développer |
| exchange-service | 8086 | ⏳ À développer |
| billing-service | 8087 | ⏳ À développer |
| document-service | 8088 | ⏳ À développer |
| batch-service | 8089 | ⏳ À développer |
| notification-service | 8090 | ⏳ À développer |
| admin-service | 8091 | ⏳ À développer |

## Documentation

- [Architecture](docs/architecture/overview.md)
- [API Auth](docs/api/auth-service.md)
- [Schéma base de données](docs/database/schema.md)
- [Guide de déploiement local](docs/deployment/local.md)
- [Onboarding développeur](docs/onboarding.md)
- [Changelog](CHANGELOG.md)

## Pour Claude Code

Ce projet utilise 3 sous-agents spécialisés — voir [CLAUDE.md](CLAUDE.md).

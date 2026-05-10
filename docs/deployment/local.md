# Guide de démarrage local — CliniTrak

*Dernière mise à jour : 2026-05-09*

## Prérequis

| Outil | Version min | Installation |
|-------|------------|--------------|
| Java JDK | 21 | https://adoptium.net |
| Maven | 3.9.x | https://maven.apache.org |
| Node.js | 20.x | https://nodejs.org |
| Docker | 24.x | https://docker.com |
| Docker Compose | 2.x | inclus avec Docker Desktop |

## Étape 1 — Configuration

```bash
cd /home/claude-worker/clinitrak

# Copier le template d'environnement
cp .env.example .env

# Générer un JWT_SECRET sécurisé
openssl rand -base64 64
# Coller la valeur dans .env → JWT_SECRET=...
```

Variables minimales à configurer dans `.env` :
```
DB_PASSWORD=clinitrak_dev          # Peut rester tel quel en dev
JWT_SECRET=<valeur générée>        # OBLIGATOIRE
```

## Étape 2 — Infrastructure Docker

```bash
# Démarrer PostgreSQL, Redis et MinIO
docker compose up -d postgres redis minio

# Vérifier que tous les services sont healthy
docker compose ps

# Logs d'un service
docker compose logs -f postgres
```

**Accès MinIO Console** : http://localhost:9001
- User : `clinitrak_minio` (ou valeur de MINIO_ACCESS_KEY)
- Password : `clinitrak_minio_secret` (ou MINIO_SECRET_KEY)

## Étape 3 — Auth Service

```bash
cd auth-service

# Démarrage avec profil dev (Liquibase s'exécute automatiquement)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Vérification
curl http://localhost:8081/api/v1/auth/health
# → {"status":"UP","service":"auth-service"}

# Swagger UI
# → http://localhost:8081/swagger-ui.html
```

**Liquibase** crée automatiquement toutes les tables et les données de référence au premier démarrage.

## Étape 4 — Frontend Angular

```bash
cd clinitrak-frontend

# Installation des dépendances (première fois uniquement)
npm install

# Démarrage avec proxy vers le gateway (port 8080)
# Note : le gateway n'est pas encore complet, passer directement par auth (8081)
npm start

# → http://localhost:4200
```

**Compte de test** : créer via l'endpoint `/api/v1/auth/register` avec X-Tenant-ID: saintluc

## Commandes utiles

```bash
# Build complet (tous les modules Maven)
mvn clean install -DskipTests

# Tests avec TestContainers (nécessite Docker)
mvn test -pl auth-service

# Vérifier les migrations Liquibase en attente
cd auth-service
mvn liquibase:status

# Arrêter toute l'infrastructure
docker compose down

# Arrêter et supprimer les données (reset complet)
docker compose down -v
```

## Troubleshooting

**PostgreSQL ne démarre pas** :
```bash
docker compose logs postgres
# Si port 5432 déjà utilisé :
lsof -i :5432
```

**Liquibase échoue** :
```bash
# Vérifier la connexion DB
docker compose exec postgres psql -U clinitrak -d clinitrak_auth -c "\dt"
```

**Frontend ne trouve pas l'API** :
- Vérifier que `proxy.conf.json` pointe vers le bon port
- En attendant le gateway, modifier `src/environments/environment.ts` → `apiBaseUrl: 'http://localhost:8081'`

**JWT_SECRET invalide** :
```bash
# Le secret doit être un Base64 d'au moins 32 bytes (256 bits)
openssl rand -base64 64
```

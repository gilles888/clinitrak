# CliniTrak — Guide de déploiement production

## Serveur cible

| Paramètre | Valeur |
|-----------|--------|
| Hôte | `vmi2936009` |
| IP publique IPv4 | `45.88.223.242` |
| IP publique IPv6 | `2a02:c207:2293:6009::1` |
| OS | Ubuntu 22.04 (systemd) |
| Utilisateur applicatif | `claude-worker` |
| Domaine | `clinitrak.gilmotech.be` (à configurer) |

### Outils disponibles sur le serveur

```
/home/claude-worker/tools/
├── jdk-21.0.5+11/          # Java 21 (JAVA_HOME à pointer ici)
├── apache-maven-3.9.6/     # Maven 3.9
└── ...
```

- **Node.js** : v20.20.1 (npm 10.8.2)
- **nginx** + **certbot** : installés système
- **Docker** + **Docker Compose** : à vérifier / installer si absent

### PostgreSQL

CliniTrak utilise PostgreSQL 16 via Docker Compose (conteneur `postgres`). Si PostgreSQL tourne déjà nativement sur le serveur (port 5433 pour CareTrack), le conteneur Docker sera sur le port **5434** pour éviter le conflit :

```yaml
# Dans docker-compose.yml, modifier le mapping de port si nécessaire :
ports:
  - "5434:5432"   # Si le port 5432 est déjà occupé
```

### DNS (à configurer)

```
clinitrak.gilmotech.be    A     45.88.223.242
clinitrak.gilmotech.be    AAAA  2a02:c207:2293:6009::1
```

---

## Déploiement via Docker Compose (recommandé)

### 1. Cloner le dépôt

```bash
cd /home/claude-worker/clinitrak
git pull origin main
```

### 2. Créer le fichier .env.prod

```bash
cp .env.example .env.prod
chmod 600 .env.prod
# Éditer et renseigner toutes les valeurs obligatoires
nano .env.prod
```

### 3. Build et démarrage

```bash
# Build de toutes les images
docker compose --env-file .env.prod build --no-cache

# Démarrage (infrastructure d'abord)
docker compose --env-file .env.prod up -d postgres redis minio mailhog

# Attendre que postgres soit healthy (30s)
docker compose ps postgres

# Démarrer les services backend
docker compose --env-file .env.prod up -d \
  clinitrak-auth clinitrak-study clinitrak-ethics \
  clinitrak-ctc clinitrak-pharmacy clinitrak-exchange \
  clinitrak-notification clinitrak-document \
  clinitrak-batch clinitrak-admin

# Démarrer la gateway
docker compose --env-file .env.prod up -d clinitrak-gateway
```

### 4. Nginx (reverse proxy)

Créer `/etc/nginx/sites-available/clinitrak.gilmotech.be` :

```nginx
server {
    listen 80;
    server_name clinitrak.gilmotech.be;
    return 301 https://$host$request_uri;
}

server {
    listen 443 ssl;
    server_name clinitrak.gilmotech.be;

    ssl_certificate     /etc/letsencrypt/live/clinitrak.gilmotech.be/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/clinitrak.gilmotech.be/privkey.pem;

    root /home/claude-worker/clinitrak/clinitrak-frontend/dist/clinitrak-frontend/browser;
    index index.html;

    # API → Gateway Docker
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        # SSE : désactiver le buffering
        proxy_buffering off;
        proxy_cache off;
        chunked_transfer_encoding on;
    }

    # SPA Angular
    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

```bash
sudo ln -s /etc/nginx/sites-available/clinitrak.gilmotech.be \
           /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 5. SSL Let's Encrypt

```bash
sudo certbot --nginx -d clinitrak.gilmotech.be --email gilmoreau73@gmail.com
```

### 6. Build frontend Angular (production)

```bash
cd /home/claude-worker/clinitrak/clinitrak-frontend
npm install --legacy-peer-deps   # --legacy-peer-deps obligatoire (Angular 20 peer deps)
npm run build -- --configuration production
```

> **Note** : `npm ci` peut échouer sur les peer deps Angular 20. Toujours utiliser `npm install --legacy-peer-deps`.

---

## Pièges connus (leçons du projet CareTrack)

## Prérequis

| Composant | Version minimale | Notes |
|-----------|-----------------|-------|
| Java (JRE) | 21 | Eclipse Temurin recommandé |
| Docker | 24.x | Pour déploiement conteneurisé |
| Docker Compose | 2.x | Plugin Docker (pas standalone) |
| PostgreSQL | 16 | Alpine accepté |
| Redis | 7 | Mode standalone ou Sentinel |
| MinIO | RELEASE.2024+ | Compatible API S3 |

## Variables d'environnement obligatoires en production

Les variables suivantes n'ont **pas** de valeur par défaut acceptable en production. Elles doivent être définies explicitement dans un fichier `.env` ou via les secrets de l'orchestrateur (Kubernetes, Vault, etc.).

```bash
# Sécurité — OBLIGATOIRES
JWT_SECRET=<base64 64 octets — générer avec : openssl rand -base64 64>
EXCHANGE_JWT_SECRET=<base64 64 octets — clé distincte de JWT_SECRET>
PHARMACY_ENCRYPTION_KEY=<32 caractères hex — AES-256>

# Base de données
DB_USER=<utilisateur PostgreSQL non-root>
DB_PASSWORD=<mot de passe fort>

# Redis
REDIS_PASSWORD=<mot de passe Redis>

# MinIO
MINIO_ACCESS_KEY=<access key MinIO>
MINIO_SECRET_KEY=<secret key MinIO>

# Mail (SMTP réel en production)
MAIL_HOST=<serveur SMTP>
MAIL_PORT=587
MAIL_USERNAME=<compte SMTP>
MAIL_PASSWORD=<mot de passe SMTP>

# Domaine
CLINITRAK_ROOT_DOMAIN=clinitrak.be
CORS_ORIGINS=https://app.clinitrak.be
```

## Procédure de démarrage (ordre des services)

Le démarrage doit respecter l'ordre de dépendance suivant. Avec Docker Compose, les `depends_on` avec `condition: service_healthy` gèrent cet ordre automatiquement.

### Ordre recommandé

```
1. postgres          — Base de données principale
2. redis             — Cache et sessions
3. minio             — Stockage objet
4. mailhog           — (dev uniquement, remplacer par SMTP réel en prod)
5. clinitrak-auth    — Authentification (dépend de postgres + redis)
6. clinitrak-study   — Études (dépend de auth)
7. clinitrak-ethics  — CE (dépend de study)
8. clinitrak-ctc     — CTC (dépend de study)
9. clinitrak-pharmacy — Pharmacie (dépend de study)
10. clinitrak-exchange — Portail externe (dépend de postgres)
11. clinitrak-notification — Notifications (dépend de postgres + mailhog/SMTP)
12. clinitrak-document — GED (dépend de postgres + minio)
13. clinitrak-batch   — Jobs batch (dépend de notification + pharmacy)
14. clinitrak-admin   — Administration (dépend de postgres)
15. clinitrak-gateway — Passerelle API (dépend de auth)
```

### Commandes de démarrage

```bash
# Démarrage complet (production)
docker compose -f docker-compose.yml up -d

# Démarrage partiel (infrastructure seule, pour déploiements progressifs)
docker compose up -d postgres redis minio
docker compose up -d clinitrak-auth clinitrak-study
# ... etc.

# Vérification du statut
docker compose ps
```

## Health checks

Chaque service expose un endpoint `/actuator/health` (services Spring Boot) ou `/api/v1/auth/health` (auth-service).

```bash
# Vérifier tous les health checks
for port in 8081 8082 8083 8084 8085 8086 8088 8089 8090 8091; do
  echo -n "Port $port : "
  curl -sf http://localhost:$port/actuator/health | jq -r .status 2>/dev/null || echo "KO"
done

# Gateway
curl -sf http://localhost:8080/actuator/health | jq .
```

## Migrations Liquibase

Les migrations Liquibase s'exécutent **automatiquement au démarrage** de chaque service via Spring Boot. Elles sont idempotentes.

Pour vérifier l'état des migrations sans démarrer le service :

```bash
cd <service>/
mvn liquibase:status -Dspring.profiles.active=prod \
  -Dspring.datasource.url=jdbc:postgresql://<host>:5432/<db> \
  -Dspring.datasource.username=<user> \
  -Dspring.datasource.password=<pass>
```

## Pièges connus (leçons du projet CareTrack)

### Ne pas utiliser `${VAR:-default}` dans les paramètres `-D` systemd/JVM

Spring Boot 3.3+ interprète ce pattern comme un placeholder Spring et génère une référence circulaire. Toujours écrire la valeur résolue.

### Health check trop court

Spring Boot prend ~25-40s à démarrer selon les ressources. Ajuster `start_period` dans les healthchecks Docker si les services sont marqués "unhealthy" trop tôt.

### Permissions root sur target/

Si Maven tourne en `sudo`, les fichiers `target/` sont `root:root`. Corriger avec :
```bash
sudo chown -R claude-worker:claude-worker /home/claude-worker/clinitrak
```

### Générer JWT_SECRET une seule fois

```bash
openssl rand -base64 64
```
Changer ce secret invalide tous les tokens JWT actifs (déconnexion forcée de tous les utilisateurs).

---

## Troubleshooting courant

### Service ne démarre pas (OOMKilled)

Augmenter la RAM allouée au conteneur. Les services Spring Boot nécessitent au minimum 512 Mo. La JVM utilise `MaxRAMPercentage=75.0` — avec 512 Mo conteneur, le heap max sera ~384 Mo.

```yaml
# docker-compose.yml
deploy:
  resources:
    limits:
      memory: 768m
```

### Erreur "Connection refused" sur postgres

Vérifier que le healthcheck postgres est green avant le démarrage des services. Le `pg_isready` peut répondre avant que PostgreSQL soit prêt à accepter des connexions authentifiées.

```bash
docker compose logs postgres | tail -20
docker compose exec postgres pg_isready -U clinitrak
```

### Liquibase : "Table already exists"

Ne jamais modifier les migrations existantes. Si une table doit changer, créer une nouvelle migration `V{n+1}__description.sql`.

### JWT invalide entre services

Tous les services partagent le même `JWT_SECRET`. Vérifier que la variable est identique dans tous les conteneurs.

```bash
docker compose exec clinitrak-auth env | grep JWT_SECRET
docker compose exec clinitrak-study env | grep JWT_SECRET
```

### MinIO inaccessible depuis document-service

Vérifier que `MINIO_ENDPOINT` pointe vers le nom du service Docker (`http://minio:9000`) et non `localhost`.

### MailHog vs SMTP production

En développement, `MAIL_HOST=mailhog` (port 1025) intercepte tous les emails. En production, remplacer par le vrai serveur SMTP et supprimer le service mailhog du compose.

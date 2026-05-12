# CliniTrak — Guide de déploiement production

## Serveur cible

| Paramètre | Valeur |
|-----------|--------|
| Hôte | `vmi2936009` |
| IP publique IPv4 | `45.88.223.242` |
| IP publique IPv6 | `2a02:c207:2293:6009::1` |
| OS | Ubuntu 22.04 (systemd) |
| Utilisateur applicatif | `claude-worker` |
| Domaine | `clinitrak.gilmotech.be` |

### DNS

```
clinitrak.gilmotech.be    A     45.88.223.242
clinitrak.gilmotech.be    AAAA  2a02:c207:2293:6009::1
```

### Outils disponibles sur le serveur

```
/home/claude-worker/tools/
├── jdk-21.0.5+11/          # Java 21 (JAVA_HOME à pointer ici)
└── apache-maven-3.9.6/     # Maven 3.9
```

- **Node.js** : v20.20.1 (npm 10.8.2)
- **nginx** + **certbot** : installés système
- **PostgreSQL 16** : port **5433** (non-standard — partagé avec CareTrack)
- **Redis 7** : installé par `setup-server.sh`
- **MinIO** : binaire téléchargé par `setup-server.sh`

---

## Déploiement bare-metal (production)

CliniTrak est déployé en **bare-metal** sur `vmi2936009` : chaque service Spring Boot tourne comme un service systemd indépendant. Il n'y a pas de Docker en production.

### Mapping des services

| Service | Port | Base de données | Fichier systemd |
|---------|------|----------------|-----------------|
| gateway | 8080 | — | clinitrak-gateway.service |
| auth-service | 8081 | clinitrak_auth | clinitrak-auth.service |
| study-service | 8082 | clinitrak_study | clinitrak-study.service |
| ethics-service | 8084 | clinitrak_ethics | clinitrak-ethics.service |
| ctc-service | 8085 | clinitrak_ctc | clinitrak-ctc.service |
| pharmacy-service | 8086 | clinitrak_pharmacy | clinitrak-pharmacy.service |
| exchange-service | 8087 | clinitrak_exchange | clinitrak-exchange.service |
| document-service | 8088 | clinitrak_document | clinitrak-document.service |
| batch-service | 8089 | clinitrak_batch | clinitrak-batch.service |
| notification-service | 8090 | clinitrak_notification | clinitrak-notification.service |
| admin-service | 8091 | clinitrak_admin | clinitrak-admin.service |

---

### 1. Initialisation serveur (une seule fois)

```bash
cd /home/claude-worker/clinitrak

# Créer et remplir les secrets
cp .env.prod.example .env.prod   # ou copier depuis le gestionnaire de mots de passe
chmod 600 .env.prod
nano .env.prod   # Renseigner TOUTES les valeurs obligatoires

# Lancer le setup (installe Redis, MinIO, crée les bases PostgreSQL)
source .env.prod
sudo -E DB_PASSWORD="$DB_PASSWORD" \
        MINIO_ACCESS_KEY="$MINIO_ACCESS_KEY" \
        MINIO_SECRET_KEY="$MINIO_SECRET_KEY" \
        ./scripts/setup-server.sh
```

### 2. Configuration Nginx + SSL

```bash
# Copier la config nginx
sudo cp scripts/nginx/clinitrak.gilmotech.be /etc/nginx/sites-available/
sudo ln -s /etc/nginx/sites-available/clinitrak.gilmotech.be \
           /etc/nginx/sites-enabled/clinitrak.gilmotech.be

# Vérifier et recharger
sudo nginx -t
sudo systemctl reload nginx

# Obtenir le certificat SSL (DNS doit pointer vers le serveur)
sudo certbot --nginx -d clinitrak.gilmotech.be \
     --email gilmoreau73@gmail.com --agree-tos --non-interactive
```

### 3. Premier déploiement complet

```bash
# Commande de référence (déploiement complet)
source .env.prod && sudo -E ./scripts/deploy.sh all
```

Le script `deploy.sh` effectue dans l'ordre :
1. Valide les variables obligatoires
2. Corrige les permissions (`chown claude-worker`)
3. Build Maven de chaque service (`-DskipTests`)
4. Copie les JARs dans `/home/claude-worker/clinitrak/jars/`
5. Installe les fichiers systemd avec les vraies valeurs (substitution des PLACEHOLDER)
6. Démarre chaque service dans l'ordre des dépendances
7. Build Angular (`npm install --legacy-peer-deps && npm run build --configuration production`)
8. Met à jour la config Nginx si modifiée

### 4. Déploiements partiels

```bash
# Backend uniquement (tous les services Java)
source .env.prod && sudo -E ./scripts/deploy.sh backend

# Frontend uniquement (Angular)
source .env.prod && sudo -E ./scripts/deploy.sh frontend

# Un seul service (ex : auth après un hotfix)
source .env.prod && sudo -E ./scripts/deploy.sh auth
source .env.prod && sudo -E ./scripts/deploy.sh gateway
```

### 5. Vérification de santé

```bash
./scripts/check-health.sh
```

---

## Variables d'environnement obligatoires en production

| Variable | Description | Génération |
|----------|-------------|------------|
| `DB_PASSWORD` | Mot de passe PostgreSQL user `clinitrak` | `openssl rand -base64 24` |
| `JWT_SECRET` | Secret HMAC-SHA256 (min 32 chars), partagé par tous les services | `openssl rand -base64 64` |
| `EXCHANGE_JWT_SECRET` | JWT distinct pour le portail externe | `openssl rand -base64 64` |
| `PHARMACY_ENCRYPTION_KEY` | Clé AES-256 (32 chars hex) pour chiffrement Pharmacie | `openssl rand -hex 16` |
| `MINIO_ACCESS_KEY` | Access key MinIO | ex: `clinitrak_minio` |
| `MINIO_SECRET_KEY` | Secret key MinIO | `openssl rand -base64 24` |

> Conserver `JWT_SECRET` identique entre les déploiements. Le changer invalide tous les tokens actifs.

---

## Gestion des services systemd

```bash
# Statut d'un service
sudo systemctl status clinitrak-auth

# Redémarrage manuel
sudo systemctl restart clinitrak-auth

# Logs en temps réel (systemd journal)
journalctl -u clinitrak-auth -f

# Logs applicatifs (fichier)
tail -f /var/log/clinitrak/auth-service.log
tail -f /var/log/clinitrak/gateway.log

# Après modification manuelle d'un .service dans /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl restart clinitrak-<service>

# Désactiver un service
sudo systemctl disable clinitrak-batch
sudo systemctl stop clinitrak-batch
```

### Ordre de démarrage des dépendances

```
redis.service         (Redis 7)
minio.service         (MinIO)
                ↓
clinitrak-auth        (authentification)
                ↓
clinitrak-study       (études — Feign vers auth)
                ↓
clinitrak-ethics      (Feign vers study)
clinitrak-ctc         (Feign vers study)
clinitrak-pharmacy    (Feign vers study)
                ↓
clinitrak-exchange    (JWT distinct — indépendant)
clinitrak-document    (MinIO — indépendant)
clinitrak-notification (SMTP — indépendant)
clinitrak-admin        (indépendant)
                ↓
clinitrak-batch       (Feign vers notification + pharmacy)
                ↓
clinitrak-gateway     (proxy vers tous les services)
```

---

## Procédure de mise à jour habituelle

```bash
# 1. Mettre à jour le code
cd /home/claude-worker/clinitrak
git pull origin main

# 2. Déployer
source .env.prod && sudo -E ./scripts/deploy.sh all

# 3. Vérifier
./scripts/check-health.sh
```

---

## Infrastructure complémentaire (bare-metal)

### Redis

```bash
# Statut
sudo systemctl status redis-server

# Vérifier connexion
redis-cli ping   # → PONG

# Logs
journalctl -u redis-server -n 50
```

### MinIO

```bash
# Statut
sudo systemctl status minio

# Console web
# http://localhost:9001  (ou https://clinitrak.gilmotech.be/minio/)

# Logs
tail -f /var/log/clinitrak/minio.log
```

### PostgreSQL (port 5433)

```bash
# Connexion directe
psql -h localhost -p 5433 -U clinitrak -d clinitrak_auth

# Via superutilisateur
sudo -u postgres psql -p 5433

# Vérifier les connexions actives
sudo -u postgres psql -p 5433 -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"
```

---

## Migrations Liquibase

Les migrations s'exécutent **automatiquement au démarrage** de chaque service. Elles sont idempotentes.

Pour vérifier l'état sans démarrer le service :

```bash
cd /home/claude-worker/clinitrak/auth-service
export JAVA_HOME=/home/claude-worker/tools/jdk-21.0.5+11
export PATH=$JAVA_HOME/bin:$PATH
/home/claude-worker/tools/apache-maven-3.9.6/bin/mvn liquibase:status \
    -Dspring.profiles.active=prod \
    -DDB_URL=jdbc:postgresql://localhost:5433/clinitrak_auth \
    -DDB_USER=clinitrak \
    -DDB_PASSWORD="$DB_PASSWORD"
```

---

## Pièges connus (leçons CareTrack et CliniTrak)

### Ne pas utiliser `${VAR:-default}` dans les `-D` systemd

Spring Boot 3.3+ interprète ce pattern comme un placeholder Spring et génère une `PlaceholderResolutionException`. Les fichiers `.service` dans `scripts/systemd/` utilisent des valeurs `PLACEHOLDER_*` que `deploy.sh` remplace via `sed`. Ne jamais écrire les vraies valeurs dans ces fichiers versionnés.

### Permissions root sur `target/` et les JARs

Si Maven tourne en `sudo`, les fichiers `target/` appartiennent à `root`. `deploy.sh` corrige automatiquement avec `chown -R claude-worker` avant chaque build.

### Health check trop court

Spring Boot prend ~25-40s à démarrer sur ce serveur. `HEALTH_WAIT=45` dans `deploy.sh`.

### `npm install` : toujours `--legacy-peer-deps`

Angular 20 a des conflits de peer deps. `npm ci` échoue. Toujours utiliser `npm install --legacy-peer-deps`.

### PostgreSQL sur le port 5433

Partagé avec CareTrack. Ne jamais modifier ce port. Le port standard 5432 est réservé ou non utilisé.

### MaxRAMPercentage à 20% par service

11 services Java + Redis + MinIO = charge importante. Avec 20% par service, le heap max de chaque JVM est ~20% de la RAM totale de la machine. Sur un serveur à 4 Go RAM, c'est ~800 Mo par JVM — suffisant pour Spring Boot, à ajuster si OOMKilled.

---

## Troubleshooting

### Service ne démarre pas

```bash
# Vérifier les logs systemd
journalctl -u clinitrak-auth --no-pager -n 50

# Vérifier les logs applicatifs
tail -50 /var/log/clinitrak/auth-service.log

# Vérifier que le JAR est bien là
ls -lh /home/claude-worker/clinitrak/jars/
```

### OOMKilled (manque de mémoire)

Réduire `MaxRAMPercentage` dans le fichier `.service` (ex: 15%) ou désactiver les services non utilisés (`systemctl stop clinitrak-batch`).

### Erreur de connexion PostgreSQL

```bash
# Vérifier que PostgreSQL écoute sur 5433
ss -tlnp | grep 5433

# Tester la connexion
psql -h localhost -p 5433 -U clinitrak -d clinitrak_auth -c "SELECT 1;"
```

### JWT invalide entre services

Vérifier que `JWT_SECRET` est identique dans tous les fichiers `.service` installés :

```bash
grep JWT_SECRET /etc/systemd/system/clinitrak-*.service
```

### MinIO inaccessible depuis document-service

Sur bare-metal, l'endpoint est `http://localhost:9000` (pas `http://minio:9000` comme en Docker).

### Liquibase : "Table already exists"

Ne jamais modifier les migrations existantes. Créer `V{n+1}__description.sql`.

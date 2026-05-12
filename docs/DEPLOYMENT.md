# CliniTrak — Guide de déploiement production

## Serveur cible

| Paramètre | Valeur |
|-----------|--------|
| Hôte | `vmi2936009` |
| IP publique IPv4 | `45.88.223.242` |
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
├── jdk-21.0.5+11/          # Java 21 (JAVA_HOME)
└── apache-maven-3.9.6/     # Maven 3.9
```

- **Node.js** : v20.20.1 (npm 10.8.2)
- **nginx** + **certbot** : installés système
- **PostgreSQL 16** : port **5433** (non-standard — partagé avec CareTrack et arsbotanica)
- **Redis 7** : installé par `setup-server.sh`
- **MinIO** : binaire téléchargé par `setup-server.sh`

---

## Mapping des services (ports réels)

> **Attention** : study et ethics n'utilisent **pas** les ports 8082/8083.
> Ces ports sont occupés par d'autres applications sur le serveur (arsbotanica sur 8082, CareTrack sur 8083).

| Service | Port | Base de données | Statut |
|---------|------|----------------|--------|
| gateway | 8080 | — | ✅ prod |
| auth-service | 8081 | clinitrak_auth | ✅ prod |
| study-service | **8092** | clinitrak_study | ✅ prod |
| ctc-service | 8084 | clinitrak_ctc | ✅ prod |
| pharmacy-service | 8085 | clinitrak_pharmacy | ✅ prod |
| exchange-service | 8086 | clinitrak_exchange | ✅ prod |
| document-service | 8088 | clinitrak_document | ✅ prod |
| batch-service | 8089 | clinitrak_batch | ✅ prod |
| notification-service | 8090 | clinitrak_notification | ✅ prod |
| admin-service | 8091 | clinitrak_admin | ✅ prod |
| ethics-service | **8093** | clinitrak_ethics | ✅ prod |

---

## Déploiement bare-metal

CliniTrak tourne en **bare-metal** sur `vmi2936009` : chaque service Spring Boot est un service systemd indépendant. Pas de Docker en production.

### Script principal : `fix-deploy.sh`

```bash
# Déploiement complet (backend + frontend + nginx)
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && bash /home/claude-worker/clinitrak/fix-deploy.sh'

# Backend uniquement (skip Angular build)
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && SKIP_FRONTEND=1 bash /home/claude-worker/clinitrak/fix-deploy.sh'

# Frontend uniquement
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && TARGET=frontend bash /home/claude-worker/clinitrak/fix-deploy.sh'

# Un seul service (ex : après un hotfix)
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && TARGET=auth bash /home/claude-worker/clinitrak/fix-deploy.sh'
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && TARGET=ethics bash /home/claude-worker/clinitrak/fix-deploy.sh'
```

> **Important** : toujours utiliser `set -a && source .env.prod && set +a` dans le contexte sudo.
> `sudo -E` seul ne propage pas les variables non exportées.

---

### Étape 1 — Initialisation serveur (une seule fois)

```bash
cd /home/claude-worker/clinitrak

# Créer le fichier de secrets
cp .env.example .env.prod
chmod 600 .env.prod
nano .env.prod   # Renseigner TOUTES les valeurs obligatoires

# Setup : installe Redis, MinIO, crée les bases PostgreSQL, installe les systemd
sudo bash -c 'set -a && source .env.prod && set +a && bash scripts/setup-server.sh'
```

### Étape 2 — Configuration Nginx

```bash
# Créer le lien symbolique (HTTP d'abord, en attendant le DNS)
sudo cp scripts/nginx/clinitrak.gilmotech.be /etc/nginx/sites-available/
sudo ln -sf /etc/nginx/sites-available/clinitrak.gilmotech.be \
            /etc/nginx/sites-enabled/clinitrak.gilmotech.be
sudo nginx -t && sudo systemctl reload nginx
```

### Étape 3 — SSL (après propagation DNS)

```bash
sudo certbot --nginx -d clinitrak.gilmotech.be \
     --email gilmoreau73@gmail.com --agree-tos --non-interactive
sudo nginx -t && sudo systemctl reload nginx
```

### Étape 4 — Premier déploiement complet

```bash
# Préparer le swap si le serveur a peu de RAM (Angular build OOM)
sudo fallocate -l 2G /swapfile
sudo chmod 600 /swapfile
sudo mkswap /swapfile
sudo swapon /swapfile

# Déployer
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && bash /home/claude-worker/clinitrak/fix-deploy.sh'
```

### Étape 5 — Health check

```bash
# Status systemd de tous les services
sudo systemctl status 'clinitrak-*' --no-pager | grep -E "(Active|Failed)"

# Vérification HTTP
for port in 8080 8081 8084 8085 8086 8088 8089 8090 8091 8092 8093; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 http://localhost:$port/actuator/health)
  echo "$port: $code"
done
```

---

## Variables d'environnement obligatoires

| Variable | Description | Génération |
|----------|-------------|------------|
| `DB_PASSWORD` | Mot de passe PostgreSQL user `clinitrak` | `openssl rand -base64 24` |
| `JWT_SECRET` | Secret HMAC-SHA256 (min 32 chars) | `openssl rand -base64 64` |
| `EXCHANGE_JWT_SECRET` | JWT distinct portail externe | `openssl rand -base64 64` |
| `PHARMACY_ENCRYPTION_KEY` | Clé AES-256 (32 chars hex) pour pharmacie | `openssl rand -hex 16` |
| `MINIO_ACCESS_KEY` | Access key MinIO | ex: `clinitrak_minio` |
| `MINIO_SECRET_KEY` | Secret key MinIO | `openssl rand -base64 24` |

Variables optionnelles (SMTP) :

| Variable | Défaut | Description |
|----------|--------|-------------|
| `MAIL_HOST` | localhost | Serveur SMTP |
| `MAIL_PORT` | 587 | Port SMTP |
| `MAIL_USERNAME` | — | Login SMTP |
| `MAIL_PASSWORD` | — | Mot de passe SMTP |

> **Conserver `JWT_SECRET` identique** entre les déploiements. Le changer invalide tous les tokens actifs.

---

## Accès utilisateurs

### Premiers accès — Créer le super-admin

La base de données est initialisée **sans utilisateur** (uniquement les rôles et permissions seed).
Le premier compte doit être créé via l'API après le démarrage du service :

```bash
curl -s -X POST https://clinitrak.gilmotech.be/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: saintluc" \
  -d '{
    "email": "admin@clinitrak.be",
    "password": "MotDePasseForte123!",
    "firstName": "Admin",
    "lastName": "CliniTrak"
  }'
```

Le rôle `ROLE_SUPER_ADMIN` doit ensuite être assigné directement en base :

```sql
-- Se connecter à la base auth
psql -h localhost -p 5433 -U clinitrak -d clinitrak_auth

-- Trouver l'ID de l'utilisateur créé
SELECT id, email FROM users WHERE email = 'admin@clinitrak.be';

-- Assigner le rôle SUPER_ADMIN
INSERT INTO user_roles (user_id, role_id)
VALUES (
  '<UUID_USER>',
  '10000000-0000-0000-0000-000000000001'
);
```

### Rôles disponibles

| Rôle | Description | Permissions clés |
|------|-------------|-----------------|
| `ROLE_SUPER_ADMIN` | Admin global plateforme | Toutes les permissions |
| `ROLE_ADMIN_TENANT` | Admin d'une institution | ADMIN:USERS, ADMIN:AUDIT, lectures |
| `ROLE_CE_SECRETARY` | Secrétariat Comité d'Éthique | ETHICS:READ/SUBMIT, STUDY:READ |
| `ROLE_CE_COORDINATOR` | Coordinateur CE | ETHICS:READ/REVIEW/APPROVE |
| `ROLE_CTC_DESK` | Secrétariat CTC | CTC:READ/CREATE, STUDY:READ |
| `ROLE_CTC_CRA` | Clinical Research Associate | CTC + STUDY gestion complète |
| `ROLE_CTC_PM` | Project Manager CTC | Études + CTC + BILLING:READ |
| `ROLE_CTC_COFI` | Coordinateur financier | BILLING complet |
| `ROLE_PHARMACIST` | Pharmacien | PHARMACY complet |
| `ROLE_INVESTIGATOR` | Investigateur | STUDY:READ, ETHICS:SUBMIT |
| `ROLE_EXTERNAL` | Collaborateur externe | STUDY:READ, DOC:READ |

### Tenant de démonstration

| Paramètre | Valeur |
|-----------|--------|
| Slug | `saintluc` |
| Nom | Cliniques Universitaires Saint-Luc |
| Domaine | `saintluc.clinitrak.be` |
| Header API | `X-Tenant-ID: saintluc` |

---

## Gestion des services systemd

```bash
# Statut d'un service
sudo systemctl status clinitrak-auth

# Redémarrage manuel
sudo systemctl restart clinitrak-auth

# Logs en temps réel
journalctl -u clinitrak-auth -f

# Logs fichier
tail -f /var/log/clinitrak/auth.log
tail -f /var/log/clinitrak/gateway.log

# Après modification manuelle d'un .service
sudo systemctl daemon-reload
sudo systemctl restart clinitrak-<service>
```

### Ordre de démarrage (dépendances)

```
PostgreSQL :5433 + Redis :6379 + MinIO :9000
                    ↓
           clinitrak-auth :8081
                    ↓
           clinitrak-study :8092
                    ↓
  clinitrak-ethics :8093   clinitrak-ctc :8084   clinitrak-pharmacy :8085
                    ↓
  clinitrak-exchange :8086  clinitrak-document :8088
  clinitrak-notification :8090  clinitrak-admin :8091
                    ↓
           clinitrak-batch :8089
                    ↓
           clinitrak-gateway :8080
```

---

## Procédure de mise à jour habituelle

```bash
cd /home/claude-worker/clinitrak
git pull origin main

sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && bash /home/claude-worker/clinitrak/fix-deploy.sh'
```

---

## Infrastructure complémentaire

### Redis

```bash
sudo systemctl status redis-server
redis-cli ping   # → PONG
journalctl -u redis-server -n 50
```

### MinIO

```bash
sudo systemctl status minio
# Console web : http://localhost:9001
tail -f /var/log/clinitrak/minio.log
```

### PostgreSQL (port 5433)

```bash
# Connexion directe
psql -h localhost -p 5433 -U clinitrak -d clinitrak_auth

# Via superutilisateur
sudo -u postgres psql -p 5433

# Connexions actives
sudo -u postgres psql -p 5433 -c "SELECT datname, count(*) FROM pg_stat_activity GROUP BY datname;"

# Vérifier que PostgreSQL écoute
pg_isready -h localhost -p 5433
```

---

## Migrations Liquibase

Les migrations s'exécutent automatiquement au démarrage. Pour vérifier manuellement :

```bash
export JAVA_HOME=/home/claude-worker/tools/jdk-21.0.5+11
export PATH=$JAVA_HOME/bin:$PATH
/home/claude-worker/tools/apache-maven-3.9.6/bin/mvn liquibase:status \
    -pl auth-service \
    -Dspring.profiles.active=prod \
    -DDB_HOST=localhost \
    -DDB_PORT=5433 \
    -DDB_NAME=clinitrak_auth \
    -DDB_USER=clinitrak \
    -DDB_PASSWORD="$(grep DB_PASSWORD /home/claude-worker/clinitrak/.env.prod | cut -d= -f2)"
```

---

## Pièges connus

### Ports 8082 et 8083 occupés

`arsbotanica` tourne sur 8082, `CareTrack` sur 8083. CliniTrak utilise donc :
- study-service → **8092**
- ethics-service → **8093**

La gateway et les configurations Feign internes ont été mises à jour en conséquence.

### `set -a` obligatoire avec sudo

```bash
# ❌ Ne propage pas les variables
source .env.prod && sudo -E ./fix-deploy.sh

# ✅ Exporte toutes les variables avant sudo
sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && bash fix-deploy.sh'
```

### Angular build OOM (Out Of Memory)

Avec 11 services Java en mémoire, le build Angular est tué par le kernel. Solution :

```bash
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
```

### `npm install` : toujours `--legacy-peer-deps`

Angular 20 a des conflits de peer deps. `npm ci` échoue. Toujours utiliser `--legacy-peer-deps`.

### Mail health indicator en production

Sans serveur SMTP disponible, Spring Boot signale le service comme DOWN (HTTP 503).
Solution dans `application.yml` de tout service utilisant `spring.mail` :

```yaml
management:
  health:
    mail:
      enabled: false
```

Appliqué à : `ethics-service`, `notification-service`.

### `@Configuration` obligatoire pour les jobs Spring Batch 5.x

En Spring Batch 5.x, une classe contenant `@Bean(name = "monJob")` doit être annotée `@Configuration("nomUnique")` et non `@Component`. Sinon, le bean est enregistré deux fois et Spring lève une `BeanDefinitionOverrideException`.

Appliqué à : `WeeklyReportJob`, `MonthlyBillingJob` dans `batch-service`.

### PostgreSQL sur le port 5433

Partagé avec CareTrack. Les YAMLs ont des défauts sur 5432. Toujours passer `-DDB_PORT=5433` explicitement via `fix-deploy.sh` (variable `COMMON_DB`).

### `angular.json` absent du repo

Le fichier `clinitrak-frontend/angular.json` n'a jamais été committé. Il est présent sur le serveur. Si le repo est cloné sur une nouvelle machine, le recréer avec la configuration Angular 20 (`@angular-devkit/build-angular:application`, output `dist/clinitrak-frontend`).

### TypeScript ≥ 5.8.0 requis

Angular 20 requiert TypeScript ≥ 5.8.0. La version initiale était 5.5.4.

### PrimeNG 17 — API modifiée

| Ancienne importation | Nouvelle importation |
|---------------------|---------------------|
| `primeng/textarea` | `primeng/inputtextarea` |
| `primeng/datepicker` | `primeng/calendar` |
| `severity="warn"` | `severity="warning"` |
| `severity: string` | `severity: 'success' \| 'info' \| 'warning' \| 'danger'` |

---

## Troubleshooting

### Service ne démarre pas

```bash
# Logs systemd
journalctl -u clinitrak-auth --no-pager -n 50

# Logs applicatifs
tail -50 /var/log/clinitrak/auth.log

# JAR présent ?
ls -lh /home/claude-worker/clinitrak/jars/
```

### Service retourne HTTP 503

Vérifier les health indicators. Un indicateur en erreur (mail, Redis, DB) met le service DOWN :

```bash
curl -s http://localhost:8093/actuator/health | python3 -m json.tool
```

### OOMKilled (manque de mémoire)

Réduire `MaxRAMPercentage` dans `fix-deploy.sh` (ligne `write_service`) ou désactiver les services non utilisés :

```bash
sudo systemctl stop clinitrak-batch
```

### JWT invalide entre services

Vérifier que `JWT_SECRET` est identique dans tous les services actifs :

```bash
grep -h JWT_SECRET /etc/systemd/system/clinitrak-*.service | sort -u
```

### MinIO inaccessible

En bare-metal, l'endpoint est `http://localhost:9000` (pas `http://minio:9000` comme en Docker).

### Liquibase : "Table already exists"

Ne jamais modifier les migrations existantes. Créer `V{n+1}__description.sql`.

#!/usr/bin/env bash
# =============================================================================
# setup-server.sh — Initialisation serveur CliniTrak (une seule fois)
#
# Usage :
#   sudo DB_PASSWORD="..." MINIO_ACCESS_KEY="..." MINIO_SECRET_KEY="..." \
#        ./scripts/setup-server.sh
#
# Prérequis :
#   - Ubuntu avec systemd
#   - PostgreSQL 16 déjà actif sur le port 5433 (partagé avec CareTrack)
#   - nginx + certbot installés (apt)
#   - Utilisateur claude-worker existant
# =============================================================================
set -euo pipefail

OWNER=claude-worker
BASE_DIR=/home/claude-worker/clinitrak
LOG_DIR=/var/log/clinitrak
JAVA_HOME=/home/claude-worker/tools/jdk-21.0.5+11

log_info() { echo "[INFO]  $*"; }
log_ok()   { echo "[OK]    $*"; }
log_err()  { echo "[ERREUR] $*" >&2; }

# ── Vérifications préalables ─────────────────────────────────────────────────

if [ "$(id -u)" -ne 0 ]; then
    log_err "Ce script doit être exécuté en tant que root (sudo)."
    exit 1
fi

if [ -z "${DB_PASSWORD:-}" ]; then
    log_err "DB_PASSWORD est obligatoire."
    log_err "Usage : sudo DB_PASSWORD='...' MINIO_ACCESS_KEY='...' MINIO_SECRET_KEY='...' ./scripts/setup-server.sh"
    exit 1
fi

if [ -z "${MINIO_ACCESS_KEY:-}" ] || [ -z "${MINIO_SECRET_KEY:-}" ]; then
    log_err "MINIO_ACCESS_KEY et MINIO_SECRET_KEY sont obligatoires."
    exit 1
fi

echo ""
echo "============================================================"
echo "  CliniTrak — Initialisation serveur"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"
echo ""

# ── 1. Redis ─────────────────────────────────────────────────────────────────
log_info "Installation de Redis 7..."
apt-get update -q
apt-get install -y redis-server
systemctl enable redis-server
systemctl start redis-server
log_ok "Redis installé et démarré."

# ── 2. MinIO ─────────────────────────────────────────────────────────────────
log_info "Installation de MinIO..."
wget -q https://dl.min.io/server/minio/release/linux-amd64/minio -O /usr/local/bin/minio
chmod +x /usr/local/bin/minio

mkdir -p /var/lib/minio
chown ${OWNER}:${OWNER} /var/lib/minio
log_ok "Binaire MinIO installé dans /usr/local/bin/minio"

log_info "Création du service systemd MinIO..."
cat > /etc/systemd/system/minio.service << MINIO_SERVICE
[Unit]
Description=MinIO Object Storage
After=network.target

[Service]
Type=simple
User=${OWNER}
Environment="MINIO_ROOT_USER=${MINIO_ACCESS_KEY}"
Environment="MINIO_ROOT_PASSWORD=${MINIO_SECRET_KEY}"
ExecStart=/usr/local/bin/minio server /var/lib/minio --console-address :9001
Restart=always
RestartSec=10
StandardOutput=append:/var/log/clinitrak/minio.log
StandardError=append:/var/log/clinitrak/minio.log

[Install]
WantedBy=multi-user.target
MINIO_SERVICE

systemctl daemon-reload
systemctl enable minio
log_ok "Service minio créé et activé."

# ── 3. Bases de données PostgreSQL ───────────────────────────────────────────
log_info "Création des bases de données PostgreSQL (port 5433)..."

# Vérifier que PostgreSQL écoute sur 5433
if ! ss -tlnp | grep -q ':5433 '; then
    log_err "PostgreSQL n'écoute pas sur le port 5433. Vérifier que la base est démarrée."
    exit 1
fi

sudo -u postgres psql -p 5433 << SQL
-- Créer l'utilisateur clinitrak s'il n'existe pas
DO \$\$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'clinitrak') THEN
        CREATE USER clinitrak WITH PASSWORD '${DB_PASSWORD}';
    ELSE
        ALTER USER clinitrak WITH PASSWORD '${DB_PASSWORD}';
    END IF;
END
\$\$;

-- Bases de données par service
CREATE DATABASE clinitrak_auth        OWNER clinitrak;
CREATE DATABASE clinitrak_study       OWNER clinitrak;
CREATE DATABASE clinitrak_ethics      OWNER clinitrak;
CREATE DATABASE clinitrak_ctc         OWNER clinitrak;
CREATE DATABASE clinitrak_pharmacy    OWNER clinitrak;
CREATE DATABASE clinitrak_exchange    OWNER clinitrak;
CREATE DATABASE clinitrak_document    OWNER clinitrak;
CREATE DATABASE clinitrak_batch       OWNER clinitrak;
CREATE DATABASE clinitrak_notification OWNER clinitrak;
CREATE DATABASE clinitrak_admin       OWNER clinitrak;

-- Privilèges
GRANT ALL PRIVILEGES ON DATABASE clinitrak_auth         TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_study        TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_ethics       TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_ctc          TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_pharmacy     TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_exchange     TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_document     TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_batch        TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_notification TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_admin        TO clinitrak;
SQL

log_ok "Bases de données créées."

# ── 4. Répertoires ───────────────────────────────────────────────────────────
log_info "Création des répertoires applicatifs..."
mkdir -p "${BASE_DIR}/jars"
mkdir -p "${LOG_DIR}"
chown -R ${OWNER}:${OWNER} "${BASE_DIR}/jars"
chown -R ${OWNER}:${OWNER} "${LOG_DIR}"
log_ok "Répertoires créés : ${BASE_DIR}/jars et ${LOG_DIR}"

# ── 5. Services systemd CliniTrak ─────────────────────────────────────────────
log_info "Installation des services systemd CliniTrak..."
SCRIPT_DIR="${BASE_DIR}/scripts"

for unit in ${SCRIPT_DIR}/systemd/clinitrak-*.service; do
    name=$(basename "$unit")
    cp "$unit" "/etc/systemd/system/${name}"
    chmod 644 "/etc/systemd/system/${name}"
    log_ok "  ${name} installé (sans remplacement de PLACEHOLDER — utiliser deploy.sh)"
done

systemctl daemon-reload
log_ok "Services systemd enregistrés."

# ── 6. Démarrage MinIO ────────────────────────────────────────────────────────
log_info "Démarrage de MinIO..."
# Créer le répertoire de logs avant le démarrage
touch "${LOG_DIR}/minio.log"
chown ${OWNER}:${OWNER} "${LOG_DIR}/minio.log"
systemctl start minio

sleep 5
if systemctl is-active --quiet minio; then
    log_ok "MinIO démarré (API :9000, Console :9001)"
else
    log_err "MinIO n'a pas démarré. Vérifier les logs : journalctl -u minio -n 30"
fi

# ── 7. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
log_ok "Initialisation terminée !"
echo ""
echo "  Prochaine étape : déploiement avec deploy.sh"
echo ""
echo "  source .env.prod && sudo -E ./scripts/deploy.sh all"
echo ""
echo "  Certbot SSL (après DNS configuré) :"
echo "  sudo certbot --nginx -d clinitrak.gilmotech.be \\"
echo "       --email gilmoreau73@gmail.com --agree-tos --non-interactive"
echo ""
echo "  Nginx : copier scripts/nginx/clinitrak.gilmotech.be vers"
echo "          /etc/nginx/sites-available/ et créer le lien symbolique."
echo "============================================================"

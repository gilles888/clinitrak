#!/usr/bin/env bash
# =============================================================================
# deploy.sh — Déploiement bare-metal CliniTrak (multi-services)
#
# Usage :
#   source .env.prod && sudo -E ./scripts/deploy.sh all
#   source .env.prod && sudo -E ./scripts/deploy.sh backend
#   source .env.prod && sudo -E ./scripts/deploy.sh frontend
#   source .env.prod && sudo -E ./scripts/deploy.sh auth
#   source .env.prod && sudo -E ./scripts/deploy.sh gateway
#
# Cibles disponibles :
#   all        → build et déploie backend + frontend
#   backend    → build et déploie tous les services Java
#   frontend   → build Angular et déploie le statique
#   <service>  → build et déploie un service spécifique (ex: auth, study, gateway)
#
# Variables obligatoires (depuis .env.prod) :
#   DB_PASSWORD, JWT_SECRET, EXCHANGE_JWT_SECRET,
#   PHARMACY_ENCRYPTION_KEY, MINIO_ACCESS_KEY, MINIO_SECRET_KEY
# =============================================================================
set -euo pipefail

# ── Configuration ─────────────────────────────────────────────────────────────
BASE_DIR=/home/claude-worker/clinitrak
OWNER=claude-worker
JAVA_HOME=/home/claude-worker/tools/jdk-21.0.5+11
MAVEN=/home/claude-worker/tools/apache-maven-3.9.6/bin/mvn
JARS_DIR="${BASE_DIR}/jars"
LOG_DIR=/var/log/clinitrak
NGINX_CONF_REPO="${BASE_DIR}/scripts/nginx/clinitrak.gilmotech.be"
NGINX_CONF_LIVE=/etc/nginx/sites-available/clinitrak.gilmotech.be
HEALTH_WAIT=45

# Variables optionnelles avec valeurs par défaut (valeurs littérales — pas de ${VAR:-default})
MAIL_HOST="${MAIL_HOST:-localhost}"
MAIL_PORT="${MAIL_PORT:-587}"

# Cible du déploiement (premier argument, défaut : all)
TARGET="${1:-all}"

log_info() { echo "[INFO]  $*"; }
log_ok()   { echo "[OK]    $*"; }
log_warn() { echo "[WARN]  $*"; }
log_err()  { echo "[ERREUR] $*" >&2; }

# ── 0. Vérifications préalables ───────────────────────────────────────────────

if [ "$(id -u)" -ne 0 ]; then
    log_err "Ce script doit être exécuté en tant que root (sudo -E)."
    log_err "Usage : source .env.prod && sudo -E ./scripts/deploy.sh [TARGET]"
    exit 1
fi

# Valider les secrets obligatoires
check_var() {
    local var_name=$1
    local min_len=${2:-1}
    local value="${!var_name:-}"
    if [ -z "$value" ]; then
        log_err "Variable obligatoire manquante : ${var_name}"
        log_err "Définir dans .env.prod et recharger avec : source .env.prod && sudo -E ./scripts/deploy.sh"
        exit 1
    fi
    if [ "${#value}" -lt "$min_len" ]; then
        log_err "${var_name} trop court (${#value} chars). Minimum ${min_len} caractères."
        exit 1
    fi
}

check_var DB_PASSWORD 8
check_var JWT_SECRET 32
check_var EXCHANGE_JWT_SECRET 32
check_var PHARMACY_ENCRYPTION_KEY 32
check_var MINIO_ACCESS_KEY 3
check_var MINIO_SECRET_KEY 8

echo ""
echo "============================================================"
echo "  CliniTrak — Déploiement bare-metal"
echo "  Cible : ${TARGET}"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"
echo ""

# ── 1. Correction des permissions ─────────────────────────────────────────────
log_info "Correction des permissions (chown claude-worker)..."
chown -R ${OWNER}:${OWNER} "${BASE_DIR}" 2>/dev/null || true
log_ok "Permissions corrigées."

# ── 2. Répertoires requis ─────────────────────────────────────────────────────
mkdir -p "${JARS_DIR}"
mkdir -p "${LOG_DIR}"
chown ${OWNER}:${OWNER} "${JARS_DIR}"
chown ${OWNER}:${OWNER} "${LOG_DIR}"

# ── Fonctions de build ────────────────────────────────────────────────────────

# Mappe le nom court → répertoire du module Maven
service_module() {
    case "$1" in
        gateway)      echo "gateway" ;;
        auth)         echo "auth-service" ;;
        study)        echo "study-service" ;;
        ethics)       echo "ethics-service" ;;
        ctc)          echo "ctc-service" ;;
        pharmacy)     echo "pharmacy-service" ;;
        exchange)     echo "exchange-service" ;;
        document)     echo "document-service" ;;
        notification) echo "notification-service" ;;
        batch)        echo "batch-service" ;;
        admin)        echo "admin-service" ;;
        *)            log_err "Service inconnu : $1"; exit 1 ;;
    esac
}

# Build Maven d'un service et copie du JAR
build_service() {
    local name=$1
    local module
    module=$(service_module "$name")

    log_info "Build ${module}..."
    sudo -u ${OWNER} bash -c "
        export JAVA_HOME=${JAVA_HOME}
        export PATH=\$JAVA_HOME/bin:\$PATH
        cd ${BASE_DIR}
        ${MAVEN} clean package -pl ${module} -am -DskipTests -B --no-transfer-progress -q
    "
    log_ok "Build ${module} terminé."

    log_info "Copie du JAR ${module}..."
    # Le JAR peut s'appeler *.jar dans target/ — prendre le premier non-sources non-tests
    local jar_src
    jar_src=$(find "${BASE_DIR}/${module}/target" -maxdepth 1 \
        -name "*.jar" ! -name "*-sources.jar" ! -name "*-tests.jar" \
        | head -1)

    if [ -z "$jar_src" ]; then
        log_err "Aucun JAR trouvé dans ${BASE_DIR}/${module}/target/"
        exit 1
    fi

    cp "$jar_src" "${JARS_DIR}/${module}.jar"
    chown ${OWNER}:${OWNER} "${JARS_DIR}/${module}.jar"
    log_ok "JAR copié → ${JARS_DIR}/${module}.jar"
}

# Build Angular frontend
build_frontend() {
    log_info "Build frontend Angular (production)..."
    sudo -u ${OWNER} bash -c "
        cd ${BASE_DIR}/clinitrak-frontend
        npm install --legacy-peer-deps --prefer-offline 2>/dev/null \
            || npm install --legacy-peer-deps
        npm run build -- --configuration production
    "
    log_ok "Build frontend terminé."

    # Vérifier que index.html est bien là
    local dist="${BASE_DIR}/clinitrak-frontend/dist/clinitrak-frontend/browser"
    if [ ! -f "${dist}/index.html" ]; then
        log_err "index.html absent dans ${dist} — vérifiez le build Angular."
        exit 1
    fi
    log_ok "Frontend déployé dans ${dist}"
}

# ── Fonctions systemd ────────────────────────────────────────────────────────

# Installe le fichier .service en remplaçant les PLACEHOLDER par les vraies valeurs
install_service() {
    local name=$1
    local module
    module=$(service_module "$name")
    local unit_file="${BASE_DIR}/scripts/systemd/clinitrak-${name}.service"
    local tmp="/tmp/clinitrak-${name}.service"

    if [ ! -f "$unit_file" ]; then
        log_err "Fichier systemd introuvable : ${unit_file}"
        exit 1
    fi

    sed \
        -e "s/PLACEHOLDER_DB_PASSWORD/${DB_PASSWORD}/g" \
        -e "s/PLACEHOLDER_JWT_SECRET/${JWT_SECRET}/g" \
        -e "s/PLACEHOLDER_EXCHANGE_JWT_SECRET/${EXCHANGE_JWT_SECRET}/g" \
        -e "s/PLACEHOLDER_PHARMACY_ENCRYPTION_KEY/${PHARMACY_ENCRYPTION_KEY}/g" \
        -e "s/PLACEHOLDER_MINIO_ACCESS_KEY/${MINIO_ACCESS_KEY}/g" \
        -e "s/PLACEHOLDER_MINIO_SECRET_KEY/${MINIO_SECRET_KEY}/g" \
        -e "s/PLACEHOLDER_MAIL_HOST/${MAIL_HOST}/g" \
        -e "s/PLACEHOLDER_MAIL_PORT/${MAIL_PORT}/g" \
        "$unit_file" > "$tmp"

    cp "$tmp" "/etc/systemd/system/clinitrak-${name}.service"
    chmod 640 "/etc/systemd/system/clinitrak-${name}.service"
    rm "$tmp"
    log_ok "Service clinitrak-${name} installé dans /etc/systemd/system/"
}

# Redémarre un service et vérifie son état
restart_service() {
    local name=$1
    systemctl daemon-reload
    systemctl enable "clinitrak-${name}" 2>/dev/null || true
    systemctl restart "clinitrak-${name}"
    log_info "Attente du démarrage de clinitrak-${name} (${HEALTH_WAIT}s max)..."
    sleep "${HEALTH_WAIT}"
    if systemctl is-active --quiet "clinitrak-${name}"; then
        log_ok "clinitrak-${name} actif."
    else
        log_err "clinitrak-${name} n'a pas démarré correctement."
        log_err "Dernières lignes :"
        journalctl -u "clinitrak-${name}" --no-pager -n 30 2>/dev/null || true
        tail -30 "${LOG_DIR}/${name}-service.log" 2>/dev/null || true
        # Ne pas sortir en erreur fatale — continuer le déploiement des autres services
    fi
}

# Health check via /actuator/health
health_check() {
    local port=$1
    local name=$2
    local max_wait=${3:-${HEALTH_WAIT}}
    local waited=0

    printf "Health check %-20s (port %s)... " "$name" "$port"
    while [ $waited -lt $max_wait ]; do
        if curl -sf "http://localhost:${port}/actuator/health" > /dev/null 2>&1; then
            echo "UP"
            return 0
        fi
        sleep 2
        waited=$((waited + 2))
    done
    echo "TIMEOUT (${max_wait}s)"
    return 1
}

# ── Déploiement d'un service complet (build + install + restart) ──────────────
deploy_service() {
    local name=$1
    log_info "=== Déploiement de ${name} ==="
    build_service "$name"
    install_service "$name"
    restart_service "$name"
}

# Ordre de démarrage des services backend (dépendances respectées)
BACKEND_SERVICES=(
    auth
    study
    ethics
    ctc
    pharmacy
    exchange
    document
    notification
    batch
    admin
    gateway
)

# ── 3. Dispatch selon la cible ────────────────────────────────────────────────

case "$TARGET" in

    all)
        log_info "=== Déploiement complet (backend + frontend) ==="
        for svc in "${BACKEND_SERVICES[@]}"; do
            deploy_service "$svc"
        done
        build_frontend
        ;;

    backend)
        log_info "=== Déploiement de tous les services backend ==="
        for svc in "${BACKEND_SERVICES[@]}"; do
            deploy_service "$svc"
        done
        ;;

    frontend)
        log_info "=== Déploiement du frontend uniquement ==="
        build_frontend
        ;;

    # Service individuel
    auth|study|ethics|ctc|pharmacy|exchange|document|notification|batch|admin|gateway)
        deploy_service "$TARGET"
        ;;

    *)
        log_err "Cible inconnue : ${TARGET}"
        log_err "Cibles valides : all, backend, frontend, auth, study, ethics, ctc,"
        log_err "                 pharmacy, exchange, document, notification, batch, admin, gateway"
        exit 1
        ;;
esac

# ── 4. Config Nginx ───────────────────────────────────────────────────────────
if [ -f "${NGINX_CONF_REPO}" ]; then
    if ! diff -q "${NGINX_CONF_REPO}" "${NGINX_CONF_LIVE}" > /dev/null 2>&1; then
        log_info "Config Nginx modifiée — mise à jour..."
        cp "${NGINX_CONF_REPO}" "${NGINX_CONF_LIVE}"
        if [ ! -L "/etc/nginx/sites-enabled/clinitrak.gilmotech.be" ]; then
            ln -s "${NGINX_CONF_LIVE}" "/etc/nginx/sites-enabled/clinitrak.gilmotech.be"
        fi
        log_ok "Config Nginx copiée."
    else
        log_info "Config Nginx inchangée."
    fi
else
    log_warn "Fichier ${NGINX_CONF_REPO} absent — config Nginx non modifiée."
fi

if nginx -t > /dev/null 2>&1; then
    systemctl reload nginx
    log_ok "Nginx rechargé."
else
    log_err "Configuration Nginx invalide — rechargement annulé."
    nginx -t
fi

# ── 5. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
log_ok "Déploiement terminé !"
echo ""
echo "  Health checks :"
echo "    ./scripts/check-health.sh"
echo ""
echo "  Logs :"
echo "    tail -f /var/log/clinitrak/auth-service.log"
echo "    tail -f /var/log/clinitrak/gateway.log"
echo ""
echo "  URLs :"
echo "    API Gateway  : https://clinitrak.gilmotech.be/api/"
echo "    Frontend     : https://clinitrak.gilmotech.be"
echo "    Swagger      : https://clinitrak.gilmotech.be/swagger-ui/"
echo "============================================================"

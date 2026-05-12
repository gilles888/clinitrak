#!/usr/bin/env bash
# =============================================================================
# fix-deploy.sh — Déploiement complet CliniTrak (backend + frontend + Nginx)
#
# Usage :
#   source .env.prod && sudo -E ./fix-deploy.sh
#   source .env.prod && sudo -E SKIP_FRONTEND=1 ./fix-deploy.sh
#   source .env.prod && sudo -E SKIP_BACKEND=1 ./fix-deploy.sh
#   source .env.prod && sudo -E TARGET=auth ./fix-deploy.sh
#
# Variables obligatoires (depuis .env.prod) :
#   DB_PASSWORD              — mot de passe PostgreSQL user clinitrak
#   JWT_SECRET               — secret HMAC-SHA256 partagé (≥32 chars)
#   EXCHANGE_JWT_SECRET      — JWT distinct portail externe (≥32 chars)
#   PHARMACY_ENCRYPTION_KEY  — clé AES-256 (32 chars hex)
#   MINIO_ACCESS_KEY         — access key MinIO
#   MINIO_SECRET_KEY         — secret key MinIO
#
# Variables optionnelles :
#   MAIL_HOST        — serveur SMTP (défaut : localhost)
#   MAIL_PORT        — port SMTP (défaut : 587)
#   MAIL_USERNAME    — login SMTP
#   MAIL_PASSWORD    — mot de passe SMTP
#   SKIP_FRONTEND    — mettre à 1 pour sauter le build Angular
#   SKIP_BACKEND     — mettre à 1 pour sauter tous les builds Java
#   TARGET           — service individuel (auth|study|ethics|ctc|pharmacy|
#                      exchange|document|notification|batch|admin|gateway)
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
FRONTEND_DIST="${BASE_DIR}/clinitrak-frontend/dist/clinitrak-frontend/browser"
WEB_DIR="${BASE_DIR}/web"
HEALTH_WAIT=45

# Variables optionnelles (valeurs littérales — pas de ${VAR:-default} dans les -D Spring)
MAIL_HOST="${MAIL_HOST:-localhost}"
MAIL_PORT="${MAIL_PORT:-587}"
MAIL_USERNAME="${MAIL_USERNAME:-}"
MAIL_PASSWORD="${MAIL_PASSWORD:-}"
SKIP_FRONTEND="${SKIP_FRONTEND:-0}"
SKIP_BACKEND="${SKIP_BACKEND:-0}"
TARGET="${TARGET:-all}"

log_info() { echo "[INFO]  $*"; }
log_ok()   { echo "[OK]    ✓ $*"; }
log_warn() { echo "[WARN]  $*"; }
log_err()  { echo "[ERREUR] $*" >&2; }
log_step() { echo ""; echo "── $* ──────────────────────────────────────────"; }

# ── 0. Vérifications préalables ───────────────────────────────────────────────

if [ "$(id -u)" -ne 0 ]; then
    log_err "Ce script doit être exécuté en tant que root."
    log_err "Usage : source .env.prod && sudo -E ./fix-deploy.sh"
    exit 1
fi

check_var() {
    local name=$1 min=${2:-16}
    local val="${!name:-}"
    if [ -z "$val" ]; then
        log_err "Variable obligatoire manquante : $name"
        log_err "Renseigner dans .env.prod puis relancer : source .env.prod && sudo -E ./fix-deploy.sh"
        exit 1
    fi
    if [ "${#val}" -lt "$min" ]; then
        log_err "$name trop court (${#val} chars, minimum $min)."
        exit 1
    fi
}

check_var DB_PASSWORD 8
check_var JWT_SECRET 32
check_var EXCHANGE_JWT_SECRET 32
check_var PHARMACY_ENCRYPTION_KEY 32
check_var MINIO_ACCESS_KEY 4
check_var MINIO_SECRET_KEY 8

echo ""
echo "============================================================"
echo "  CliniTrak — Déploiement complet"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "  Target : ${TARGET}"
echo "============================================================"
echo ""

# ── 1. Répertoires et permissions ────────────────────────────────────────────
log_step "Préparation"

mkdir -p "${JARS_DIR}" "${LOG_DIR}" "${WEB_DIR}"
# Corriger les permissions si build précédent en root (piège Maven+sudo)
chown -R ${OWNER}:${OWNER} "${BASE_DIR}" 2>/dev/null || true
log_ok "Répertoires et permissions OK"

# ── 2. Vérification infrastructure ───────────────────────────────────────────
log_step "Infrastructure"

if ! ss -tlnp | grep -q ':5433 '; then
    log_err "PostgreSQL n'écoute pas sur le port 5433."
    log_err "Lancer d'abord : ./scripts/setup-server.sh"
    exit 1
fi
log_ok "PostgreSQL :5433"

if ! redis-cli ping > /dev/null 2>&1; then
    log_warn "Redis ne répond pas — tentative de démarrage..."
    systemctl start redis-server || true
    sleep 3
fi
redis-cli ping > /dev/null 2>&1 && log_ok "Redis :6379" || { log_err "Redis KO"; exit 1; }

if ! systemctl is-active --quiet minio 2>/dev/null; then
    log_warn "MinIO non actif — tentative de démarrage..."
    systemctl start minio 2>/dev/null || log_warn "MinIO absent (installer via setup-server.sh)"
else
    log_ok "MinIO :9000"
fi

# ── 3. Fichiers systemd (écriture avec les vrais secrets) ────────────────────
log_step "Services systemd"

write_service() {
    local name=$1 port=$2 db=$3 extra_opts=$4
    cat > "/etc/systemd/system/clinitrak-${name}.service" << UNIT
[Unit]
Description=CliniTrak ${name} (Spring Boot / Java 21)
After=network.target postgresql.service redis.service minio.service

[Service]
User=${OWNER}
Group=${OWNER}
WorkingDirectory=${BASE_DIR}
Environment="JAVA_HOME=${JAVA_HOME}"
Environment="PATH=${JAVA_HOME}/bin:/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"
ExecStart=${JAVA_HOME}/bin/java \
  -XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=20.0 \
  -Djava.security.egd=file:/dev/./urandom \
  -Dspring.profiles.active=prod \
  -DSERVER_PORT=${port} \
  -DJWT_SECRET=${JWT_SECRET} \
  -DCORS_ORIGINS=https://clinitrak.gilmotech.be \
  ${extra_opts} \
  -jar ${JARS_DIR}/${name}.jar \
  >> ${LOG_DIR}/${name}.log 2>&1
Restart=always
RestartSec=15
StandardOutput=append:${LOG_DIR}/${name}.log
StandardError=append:${LOG_DIR}/${name}.log

[Install]
WantedBy=multi-user.target
UNIT
    chmod 640 "/etc/systemd/system/clinitrak-${name}.service"
}

# gateway — pas de DB, rate limiting Redis
write_service "gateway" "8080" "" \
  "-DREDIS_HOST=localhost \
  -DREDIS_PORT=6379"

# auth-service
write_service "auth" "8081" "clinitrak_auth" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_auth \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DSPRING_REDIS_HOST=localhost \
  -DSPRING_REDIS_PORT=6379"

# study-service
write_service "study" "8082" "clinitrak_study" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_study \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}"

# ethics-service
write_service "ethics" "8083" "clinitrak_ethics" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_ethics \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DMAIL_HOST=${MAIL_HOST} \
  -DMAIL_PORT=${MAIL_PORT} \
  -DMAIL_USERNAME=${MAIL_USERNAME} \
  -DMAIL_PASSWORD=${MAIL_PASSWORD}"

# ctc-service
write_service "ctc" "8084" "clinitrak_ctc" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_ctc \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}"

# pharmacy-service
write_service "pharmacy" "8085" "clinitrak_pharmacy" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_pharmacy \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DPHARMACY_ENCRYPTION_KEY=${PHARMACY_ENCRYPTION_KEY} \
  -DMAIL_HOST=${MAIL_HOST} \
  -DMAIL_PORT=${MAIL_PORT}"

# exchange-service
write_service "exchange" "8086" "clinitrak_exchange" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_exchange \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DEXCHANGE_JWT_SECRET=${EXCHANGE_JWT_SECRET} \
  -DMAIL_HOST=${MAIL_HOST} \
  -DMAIL_PORT=${MAIL_PORT}"

# document-service
write_service "document" "8088" "clinitrak_document" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_document \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DMINIO_ENDPOINT=http://localhost:9000 \
  -DMINIO_ACCESS_KEY=${MINIO_ACCESS_KEY} \
  -DMINIO_SECRET_KEY=${MINIO_SECRET_KEY}"

# notification-service
write_service "notification" "8090" "clinitrak_notification" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_notification \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DMAIL_HOST=${MAIL_HOST} \
  -DMAIL_PORT=${MAIL_PORT} \
  -DMAIL_USERNAME=${MAIL_USERNAME} \
  -DMAIL_PASSWORD=${MAIL_PASSWORD}"

# batch-service
write_service "batch" "8089" "clinitrak_batch" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_batch \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD} \
  -DNOTIFICATION_SERVICE_URL=http://localhost:8090 \
  -DPHARMACY_SERVICE_URL=http://localhost:8085"

# admin-service
write_service "admin" "8091" "clinitrak_admin" \
  "-DSPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5433/clinitrak_admin \
  -DSPRING_DATASOURCE_USERNAME=clinitrak \
  -DSPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}"

systemctl daemon-reload
log_ok "11 fichiers systemd écrits et rechargés"

# ── 4. Build backend ──────────────────────────────────────────────────────────
log_step "Build backend"

# Services à construire selon TARGET
if [ "${TARGET}" = "all" ] || [ "${TARGET}" = "backend" ]; then
    ALL_SERVICES="gateway auth-service study-service ethics-service ctc-service pharmacy-service exchange-service document-service notification-service batch-service admin-service"
elif [ "${TARGET}" = "frontend" ]; then
    ALL_SERVICES=""
else
    ALL_SERVICES="${TARGET}-service"
    # gateway n'a pas le suffixe -service
    [ "${TARGET}" = "gateway" ] && ALL_SERVICES="gateway"
fi

if [ "${SKIP_BACKEND}" = "1" ]; then
    log_warn "SKIP_BACKEND=1 — build Java ignoré."
    ALL_SERVICES=""
fi

build_service() {
    local module=$1
    local jar_name="${module}"
    # Normaliser : auth-service → auth (pour le nom du JAR)
    local svc_name="${module%-service}"
    [ "${module}" = "gateway" ] && svc_name="gateway"

    log_info "Build ${module}..."
    # Exécuté en claude-worker pour éviter root:root sur target/ (piège CareTrack)
    sudo -u ${OWNER} bash -c "
        export JAVA_HOME=${JAVA_HOME}
        export PATH=\${JAVA_HOME}/bin:/home/claude-worker/tools/apache-maven-3.9.6/bin:\$PATH
        cd ${BASE_DIR}
        ${MAVEN} clean package -pl ${module} -am -DskipTests -B --no-transfer-progress -q
    "
    # Copier le JAR
    local jar_path
    jar_path=$(find "${BASE_DIR}/${module}/target" -name "*.jar" ! -name "*sources*" ! -name "*javadoc*" | head -1)
    if [ -z "${jar_path}" ]; then
        log_err "JAR introuvable pour ${module}"
        exit 1
    fi
    cp "${jar_path}" "${JARS_DIR}/${svc_name}.jar"
    chown ${OWNER}:${OWNER} "${JARS_DIR}/${svc_name}.jar"
    log_ok "${module} → ${JARS_DIR}/${svc_name}.jar"
}

for svc in ${ALL_SERVICES}; do
    build_service "${svc}"
done

# ── 5. Démarrage des services (ordre des dépendances) ─────────────────────────
log_step "Démarrage des services"

restart_svc() {
    local svc=$1
    local jar="${JARS_DIR}/${svc}.jar"
    if [ ! -f "${jar}" ]; then
        log_warn "${svc}.jar absent — service non démarré."
        return
    fi
    touch "${LOG_DIR}/${svc}.log" 2>/dev/null || true
    chown ${OWNER}:${OWNER} "${LOG_DIR}/${svc}.log" 2>/dev/null || true
    systemctl enable "clinitrak-${svc}" > /dev/null 2>&1
    systemctl restart "clinitrak-${svc}"
    log_info "clinitrak-${svc} redémarré — attente ${HEALTH_WAIT}s..."
}

health_check() {
    local svc=$1 port=$2
    local waited=0
    local url="http://localhost:${port}/actuator/health"
    while [ ${waited} -lt ${HEALTH_WAIT} ]; do
        local code
        code=$(curl -sf -o /dev/null -w "%{http_code}" "${url}" 2>/dev/null || echo "000")
        if [ "${code}" = "200" ]; then
            log_ok "clinitrak-${svc} UP (HTTP 200)"
            return 0
        fi
        sleep 3
        waited=$((waited + 3))
    done
    log_warn "clinitrak-${svc} — pas de réponse après ${HEALTH_WAIT}s (peut encore démarrer)"
    return 0
}

deploy_svc() {
    local svc=$1 port=$2
    restart_svc "${svc}"
    health_check "${svc}" "${port}"
}

if [ "${TARGET}" = "all" ] || [ "${TARGET}" = "backend" ]; then
    # Ordre des dépendances
    deploy_svc "auth"         8081
    deploy_svc "study"        8082
    deploy_svc "ethics"       8083
    deploy_svc "ctc"          8084
    deploy_svc "pharmacy"     8085
    deploy_svc "exchange"     8086
    deploy_svc "document"     8088
    deploy_svc "notification" 8090
    deploy_svc "batch"        8089
    deploy_svc "admin"        8091
    deploy_svc "gateway"      8080
elif [ "${TARGET}" != "frontend" ]; then
    # Service individuel
    declare -A SVC_PORTS=(
        [auth]=8081 [study]=8082 [ethics]=8083 [ctc]=8084
        [pharmacy]=8085 [exchange]=8086 [document]=8088
        [notification]=8090 [batch]=8089 [admin]=8091 [gateway]=8080
    )
    port="${SVC_PORTS[${TARGET}]:-0}"
    deploy_svc "${TARGET}" "${port}"
fi

# ── 6. Build frontend ─────────────────────────────────────────────────────────
log_step "Build frontend Angular"

if [ "${SKIP_FRONTEND}" = "1" ] || [ "${TARGET}" = "backend" ]; then
    log_warn "SKIP_FRONTEND=1 ou TARGET=backend — build Angular ignoré."
elif [ "${TARGET}" = "all" ] || [ "${TARGET}" = "frontend" ]; then
    log_info "npm install + build production..."
    sudo -u ${OWNER} bash -c "
        cd ${BASE_DIR}/clinitrak-frontend
        npm install --legacy-peer-deps --prefer-offline 2>/dev/null \
          || npm install --legacy-peer-deps
        npm run build -- --configuration production
    "

    # Déployer les fichiers statiques
    mkdir -p "${WEB_DIR}"
    rm -rf "${WEB_DIR:?}"/*
    if [ -d "${FRONTEND_DIST}" ]; then
        cp -r "${FRONTEND_DIST}/." "${WEB_DIR}/"
        chown -R ${OWNER}:${OWNER} "${WEB_DIR}/"
        [ -f "${WEB_DIR}/index.html" ] && log_ok "Frontend déployé → ${WEB_DIR}/" \
            || { log_err "index.html absent — build Angular échoué ?"; exit 1; }
    else
        log_err "Répertoire dist introuvable : ${FRONTEND_DIST}"
        exit 1
    fi
fi

# ── 7. Nginx ──────────────────────────────────────────────────────────────────
log_step "Nginx"

if [ -f "${NGINX_CONF_REPO}" ]; then
    if ! diff -q "${NGINX_CONF_REPO}" "${NGINX_CONF_LIVE}" > /dev/null 2>&1; then
        log_info "Config Nginx modifiée — mise à jour..."
        cp "${NGINX_CONF_REPO}" "${NGINX_CONF_LIVE}"
        # Créer le lien symbolique s'il n'existe pas
        if [ ! -L /etc/nginx/sites-enabled/clinitrak.gilmotech.be ]; then
            ln -sf "${NGINX_CONF_LIVE}" /etc/nginx/sites-enabled/clinitrak.gilmotech.be
        fi
        log_ok "Config Nginx copiée."
    else
        log_info "Config Nginx inchangée."
    fi
else
    log_warn "${NGINX_CONF_REPO} absent — nginx non modifié."
fi

if nginx -t > /dev/null 2>&1; then
    systemctl reload nginx
    log_ok "Nginx rechargé."
else
    log_err "Config Nginx invalide — rechargement annulé."
    nginx -t
fi

# ── 8. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
log_ok "Déploiement terminé !"
echo ""
echo "  Frontend  : https://clinitrak.gilmotech.be"
echo "  API       : https://clinitrak.gilmotech.be/api/v1/auth/login"
echo "  Swagger   : https://clinitrak.gilmotech.be/swagger-ui/"
echo ""
echo "  Logs :"
echo "    tail -f /var/log/clinitrak/auth.log"
echo "    tail -f /var/log/clinitrak/gateway.log"
echo ""
echo "  Health check global :"
echo "    ./scripts/check-health.sh"
echo ""
echo "  Statut systemd :"
echo "    systemctl status 'clinitrak-*'"
echo "============================================================"

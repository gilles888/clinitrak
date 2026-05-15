#!/usr/bin/env bash
# =============================================================================
# redeploy-tenant-fix.sh — Redéploiement des 5 services corrigés (bug TenantFilter)
#                          + création des données de démo ONCO-2026-01
#
# Usage (sur le serveur) :
#   sudo -u claude-worker git -C /home/claude-worker/clinitrak pull origin main && \
#   sudo bash -c 'set -a && source /home/claude-worker/clinitrak/.env.prod && set +a && \
#                 bash /home/claude-worker/clinitrak/scripts/redeploy-tenant-fix.sh'
# =============================================================================
set -euo pipefail

BASE_DIR=/home/claude-worker/clinitrak
DEPLOY_SCRIPT="${BASE_DIR}/fix-deploy.sh"

log_step() { echo ""; echo "── $* ──────────────────────────────────────────"; }
log_ok()   { echo "[OK]    ✓ $*"; }
log_info() { echo "[INFO]  $*"; }

if [ "$(id -u)" -ne 0 ]; then
    echo "[ERREUR] Ce script doit être lancé en root (via sudo)."
    echo "  Usage : sudo bash -c 'set -a && source ${BASE_DIR}/.env.prod && set +a && bash ${BASE_DIR}/scripts/redeploy-tenant-fix.sh'"
    exit 1
fi

# ── 1. Git pull ───────────────────────────────────────────────────────────────
log_step "Git pull"

cd "${BASE_DIR}"
# git doit s'exécuter en claude-worker (root est refusé sur un dossier tiers)
sudo -u claude-worker git pull origin main
log_ok "Code à jour"

# ── 2. Redéploiement des 5 services corrigés ──────────────────────────────────
log_step "Redéploiement study-service"
TARGET=study bash "${DEPLOY_SCRIPT}"

log_step "Redéploiement ethics-service"
TARGET=ethics bash "${DEPLOY_SCRIPT}"

log_step "Redéploiement ctc-service"
TARGET=ctc bash "${DEPLOY_SCRIPT}"

log_step "Redéploiement pharmacy-service"
TARGET=pharmacy bash "${DEPLOY_SCRIPT}"

log_step "Redéploiement exchange-service"
TARGET=exchange bash "${DEPLOY_SCRIPT}"

# ── 3. Vérification rapide des services ──────────────────────────────────────
log_step "Health check"

check_health() {
    local svc=$1 port=$2
    local code
    code=$(curl -sf -o /dev/null -w "%{http_code}" "http://localhost:${port}/actuator/health" 2>/dev/null || echo "000")
    if [ "${code}" = "200" ]; then
        log_ok "clinitrak-${svc} UP"
    else
        echo "[WARN]  clinitrak-${svc} répond ${code} — vérifier : tail -20 /var/log/clinitrak/${svc}.log"
    fi
}

check_health study    8092
check_health ethics   8093
check_health ctc      8084
check_health pharmacy 8085
check_health exchange 8086

# ── 4. Données de démo ────────────────────────────────────────────────────────
log_step "Création des données de démo (ONCO-2026-01)"

chmod +x "${BASE_DIR}/scripts/demo-data.sh"
# Exécuté en claude-worker (accès curl localhost)
sudo -u claude-worker bash "${BASE_DIR}/scripts/demo-data.sh"

# ── 5. Résumé ─────────────────────────────────────────────────────────────────
echo ""
echo "============================================================"
echo "  Déploiement terminé"
echo "  L'étude ONCO-2026-01 est maintenant visible pour"
echo "  pm@saintluc.be dans CliniTrak"
echo "============================================================"

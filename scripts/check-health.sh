#!/usr/bin/env bash
# =============================================================================
# check-health.sh — Monitoring rapide CliniTrak
#
# Usage :
#   ./scripts/check-health.sh
# =============================================================================
set -euo pipefail

# ── Services à vérifier ────────────────────────────────────────────────────────
declare -A SERVICES=(
    [8080]="gateway"
    [8081]="auth"
    [8082]="study"
    [8084]="ethics"
    [8085]="ctc"
    [8086]="pharmacy"
    [8087]="exchange"
    [8088]="document"
    [8089]="batch"
    [8090]="notification"
    [8091]="admin"
)

# Ordre d'affichage (ports triés)
PORTS_ORDER=(8080 8081 8082 8084 8085 8086 8087 8088 8089 8090 8091)

# ── Systemd services ───────────────────────────────────────────────────────────
SYSTEMD_SERVICES=(
    clinitrak-gateway
    clinitrak-auth
    clinitrak-study
    clinitrak-ethics
    clinitrak-ctc
    clinitrak-pharmacy
    clinitrak-exchange
    clinitrak-document
    clinitrak-batch
    clinitrak-notification
    clinitrak-admin
)

echo ""
echo "============================================================"
echo "  CliniTrak — Health Check"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "============================================================"
echo ""

# ── 1. Actuator health checks ─────────────────────────────────────────────────
echo "--- Actuator /health ---"
all_ok=true

for port in "${PORTS_ORDER[@]}"; do
    name="${SERVICES[$port]}"
    response=$(curl -sf --max-time 5 \
        "http://localhost:${port}/actuator/health" 2>/dev/null || echo "")

    if [ -n "$response" ]; then
        status=$(echo "$response" | python3 -c \
            "import sys,json; d=json.load(sys.stdin); print(d.get('status','?'))" \
            2>/dev/null || echo "PARSE_ERR")
    else
        status="DOWN"
        all_ok=false
    fi

    if [ "$status" = "UP" ]; then
        icon="OK"
    else
        icon="KO"
        all_ok=false
    fi

    printf "  [%s]  %-22s  port %-5s  %s\n" "$icon" "$name" "$port" "$status"
done

echo ""

# ── 2. Systemd status ─────────────────────────────────────────────────────────
echo "--- Systemd ---"
for svc in "${SYSTEMD_SERVICES[@]}"; do
    state=$(systemctl is-active "$svc" 2>/dev/null || echo "unknown")
    if [ "$state" = "active" ]; then
        icon="OK"
    else
        icon="KO"
    fi
    printf "  [%s]  %-35s  %s\n" "$icon" "$svc" "$state"
done

echo ""

# ── 3. Infrastructure ─────────────────────────────────────────────────────────
echo "--- Infrastructure ---"

# Redis
redis_ok=$(redis-cli ping 2>/dev/null || echo "FAIL")
if [ "$redis_ok" = "PONG" ]; then
    printf "  [OK]  %-35s  PONG\n" "redis"
else
    printf "  [KO]  %-35s  DOWN\n" "redis"
    all_ok=false
fi

# PostgreSQL (port 5433)
pg_ok=$(pg_isready -h localhost -p 5433 -U clinitrak 2>/dev/null | grep -c "accepting" || echo "0")
if [ "$pg_ok" -gt 0 ]; then
    printf "  [OK]  %-35s  accepting connections\n" "postgres (5433)"
else
    printf "  [KO]  %-35s  DOWN\n" "postgres (5433)"
    all_ok=false
fi

# MinIO
minio_ok=$(curl -sf --max-time 5 "http://localhost:9000/minio/health/live" 2>/dev/null && echo "UP" || echo "DOWN")
if [ "$minio_ok" = "UP" ]; then
    printf "  [OK]  %-35s  UP\n" "minio (9000)"
else
    printf "  [KO]  %-35s  DOWN\n" "minio (9000)"
    all_ok=false
fi

echo ""

# ── 4. Résumé ──────────────────────────────────────────────────────────────────
if [ "$all_ok" = "true" ]; then
    echo "  Tous les services sont opérationnels."
else
    echo "  Certains services sont en erreur — vérifier les logs :"
    echo "    tail -f /var/log/clinitrak/<service>.log"
    echo "    journalctl -u clinitrak-<service> -n 50 --no-pager"
fi

echo ""
echo "  URLs :"
echo "    Frontend : https://clinitrak.gilmotech.be"
echo "    API      : https://clinitrak.gilmotech.be/api/v1/"
echo "    Swagger  : https://clinitrak.gilmotech.be/swagger-ui/"
echo "============================================================"

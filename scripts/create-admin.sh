#!/usr/bin/env bash
# =============================================================================
# create-admin.sh — Crée le premier compte SUPER_ADMIN CliniTrak
#
# Usage :
#   bash /home/claude-worker/clinitrak/scripts/create-admin.sh
#
# Le script crée l'admin sur http://localhost:8081 (auth-service local).
# L'auth-service doit être démarré avant de lancer ce script.
# =============================================================================

AUTH_URL="http://localhost:8081/api/v1/auth"
TENANT="saintluc"

# ── Paramètres par défaut (modifiables) ───────────────────────────────────────
DEFAULT_EMAIL="admin@clinitrak.be"
DEFAULT_FIRST="Admin"
DEFAULT_LAST="CliniTrak"

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║     CliniTrak — Création du compte Super Admin       ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""

# ── Vérifier que l'auth-service répond ───────────────────────────────────────
if ! curl -sf http://localhost:8081/actuator/health > /dev/null 2>&1; then
    echo "[ERREUR] auth-service non disponible sur localhost:8081"
    echo "         Vérifier : sudo systemctl status clinitrak-auth"
    exit 1
fi
echo "[OK]  auth-service disponible"
echo ""

# ── Saisie des informations ───────────────────────────────────────────────────
read -rp "Email          [${DEFAULT_EMAIL}] : " EMAIL
EMAIL="${EMAIL:-$DEFAULT_EMAIL}"

read -rp "Prénom         [${DEFAULT_FIRST}] : " FIRST
FIRST="${FIRST:-$DEFAULT_FIRST}"

read -rp "Nom            [${DEFAULT_LAST}]  : " LAST
LAST="${LAST:-$DEFAULT_LAST}"

while true; do
    read -rsp "Mot de passe   (min 8 caractères) : " PASSWORD
    echo ""
    if [ ${#PASSWORD} -ge 8 ]; then
        break
    fi
    echo "[ERREUR] Le mot de passe doit contenir au moins 8 caractères."
done

read -rsp "Confirmer      : " PASSWORD2
echo ""
if [ "$PASSWORD" != "$PASSWORD2" ]; then
    echo "[ERREUR] Les mots de passe ne correspondent pas."
    exit 1
fi

echo ""
echo "──────────────────────────────────────────────────────"
echo "  Email   : $EMAIL"
echo "  Prénom  : $FIRST"
echo "  Nom     : $LAST"
echo "  Tenant  : $TENANT"
echo "  Rôle    : ROLE_SUPER_ADMIN"
echo "──────────────────────────────────────────────────────"
echo ""

read -rp "Confirmer la création ? (oui/non) : " CONFIRM
if [ "$CONFIRM" != "oui" ]; then
    echo "Annulé."
    exit 0
fi

echo ""
echo "[INFO] Création du compte..."

# ── Appel API register ────────────────────────────────────────────────────────
RESPONSE=$(curl -s -w "\n__HTTP_STATUS__%{http_code}" \
    -X POST "${AUTH_URL}/register" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: ${TENANT}" \
    -d "{
      \"email\": \"${EMAIL}\",
      \"password\": \"${PASSWORD}\",
      \"firstName\": \"${FIRST}\",
      \"lastName\": \"${LAST}\",
      \"roleName\": \"ROLE_SUPER_ADMIN\"
    }")

HTTP_STATUS=$(echo "$RESPONSE" | grep "__HTTP_STATUS__" | sed 's/.*__HTTP_STATUS__//')
BODY=$(echo "$RESPONSE" | grep -v "__HTTP_STATUS__")

if [ "$HTTP_STATUS" != "200" ] && [ "$HTTP_STATUS" != "201" ]; then
    echo "[ERREUR] Enregistrement échoué (HTTP $HTTP_STATUS)"
    echo "         Réponse : $BODY"
    exit 1
fi
echo "[OK]  Compte créé (HTTP $HTTP_STATUS)"

# ── Test de connexion ─────────────────────────────────────────────────────────
echo "[INFO] Test de connexion..."

LOGIN_RESPONSE=$(curl -s -w "\n__HTTP_STATUS__%{http_code}" \
    -X POST "${AUTH_URL}/login" \
    -H "Content-Type: application/json" \
    -H "X-Tenant-ID: ${TENANT}" \
    -d "{
      \"email\": \"${EMAIL}\",
      \"password\": \"${PASSWORD}\"
    }")

LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | grep "__HTTP_STATUS__" | sed 's/.*__HTTP_STATUS__//')
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | grep -v "__HTTP_STATUS__")

if [ "$LOGIN_STATUS" != "200" ]; then
    echo "[ERREUR] Connexion échouée (HTTP $LOGIN_STATUS)"
    echo "         Réponse : $LOGIN_BODY"
    exit 1
fi

ACCESS_TOKEN=$(echo "$LOGIN_BODY" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))" 2>/dev/null)
TOKEN_PREVIEW="${ACCESS_TOKEN:0:40}..."

echo ""
echo "╔══════════════════════════════════════════════════════╗"
echo "║        Compte Super Admin créé avec succès !         ║"
echo "╚══════════════════════════════════════════════════════╝"
echo ""
echo "  URL         : https://clinitrak.gilmotech.be"
echo "  Email       : ${EMAIL}"
echo "  Mot de passe: (celui que vous venez de saisir)"
echo "  Tenant      : ${TENANT}  (header X-Tenant-ID)"
echo "  Rôle        : ROLE_SUPER_ADMIN (toutes permissions)"
echo ""
echo "  Token JWT (valide 15 min) :"
echo "  ${TOKEN_PREVIEW}"
echo ""
echo "  Tester l'API :"
echo "  curl -H 'Authorization: Bearer \$TOKEN' \\"
echo "       -H 'X-Tenant-ID: ${TENANT}' \\"
echo "       https://clinitrak.gilmotech.be/api/v1/admin/users"
echo ""
echo "  Swagger UI : https://clinitrak.gilmotech.be/swagger-ui/"
echo ""

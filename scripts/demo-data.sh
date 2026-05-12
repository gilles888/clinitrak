#!/usr/bin/env bash
# =============================================================================
# demo-data.sh — Initialisation des données de démonstration CliniTrak
#
# Crée 10 comptes utilisateurs (un par rôle) et un jeu de données cohérent
# simulant le cycle de vie complet d'une étude clinique.
#
# Étude fictive : ONCO-2026-01 "Évaluation de l'immunothérapie NK-Cell"
# Tenant        : saintluc (Cliniques Universitaires Saint-Luc)
#
# Usage :
#   bash /home/claude-worker/clinitrak/scripts/demo-data.sh
#
# Prérequis :
#   - auth-service   UP sur localhost:8081
#   - study-service  UP sur localhost:8092
#   - ethics-service UP sur localhost:8093
#   - ctc-service    UP sur localhost:8084
#   - pharmacy-service UP sur localhost:8085
# =============================================================================

set -euo pipefail

# ── Config ────────────────────────────────────────────────────────────────────
TENANT="saintluc"
DEMO_PASSWORD="Demo@Saintluc2026!"
TODAY=$(date '+%Y-%m-%d')
NEXT_MONTH=$(date -d '+1 month' '+%Y-%m-%d' 2>/dev/null || date -v+1m '+%Y-%m-%d')
NEXT_YEAR=$(date -d '+12 months' '+%Y-%m-%d' 2>/dev/null || date -v+1y '+%Y-%m-%d')
TWO_YEARS=$(date -d '+24 months' '+%Y-%m-%d' 2>/dev/null || date -v+2y '+%Y-%m-%d')
LAST_MONTH=$(date -d '-1 month' '+%Y-%m-%d' 2>/dev/null || date -v-1m '+%Y-%m-%d')

BASE_AUTH="http://localhost:8081/api/v1/auth"
BASE_STUDY="http://localhost:8092/api/v1/studies"
BASE_ETHICS="http://localhost:8093/api/v1/ethics"
BASE_CTC="http://localhost:8084/api/v1/ctc"
BASE_PHARMACY="http://localhost:8085/api/v1/pharmacy"

# Couleurs
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'; BOLD='\033[1m'

log_ok()   { echo -e "${GREEN}[OK]${NC}    $*"; }
log_info() { echo -e "        $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_err()  { echo -e "${RED}[ERREUR]${NC} $*"; }
log_step() { echo -e "\n${BOLD}── $* ──────────────────────────────────────────${NC}"; }

# ── Vérifications préalables ──────────────────────────────────────────────────
echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║      CliniTrak — Initialisation données de démonstration   ║${NC}"
echo -e "${BOLD}║      Étude ONCO-2026-01 — Cliniques Saint-Luc              ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════╝${NC}"

log_step "Vérification des services"

check_service() {
    local name=$1 url=$2
    if curl -sf "${url}/actuator/health" -o /dev/null 2>/dev/null; then
        log_ok "${name}"
    else
        log_warn "${name} non disponible — les étapes concernées seront ignorées"
        return 1
    fi
    return 0
}

AUTH_UP=false;     check_service "auth-service   :8081" "http://localhost:8081" && AUTH_UP=true
STUDY_UP=false;    check_service "study-service  :8092" "http://localhost:8092" && STUDY_UP=true
ETHICS_UP=false;   check_service "ethics-service :8093" "http://localhost:8093" && ETHICS_UP=true
CTC_UP=false;      check_service "ctc-service    :8084" "http://localhost:8084" && CTC_UP=true
PHARMACY_UP=false; check_service "pharmacy-service :8085" "http://localhost:8085" && PHARMACY_UP=true

if [ "$AUTH_UP" = false ]; then
    log_err "auth-service indispensable. Lancer : sudo systemctl start clinitrak-auth"
    exit 1
fi

echo ""
echo -e "  Tenant        : ${BOLD}$TENANT${NC}"
echo -e "  Mot de passe  : ${BOLD}$DEMO_PASSWORD${NC} (identique pour tous les comptes)"
echo ""
read -rp "Continuer ? (oui/non) : " CONFIRM
[ "$CONFIRM" != "oui" ] && { echo "Annulé."; exit 0; }

# ── Helpers ───────────────────────────────────────────────────────────────────

# Crée un utilisateur et renvoie son email (ou warning si déjà existant)
create_user() {
    local email=$1 first=$2 last=$3 role=$4

    local resp
    resp=$(curl -s -w "\n__STATUS__%{http_code}" \
        -X POST "${BASE_AUTH}/register" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: ${TENANT}" \
        -d "{\"email\":\"${email}\",\"password\":\"${DEMO_PASSWORD}\",\"firstName\":\"${first}\",\"lastName\":\"${last}\",\"roleName\":\"${role}\"}")

    local status body
    status=$(echo "$resp" | grep "__STATUS__" | sed 's/.*__STATUS__//')
    body=$(echo "$resp" | grep -v "__STATUS__")

    if [ "$status" = "200" ] || [ "$status" = "201" ]; then
        log_ok "${role} → ${email}"
    else
        local msg
        msg=$(echo "$body" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('detail', d.get('message','?')))" 2>/dev/null || echo "$body")
        log_warn "${role} → ${email} (HTTP ${status}: ${msg})"
    fi
}

# Authentifie et retourne le token JWT
login() {
    local email=$1
    local resp
    resp=$(curl -s -X POST "${BASE_AUTH}/login" \
        -H "Content-Type: application/json" \
        -H "X-Tenant-ID: ${TENANT}" \
        -d "{\"email\":\"${email}\",\"password\":\"${DEMO_PASSWORD}\"}")
    echo "$resp" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('accessToken',''))" 2>/dev/null
}

# POST JSON, retourne le body
api_post() {
    local url=$1 token=$2 data=$3
    curl -s -X POST "${url}" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-ID: ${TENANT}" \
        -d "${data}"
}

# PATCH JSON, retourne le body
api_patch() {
    local url=$1 token=$2 data=$3
    curl -s -X PATCH "${url}" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${token}" \
        -H "X-Tenant-ID: ${TENANT}" \
        -d "${data}"
}

# Extrait un champ JSON
json_get() {
    local json=$1 field=$2
    echo "$json" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('${field}',''))" 2>/dev/null
}

# ════════════════════════════════════════════════════════════════════════════
# PHASE 1 — Création des comptes utilisateurs
# ════════════════════════════════════════════════════════════════════════════
log_step "Phase 1 — Création des 10 comptes utilisateurs"

create_user "admin@saintluc.be"           "Gilles"    "Gilmoreau"   "ROLE_SUPER_ADMIN"
create_user "coordination@saintluc.be"    "Marie"     "Dupont"      "ROLE_ADMIN_TENANT"
create_user "secretariat.ce@saintluc.be"  "Sophie"    "Martin"      "ROLE_CE_SECRETARY"
create_user "coordinateur.ce@saintluc.be" "Dr. Pierre" "Leclerc"   "ROLE_CE_COORDINATOR"
create_user "cra@saintluc.be"             "Anna"      "Schmidt"     "ROLE_CTC_CRA"
create_user "pm@saintluc.be"              "Thomas"    "Bernard"     "ROLE_CTC_PM"
create_user "cofi@saintluc.be"            "Julie"     "Fontaine"    "ROLE_CTC_COFI"
create_user "pharmacien@saintluc.be"      "Dr. Marc"  "Lefèvre"     "ROLE_PHARMACIST"
create_user "investigateur@saintluc.be"   "Pr. François" "Renard"   "ROLE_INVESTIGATOR"
create_user "externe@pharmalab.com"       "John"      "Smith"       "ROLE_EXTERNAL"

# Connexion admin pour toutes les étapes suivantes
log_info "Connexion admin..."
ADMIN_TOKEN=$(login "admin@saintluc.be")
if [ -z "$ADMIN_TOKEN" ]; then
    log_err "Impossible de s'authentifier en tant qu'admin."
    exit 1
fi
log_ok "Token admin obtenu"

# ════════════════════════════════════════════════════════════════════════════
# PHASE 2 — Création de l'étude clinique
# ════════════════════════════════════════════════════════════════════════════
STUDY_ID=""

if [ "$STUDY_UP" = true ]; then
    log_step "Phase 2 — Création de l'étude ONCO-2026-01"

    STUDY_RESP=$(api_post "$BASE_STUDY" "$ADMIN_TOKEN" "{
      \"title\": \"Évaluation de l'efficacité et de la sécurité de l'immunothérapie NK-Cell en monothérapie chez des patients atteints de DLBCL réfractaire\",
      \"acronym\": \"ONCO-2026-01\",
      \"studyType\": \"INTERVENTIONAL\",
      \"sponsorType\": \"ACADEMIC\",
      \"sponsor\": \"Cliniques Universitaires Saint-Luc\",
      \"principalInvestigator\": \"Pr. François Renard\",
      \"therapeuticArea\": \"Oncologie hématologique\",
      \"phase\": \"PHASE_2\",
      \"startDate\": \"${TODAY}\",
      \"endDate\": \"${TWO_YEARS}\",
      \"targetEnrollment\": 45,
      \"isSponsorCusl\": true,
      \"description\": \"Étude monocentrique de phase II évaluant l'efficacité de cellules Natural Killer allogéniques (NK-Cell) chez des patients adultes atteints de lymphome diffus à grandes cellules B (DLBCL) en échec de seconde ligne.\",
      \"eudractNumber\": \"2026-001234-12\",
      \"ctisNumber\": \"2026-500123-41-00\"
    }")

    STUDY_ID=$(json_get "$STUDY_RESP" "id")
    if [ -n "$STUDY_ID" ] && [ "$STUDY_ID" != "None" ]; then
        log_ok "Étude créée — ID: $STUDY_ID"
        log_info "Acronyme : ONCO-2026-01 | Phase II | 45 patients | Oncologie"

        # Passer l'étude en SUBMITTED
        api_patch "${BASE_STUDY}/${STUDY_ID}/status" "$ADMIN_TOKEN" \
          "{\"status\":\"SUBMITTED\",\"statusDate\":\"${TODAY}\",\"comment\":\"Soumission initiale au Comité d'Éthique\"}" > /dev/null
        log_ok "Statut → SUBMITTED (soumise au CE)"
    else
        log_warn "Étude non créée — $(json_get "$STUDY_RESP" "detail")"
        STUDY_ID=""
    fi
else
    log_warn "study-service absent — Phase 2 ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 3 — Soumission au Comité d'Éthique
# ════════════════════════════════════════════════════════════════════════════
ETHICS_ID=""

if [ "$ETHICS_UP" = true ] && [ -n "$STUDY_ID" ]; then
    log_step "Phase 3 — Dossier Comité d'Éthique"

    ETHICS_RESP=$(api_post "${BASE_ETHICS}/reviews" "$ADMIN_TOKEN" "{
      \"studyId\": \"${STUDY_ID}\",
      \"reviewType\": \"INITIAL\",
      \"submissionDate\": \"${TODAY}\",
      \"rapporteurName\": \"Dr. Pierre Leclerc\",
      \"comments\": \"Dossier de soumission initiale — Protocole v1.0 du ${TODAY}. Documents joints : protocole, brochure investigateur, formulaires de consentement, assurance.\"
    }")

    ETHICS_ID=$(json_get "$ETHICS_RESP" "id")
    if [ -n "$ETHICS_ID" ] && [ "$ETHICS_ID" != "None" ]; then
        log_ok "Dossier CE créé — ID: $ETHICS_ID"
        log_info "Type : INITIAL | Rapporteur : Dr. Pierre Leclerc"

        # Enregistrer la décision favorable du CE
        api_patch "${BASE_ETHICS}/reviews/${ETHICS_ID}/decision" "$ADMIN_TOKEN" \
          "{\"decision\":\"APPROVED\",\"decisionDate\":\"${NEXT_MONTH}\",\"reviewDate\":\"${NEXT_MONTH}\",\"nextReviewDate\":\"${NEXT_YEAR}\",\"comments\":\"Avis favorable du Comité d'Éthique. Protocole approuvé sous réserve de la mise à jour du formulaire de consentement (version 1.1). Prochaine révision annuelle en ${NEXT_YEAR}.\"}" > /dev/null
        log_ok "Décision CE → APPROVED (avis favorable)"
        log_info "Prochaine révision annuelle : $NEXT_YEAR"

        # Mettre l'étude en APPROVED puis ONGOING
        if [ -n "$STUDY_ID" ]; then
            api_patch "${BASE_STUDY}/${STUDY_ID}/status" "$ADMIN_TOKEN" \
              "{\"status\":\"APPROVED\",\"statusDate\":\"${NEXT_MONTH}\",\"comment\":\"Approbation CE reçue — avis favorable\"}" > /dev/null
            log_ok "Étude → APPROVED"

            api_patch "${BASE_STUDY}/${STUDY_ID}/status" "$ADMIN_TOKEN" \
              "{\"status\":\"ONGOING\",\"statusDate\":\"${NEXT_MONTH}\",\"comment\":\"Démarrage officiel de l'étude — premier patient inclus\"}" > /dev/null
            log_ok "Étude → ONGOING (étude en cours)"
        fi
    else
        log_warn "Dossier CE non créé — $(json_get "$ETHICS_RESP" "detail")"
    fi
elif [ "$ETHICS_UP" = false ]; then
    log_warn "ethics-service absent — Phase 3 ignorée"
else
    log_warn "Pas d'ID étude — Phase 3 ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 4 — Ouverture dossier CTC
# ════════════════════════════════════════════════════════════════════════════
CTC_ID=""

if [ "$CTC_UP" = true ] && [ -n "$STUDY_ID" ]; then
    log_step "Phase 4 — Dossier CTC (Centre de Thérapie Cellulaire)"

    CTC_RESP=$(api_post "${BASE_CTC}/desk-requests" "$ADMIN_TOKEN" "{
      \"studyId\": \"${STUDY_ID}\",
      \"deskType\": \"ACADEMIC\",
      \"requestorName\": \"Thomas Bernard\",
      \"requestorEmail\": \"pm@saintluc.be\",
      \"requestorOrganization\": \"CTC — Cliniques Universitaires Saint-Luc\",
      \"requestType\": \"NEW_STUDY\",
      \"priority\": \"HIGH\",
      \"deadline\": \"${NEXT_MONTH}\",
      \"notes\": \"Ouverture du dossier CTC pour l'étude ONCO-2026-01. Protocole approuvé CE. Mise en place du monitoring et coordination des évaluations. Contact pharmacien : Dr. Marc Lefèvre.\"
    }")

    CTC_ID=$(json_get "$CTC_RESP" "id")
    if [ -n "$CTC_ID" ] && [ "$CTC_ID" != "None" ]; then
        log_ok "Dossier CTC créé — ID: $CTC_ID"
        log_info "Type : NEW_STUDY | Priorité : HIGH | Deadline : $NEXT_MONTH"
    else
        log_warn "Dossier CTC non créé — $(json_get "$CTC_RESP" "detail")"
    fi
elif [ "$CTC_UP" = false ]; then
    log_warn "ctc-service absent — Phase 4 ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# PHASE 5 — Mise en place pharmacie
# ════════════════════════════════════════════════════════════════════════════
if [ "$PHARMACY_UP" = true ] && [ -n "$STUDY_ID" ]; then
    log_step "Phase 5 — Pharmacie (médicament expérimental + stock)"

    # Créer le médicament expérimental (IMP)
    DRUG_RESP=$(api_post "${BASE_PHARMACY}/drugs" "$ADMIN_TOKEN" "{
      \"studyId\": \"${STUDY_ID}\",
      \"drugName\": \"NK-Cell Allogénique (Lot A)\",
      \"inn\": \"Cellules Natural Killer allogéniques\",
      \"dosage\": \"50×10⁶ cellules / perfusion IV\",
      \"form\": \"INJECTION\",
      \"manufacturer\": \"CTC Biotherapy — Saint-Luc\",
      \"batchNumber\": \"NK2026-A001\",
      \"expiryDate\": \"${NEXT_YEAR}\",
      \"storageConditions\": \"Azote liquide (-196°C) — transport sous cryoconservation\",
      \"category\": \"IMP\"
    }")

    DRUG_ID=$(json_get "$DRUG_RESP" "id")
    if [ -n "$DRUG_ID" ] && [ "$DRUG_ID" != "None" ]; then
        log_ok "Médicament expérimental créé — ID: $DRUG_ID"
        log_info "NK-Cell Allogénique | IMP | Lot NK2026-A001 | Cryoconservé"

        # Réceptionner un lot en pharmacie
        STOCK_RESP=$(api_post "${BASE_PHARMACY}/stocks/receive" "$ADMIN_TOKEN" "{
          \"drugId\": \"${DRUG_ID}\",
          \"studyId\": \"${STUDY_ID}\",
          \"quantity\": 10,
          \"unit\": \"unités de perfusion\",
          \"receivedDate\": \"${TODAY}\",
          \"expiryDate\": \"${NEXT_YEAR}\",
          \"batchNumber\": \"NK2026-A001\",
          \"location\": \"Cryoconservateur C3 — Pharmacie Essais Cliniques\"
        }")

        STOCK_ID=$(json_get "$STOCK_RESP" "id")
        if [ -n "$STOCK_ID" ] && [ "$STOCK_ID" != "None" ]; then
            log_ok "Stock réceptionné — 10 unités en quarantaine"
            log_info "Emplacement : Cryoconservateur C3"

            # Libérer le stock (quarantaine → disponible)
            api_patch "${BASE_PHARMACY}/stocks/${STOCK_ID}/status" "$ADMIN_TOKEN" \
              "{\"status\":\"AVAILABLE\"}" > /dev/null
            log_ok "Stock libéré → AVAILABLE (QC validé)"
        else
            log_warn "Stock non créé — $(json_get "$STOCK_RESP" "detail")"
        fi
    else
        log_warn "Médicament non créé — $(json_get "$DRUG_RESP" "detail")"
    fi

    # Créer également un placebo
    PLACEBO_RESP=$(api_post "${BASE_PHARMACY}/drugs" "$ADMIN_TOKEN" "{
      \"studyId\": \"${STUDY_ID}\",
      \"drugName\": \"Placebo Salin (Solution NaCl 0.9%)\",
      \"inn\": \"Chlorure de sodium\",
      \"dosage\": \"100 mL / perfusion IV\",
      \"form\": \"SOLUTION\",
      \"manufacturer\": \"Pharmacie Centrale CUSL\",
      \"batchNumber\": \"PLC2026-001\",
      \"expiryDate\": \"${NEXT_YEAR}\",
      \"storageConditions\": \"Température ambiante (15-25°C)\",
      \"category\": \"PLACEBO\"
    }")

    PLACEBO_ID=$(json_get "$PLACEBO_RESP" "id")
    if [ -n "$PLACEBO_ID" ] && [ "$PLACEBO_ID" != "None" ]; then
        log_ok "Placebo créé — Solution NaCl 0.9% | Lot PLC2026-001"
    fi

elif [ "$PHARMACY_UP" = false ]; then
    log_warn "pharmacy-service absent — Phase 5 ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# RÉSUMÉ FINAL
# ════════════════════════════════════════════════════════════════════════════
echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║            Initialisation démo terminée !                  ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "${BOLD}  URL : https://clinitrak.gilmotech.be${NC}"
echo ""
echo -e "${BOLD}  Comptes utilisateurs (mot de passe : ${DEMO_PASSWORD})${NC}"
echo ""
printf "  %-35s %-25s %s\n" "EMAIL" "NOM" "RÔLE"
printf "  %-35s %-25s %s\n" "-----------------------------------" "-------------------------" "------------------------"
printf "  %-35s %-25s %s\n" "admin@saintluc.be"           "Gilles Gilmoreau"    "SUPER_ADMIN"
printf "  %-35s %-25s %s\n" "coordination@saintluc.be"    "Marie Dupont"        "ADMIN_TENANT"
printf "  %-35s %-25s %s\n" "secretariat.ce@saintluc.be"  "Sophie Martin"       "CE_SECRETARY"
printf "  %-35s %-25s %s\n" "coordinateur.ce@saintluc.be" "Dr. Pierre Leclerc"  "CE_COORDINATOR"
printf "  %-35s %-25s %s\n" "cra@saintluc.be"             "Anna Schmidt"        "CTC_CRA"
printf "  %-35s %-25s %s\n" "pm@saintluc.be"              "Thomas Bernard"      "CTC_PM"
printf "  %-35s %-25s %s\n" "cofi@saintluc.be"            "Julie Fontaine"      "CTC_COFI"
printf "  %-35s %-25s %s\n" "pharmacien@saintluc.be"      "Dr. Marc Lefèvre"    "PHARMACIST"
printf "  %-35s %-25s %s\n" "investigateur@saintluc.be"   "Pr. François Renard" "INVESTIGATOR"
printf "  %-35s %-25s %s\n" "externe@pharmalab.com"       "John Smith"          "EXTERNAL"
echo ""

if [ -n "$STUDY_ID" ]; then
    echo -e "${BOLD}  Données créées :${NC}"
    echo "  • Étude   : ONCO-2026-01 (ID: $STUDY_ID) — Statut: ONGOING"
    [ -n "$ETHICS_ID" ] && echo "  • Éthique : Dossier initial approuvé (ID: $ETHICS_ID)"
    [ -n "$CTC_ID" ]    && echo "  • CTC     : Dossier ouvert priorité HIGH (ID: $CTC_ID)"
    echo "  • Pharmacie : NK-Cell IMP + Placebo, stock disponible"
    echo ""
fi

echo -e "${BOLD}  Connexion API :${NC}"
echo "  curl -X POST https://clinitrak.gilmotech.be/api/v1/auth/login \\"
echo "    -H 'Content-Type: application/json' \\"
echo "    -H 'X-Tenant-ID: saintluc' \\"
echo "    -d '{\"email\":\"admin@saintluc.be\",\"password\":\"${DEMO_PASSWORD}\"}'"
echo ""
echo -e "${BOLD}  Swagger UI :${NC} https://clinitrak.gilmotech.be/swagger-ui/"
echo ""

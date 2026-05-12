#!/usr/bin/env bash
# =============================================================================
# reset-demo.sh — Supprime les données générées par demo-data.sh
#
# Supprime :
#   - Les 10 comptes utilisateurs de démo (clinitrak_auth)
#   - L'étude ONCO-2026-01 et tout son historique (clinitrak_study)
#   - Les dossiers CE associés (clinitrak_ethics)
#   - Les dossiers CTC associés (clinitrak_ctc)
#   - Les médicaments et stocks pharmacie (clinitrak_pharmacy)
#
# Usage :
#   bash /home/claude-worker/clinitrak/scripts/reset-demo.sh
#
# Prérequis : PostgreSQL accessible sur localhost:5433
# =============================================================================

set -euo pipefail

PG="psql -h localhost -p 5433 -U clinitrak"
GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; BOLD='\033[1m'; NC='\033[0m'

log_ok()   { echo -e "${GREEN}[OK]${NC}    $*"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC}  $*"; }
log_err()  { echo -e "${RED}[ERREUR]${NC} $*"; }
log_step() { echo -e "\n${BOLD}── $* ──────────────────────────────────────────${NC}"; }

sql() {
    local db=$1 query=$2
    $PG -d "$db" -c "$query" -t -q 2>/dev/null
}

sql_count() {
    local db=$1 query=$2
    $PG -d "$db" -c "$query" -t -q 2>/dev/null | tr -d ' \n'
}

echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║      CliniTrak — Réinitialisation des données de démo     ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo -e "  Ce script supprime ${RED}définitivement${NC} :"
echo "  • 10 comptes utilisateurs de démo (saintluc)"
echo "  • Étude ONCO-2026-01 + historique de statuts"
echo "  • Dossiers Comité d'Éthique liés"
echo "  • Dossiers CTC liés"
echo "  • Médicaments et stocks pharmacie liés"
echo ""

# Vérifier que PostgreSQL est disponible
if ! pg_isready -h localhost -p 5433 -q 2>/dev/null; then
    log_err "PostgreSQL non disponible sur localhost:5433"
    exit 1
fi

# Vérifier la connexion
if ! $PG -d clinitrak_auth -c "SELECT 1" -q > /dev/null 2>&1; then
    log_err "Connexion PostgreSQL échouée. Vérifier le mot de passe (variable PGPASSWORD)."
    echo "  Exemple : PGPASSWORD=monpass bash reset-demo.sh"
    exit 1
fi

# Compter ce qui existe avant suppression
STUDY_COUNT=$(sql_count "clinitrak_study" \
    "SELECT COUNT(*) FROM clinical_studies WHERE acronym = 'ONCO-2026-01' AND deleted_at IS NULL;")
USER_COUNT=$(sql_count "clinitrak_auth" \
    "SELECT COUNT(*) FROM users WHERE email IN (
        'admin@saintluc.be','coordination@saintluc.be','secretariat.ce@saintluc.be',
        'coordinateur.ce@saintluc.be','cra@saintluc.be','pm@saintluc.be',
        'cofi@saintluc.be','pharmacien@saintluc.be','investigateur@saintluc.be',
        'externe@pharmalab.com') AND deleted_at IS NULL;")

echo -e "  Données trouvées en base :"
echo "  • Utilisateurs de démo : ${USER_COUNT:-0}"
echo "  • Étude ONCO-2026-01   : ${STUDY_COUNT:-0}"
echo ""

if [ "${STUDY_COUNT:-0}" = "0" ] && [ "${USER_COUNT:-0}" = "0" ]; then
    log_warn "Aucune donnée de démo trouvée. Rien à supprimer."
    exit 0
fi

read -rp "Confirmer la suppression ? (oui/non) : " CONFIRM
[ "$CONFIRM" != "oui" ] && { echo "Annulé."; exit 0; }

# ── Récupérer l'UUID de l'étude ───────────────────────────────────────────────
STUDY_ID=$(sql_count "clinitrak_study" \
    "SELECT id FROM clinical_studies WHERE acronym = 'ONCO-2026-01' LIMIT 1;")

# ════════════════════════════════════════════════════════════════════════════
# PHARMACIE — supprimer stocks puis médicaments (contrainte FK)
# ════════════════════════════════════════════════════════════════════════════
log_step "Pharmacie (clinitrak_pharmacy)"

if [ -n "$STUDY_ID" ]; then
    # Dispensations liées aux stocks de l'étude
    DISP=$(sql "clinitrak_pharmacy" \
        "DELETE FROM pharmacy_dispensations
         WHERE drug_stock_id IN (
             SELECT id FROM pharmacy_stocks
             WHERE drug_id IN (
                 SELECT id FROM pharmacy_drugs WHERE study_id = '${STUDY_ID}'
             )
         ) RETURNING id;" | grep -c "^" 2>/dev/null || echo 0)
    [ "${DISP:-0}" -gt 0 ] && log_ok "Dispensations supprimées : $DISP" || log_warn "Aucune dispensation"

    # Emergency unblinding liés aux médicaments
    sql "clinitrak_pharmacy" \
        "DELETE FROM pharmacy_emergency_unblinding
         WHERE drug_id IN (
             SELECT id FROM pharmacy_drugs WHERE study_id = '${STUDY_ID}'
         );" > /dev/null 2>&1 || true

    # Stocks
    STOCKS=$(sql "clinitrak_pharmacy" \
        "DELETE FROM pharmacy_stocks
         WHERE drug_id IN (
             SELECT id FROM pharmacy_drugs WHERE study_id = '${STUDY_ID}'
         ) RETURNING id;" | grep -c "^" 2>/dev/null || echo 0)
    log_ok "Stocks supprimés : ${STOCKS:-0}"

    # Billing pharmacie
    sql "clinitrak_pharmacy" \
        "DELETE FROM pharmacy_billing WHERE study_id = '${STUDY_ID}';" > /dev/null 2>&1 || true

    # Médicaments
    DRUGS=$(sql "clinitrak_pharmacy" \
        "DELETE FROM pharmacy_drugs WHERE study_id = '${STUDY_ID}' RETURNING id;" \
        | grep -c "^" 2>/dev/null || echo 0)
    log_ok "Médicaments supprimés : ${DRUGS:-0}"
else
    log_warn "Pas d'étude trouvée — pharmacie ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# CTC — supprimer tous les dossiers liés à l'étude
# ════════════════════════════════════════════════════════════════════════════
log_step "CTC (clinitrak_ctc)"

if [ -n "$STUDY_ID" ]; then
    for table in ctc_statistics_requests ctc_quality_events ctc_monitoring_visits \
                 ctc_sponsor_cra_assignments ctc_financial_contracts ctc_desk_requests; do
        N=$(sql "clinitrak_ctc" \
            "DELETE FROM ${table} WHERE study_id = '${STUDY_ID}' RETURNING id;" \
            | grep -c "^" 2>/dev/null || echo 0)
        [ "${N:-0}" -gt 0 ] && log_ok "${table} : ${N} ligne(s) supprimée(s)" || true
    done

    # ctc_sponsor_studies utilise study_id comme référence externe
    sql "clinitrak_ctc" \
        "DELETE FROM ctc_sponsor_studies WHERE study_id = '${STUDY_ID}';" > /dev/null 2>&1 || true

    log_ok "Dossiers CTC nettoyés"
else
    log_warn "Pas d'étude trouvée — CTC ignoré"
fi

# ════════════════════════════════════════════════════════════════════════════
# ÉTHIQUE — supprimer dossiers CE et réunions liés
# ════════════════════════════════════════════════════════════════════════════
log_step "Comité d'Éthique (clinitrak_ethics)"

if [ -n "$STUDY_ID" ]; then
    # Récupérer les IDs des reviews liées à l'étude
    REVIEW_IDS=$(sql "clinitrak_ethics" \
        "SELECT string_agg(id::text, ',') FROM ethics_reviews WHERE study_id = '${STUDY_ID}';" \
        | tr -d ' ')

    if [ -n "$REVIEW_IDS" ] && [ "$REVIEW_IDS" != "" ]; then
        # Correspondances liées aux reviews
        sql "clinitrak_ethics" \
            "DELETE FROM correspondence WHERE review_id IN (${REVIEW_IDS//,/','});" \
            > /dev/null 2>&1 || true

        # Annual reports
        sql "clinitrak_ethics" \
            "DELETE FROM annual_reports WHERE review_id IN (${REVIEW_IDS//,/','});" \
            > /dev/null 2>&1 || true
    fi

    # Ethics sequences
    sql "clinitrak_ethics" \
        "DELETE FROM ethics_sequences WHERE study_id = '${STUDY_ID}';" \
        > /dev/null 2>&1 || true

    # Reviews elles-mêmes
    REVIEWS=$(sql "clinitrak_ethics" \
        "DELETE FROM ethics_reviews WHERE study_id = '${STUDY_ID}' RETURNING id;" \
        | grep -c "^" 2>/dev/null || echo 0)
    log_ok "Dossiers CE supprimés : ${REVIEWS:-0}"
else
    log_warn "Pas d'étude trouvée — éthique ignorée"
fi

# ════════════════════════════════════════════════════════════════════════════
# ÉTUDE — supprimer l'étude et son historique
# ════════════════════════════════════════════════════════════════════════════
log_step "Étude (clinitrak_study)"

if [ -n "$STUDY_ID" ]; then
    sql "clinitrak_study" \
        "DELETE FROM study_status_history WHERE study_id = '${STUDY_ID}';" > /dev/null
    sql "clinitrak_study" \
        "DELETE FROM study_contacts WHERE study_id = '${STUDY_ID}';" > /dev/null
    sql "clinitrak_study" \
        "DELETE FROM study_patients WHERE study_id = '${STUDY_ID}';" > /dev/null
    sql "clinitrak_study" \
        "DELETE FROM submissions WHERE study_id = '${STUDY_ID}';" > /dev/null 2>&1 || true
    sql "clinitrak_study" \
        "DELETE FROM clinical_studies WHERE id = '${STUDY_ID}';" > /dev/null
    log_ok "Étude ONCO-2026-01 supprimée (ID: $STUDY_ID)"
else
    log_warn "Étude ONCO-2026-01 introuvable en base — déjà supprimée ?"
fi

# ════════════════════════════════════════════════════════════════════════════
# UTILISATEURS — supprimer les 10 comptes de démo
# ════════════════════════════════════════════════════════════════════════════
log_step "Utilisateurs de démo (clinitrak_auth)"

DEMO_EMAILS="'admin@saintluc.be','coordination@saintluc.be','secretariat.ce@saintluc.be',
             'coordinateur.ce@saintluc.be','cra@saintluc.be','pm@saintluc.be',
             'cofi@saintluc.be','pharmacien@saintluc.be','investigateur@saintluc.be',
             'externe@pharmalab.com'"

# Récupérer les UUIDs des users démo
USER_IDS=$(sql "clinitrak_auth" \
    "SELECT string_agg(id::text, ',') FROM users WHERE email IN (${DEMO_EMAILS});" \
    | tr -d ' ')

if [ -n "$USER_IDS" ] && [ "$USER_IDS" != "" ]; then
    # Supprimer les tokens de refresh
    sql "clinitrak_auth" \
        "DELETE FROM refresh_tokens WHERE user_id IN (${USER_IDS//,/','});" > /dev/null

    # Supprimer les rôles associés
    sql "clinitrak_auth" \
        "DELETE FROM user_roles WHERE user_id IN (${USER_IDS//,/','});" > /dev/null

    # Supprimer les audit logs de ces users
    sql "clinitrak_auth" \
        "DELETE FROM audit_logs WHERE user_id IN (${USER_IDS//,/','});" > /dev/null 2>&1 || true

    # Supprimer les users
    DELETED=$(sql "clinitrak_auth" \
        "DELETE FROM users WHERE email IN (${DEMO_EMAILS}) RETURNING email;" \
        | grep -c "@" 2>/dev/null || echo 0)
    log_ok "${DELETED:-0} compte(s) supprimé(s)"
else
    log_warn "Aucun compte de démo trouvé en base"
fi

# ════════════════════════════════════════════════════════════════════════════
# RÉSUMÉ
# ════════════════════════════════════════════════════════════════════════════
echo ""
echo -e "${BOLD}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${BOLD}║            Réinitialisation terminée !                    ║${NC}"
echo -e "${BOLD}╚═══════════════════════════════════════════════════════════╝${NC}"
echo ""
echo "  Pour recréer les données de démo :"
echo "  bash /home/claude-worker/clinitrak/scripts/demo-data.sh"
echo ""

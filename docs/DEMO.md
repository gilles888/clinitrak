# CliniTrak — Guide de démonstration

## Scénario de démo

**Étude fictive** : ONCO-2026-01 — _Évaluation de l'immunothérapie NK-Cell_
**Institution** : Cliniques Universitaires Saint-Luc (`saintluc`)
**URL** : https://clinitrak.gilmotech.be

Le jeu de données simule le **cycle de vie complet** d'une étude clinique de phase II en oncologie, depuis la soumission initiale jusqu'à la mise à disposition du médicament expérimental en pharmacie.

---

## Initialisation rapide

```bash
# Lancer une seule fois (crée les 10 comptes + les données)
bash /home/claude-worker/clinitrak/scripts/demo-data.sh
```

---

## Comptes utilisateurs de démo

**Mot de passe commun** : `Demo@Saintluc2026!`

| Email | Nom | Rôle | Accès |
|-------|-----|------|-------|
| `admin@saintluc.be` | Gilles Gilmoreau | SUPER_ADMIN | Tout |
| `coordination@saintluc.be` | Marie Dupont | ADMIN_TENANT | Gestion utilisateurs + consultation |
| `secretariat.ce@saintluc.be` | Sophie Martin | CE_SECRETARY | Dossiers éthiques + soumission |
| `coordinateur.ce@saintluc.be` | Dr. Pierre Leclerc | CE_COORDINATOR | Éthique + approbation CE |
| `pm@saintluc.be` | Thomas Bernard | CTC_PM | Études + CTC + facturation (lecture) |
| `cra@saintluc.be` | Anna Schmidt | CTC_CRA | CTC + études |
| `cofi@saintluc.be` | Julie Fontaine | CTC_COFI | Facturation complète |
| `pharmacien@saintluc.be` | Dr. Marc Lefèvre | PHARMACIST | Pharmacie complète |
| `investigateur@saintluc.be` | Pr. François Renard | INVESTIGATOR | Études (lecture) + soumission CE |
| `externe@pharmalab.com` | John Smith | EXTERNAL | Lecture seule (sponsor) |

---

## Flux de démonstration

### Étape 1 — Connexion (tous les rôles)

```
URL     : https://clinitrak.gilmotech.be
Tenant  : saintluc  (sélectionner ou saisir dans le champ tenant)
Email   : pm@saintluc.be
Password: Demo@Saintluc2026!
```

Le dashboard affiche les études en cours. Thomas Bernard (PM) voit l'étude ONCO-2026-01 avec statut **ONGOING**.

---

### Étape 2 — Vue PM : Tableau de bord de l'étude

**Connexion** : `pm@saintluc.be`

1. Cliquer sur **Études** dans la sidebar
2. Ouvrir **ONCO-2026-01** — _Évaluation de l'immunothérapie NK-Cell_
3. Observer les informations :
   - Phase : II | 45 patients cibles
   - Investigateur principal : Pr. François Renard
   - Promoteur : Cliniques Universitaires Saint-Luc (académique, sponsor interne)
   - Numéro EudraCT : 2026-001234-12
   - Statut : **ONGOING**

> **Point de démo** : montrer la traçabilité du cycle de vie (DRAFT → SUBMITTED → APPROVED → ONGOING).

---

### Étape 3 — Vue Comité d'Éthique

**Connexion** : `coordinateur.ce@saintluc.be`

1. Aller dans **Éthique**
2. Ouvrir le dossier de soumission initiale (INITIAL) de ONCO-2026-01
3. Montrer :
   - Rapporteur : Dr. Pierre Leclerc
   - Décision : **APPROVED**
   - Date de décision : (mois suivant la soumission)
   - Prochaine révision annuelle planifiée

> **Point de démo** : la décision CE est tracée et déclenche automatiquement le passage de l'étude en APPROVED puis ONGOING.

---

### Étape 4 — Vue Pharmacie

**Connexion** : `pharmacien@saintluc.be`

1. Aller dans **Pharmacie**
2. Voir les médicaments de l'étude ONCO-2026-01 :
   - **NK-Cell Allogénique (Lot A)** — IMP | Injection | Cryoconservé (-196°C)
   - **Placebo Salin NaCl 0.9%** — PLACEBO | Solution IV
3. Ouvrir le stock NK-Cell :
   - 10 unités réceptionnées | Lot NK2026-A001
   - Statut : **AVAILABLE** (libéré après contrôle qualité)
   - Emplacement : Cryoconservateur C3

> **Point de démo** : la chaîne de traçabilité médicament — lot — stock — emplacement.

---

### Étape 5 — Vue CTC

**Connexion** : `cra@saintluc.be`

1. Aller dans **CTC** → Demandes desk
2. Ouvrir la demande pour ONCO-2026-01 :
   - Type : NEW_STUDY | Priorité : HIGH
   - Demandeur : Thomas Bernard (pm@saintluc.be)
   - Deadline : (mois suivant)
   - Notes de coordination

> **Point de démo** : le workflow CTC est déclenché après l'approbation CE.

---

### Étape 6 — Vue Externe (sponsor)

**Connexion** : `externe@pharmalab.com`

1. L'accès est **lecture seule** — pas de bouton de création
2. Voir les études partagées : ONCO-2026-01 visible
3. Montrer que la sidebar est filtrée selon le rôle EXTERNAL

> **Point de démo** : le RBAC filtre automatiquement les actions disponibles selon le rôle.

---

### Étape 7 — Administration (super-admin)

**Connexion** : `admin@saintluc.be`

1. Aller dans **Administration**
2. **Gestion des utilisateurs** : voir les 10 comptes créés avec leurs rôles
3. **Logs d'audit** : chaque action (création étude, décision CE, réception stock) est tracée avec timestamp et utilisateur
4. **Health check** : statut de tous les services

> **Point de démo** : traçabilité totale — qui a fait quoi et quand.

---

## API — Démonstration technique

### Obtenir un token JWT

```bash
TOKEN=$(curl -s -X POST https://clinitrak.gilmotech.be/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-ID: saintluc" \
  -d '{"email":"admin@saintluc.be","password":"Demo@Saintluc2026!"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['accessToken'])")
```

### Lister les études

```bash
curl -s https://clinitrak.gilmotech.be/api/v1/studies \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: saintluc" | python3 -m json.tool
```

### Chercher l'étude ONCO-2026-01

```bash
curl -s "https://clinitrak.gilmotech.be/api/v1/studies?acronym=ONCO-2026-01" \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-Tenant-ID: saintluc" | python3 -m json.tool
```

### Swagger UI interactif

```
https://clinitrak.gilmotech.be/swagger-ui/
```

Sélectionner un service dans le menu déroulant, cliquer **Authorize**, coller le token JWT.

---

## Réinitialiser la démo

Pour repartir sur une base propre (suppression + recréation) :

```bash
# 1. Vider les bases PostgreSQL (DESTRUCTIF)
sudo -u postgres psql -p 5433 -c "
  DROP DATABASE clinitrak_study;
  DROP DATABASE clinitrak_ethics;
  DROP DATABASE clinitrak_ctc;
  DROP DATABASE clinitrak_pharmacy;
  DROP DATABASE clinitrak_auth;
  CREATE DATABASE clinitrak_auth    OWNER clinitrak;
  CREATE DATABASE clinitrak_study   OWNER clinitrak;
  CREATE DATABASE clinitrak_ethics  OWNER clinitrak;
  CREATE DATABASE clinitrak_ctc     OWNER clinitrak;
  CREATE DATABASE clinitrak_pharmacy OWNER clinitrak;
"

# 2. Redémarrer les services (Liquibase recréera les tables au démarrage)
sudo systemctl restart clinitrak-auth clinitrak-study clinitrak-ethics clinitrak-ctc clinitrak-pharmacy

# 3. Attendre ~60 secondes puis relancer la démo
sleep 60
bash /home/claude-worker/clinitrak/scripts/demo-data.sh
```

---

## Architecture du scénario de démo

```
[Pr. Renard / INVESTIGATOR]
        │ propose une étude
        ▼
[Thomas Bernard / CTC_PM]
        │ crée l'étude dans CliniTrak (DRAFT → SUBMITTED)
        │
        ├──▶ [Sophie Martin / CE_SECRETARY]
        │         │ enregistre le dossier CE (INITIAL)
        │         ▼
        │    [Dr. Leclerc / CE_COORDINATOR]
        │         │ donne l'avis favorable (APPROVED)
        │         ▼
        │    Étude → APPROVED → ONGOING
        │
        ├──▶ [Thomas Bernard / CTC_PM]
        │         │ ouvre le dossier CTC (NEW_STUDY / HIGH)
        │         ▼
        │    [Anna Schmidt / CTC_CRA]
        │         coordonne le monitoring
        │
        └──▶ [Dr. Lefèvre / PHARMACIST]
                  │ crée le médicament expérimental (IMP)
                  │ réceptionne le stock (QUARANTINE → AVAILABLE)
                  ▼
             Stock disponible pour dispensation

[Julie Fontaine / CTC_COFI] → suit la facturation
[John Smith / EXTERNAL]     → accès lecture seule (sponsor)
[Gilles / SUPER_ADMIN]      → audit complet de toutes les actions
```

---

## Données de référence

| Élément | Valeur |
|---------|--------|
| Étude acronyme | ONCO-2026-01 |
| Titre court | Immunothérapie NK-Cell — DLBCL réfractaire |
| Phase | II |
| Patients cibles | 45 |
| Type | Interventionnel académique |
| EudraCT | 2026-001234-12 |
| CTIS | 2026-500123-41-00 |
| IMP | NK-Cell Allogénique — Lot NK2026-A001 |
| Placebo | NaCl 0.9% — Lot PLC2026-001 |
| Stock disponible | 10 unités (Cryoconservateur C3) |

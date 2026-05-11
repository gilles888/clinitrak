# CliniTrak — API Pharmacy Service

*Port: 8085 | Base URL: /api/v1/pharmacy*

## Authentification
Bearer JWT + X-Tenant-ID header (identique aux autres services)

## Endpoints

### Médicaments

#### GET /api/v1/pharmacy/drugs
Rôles: ROLE_PHARMACIST, ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: DrugResponse[]

#### POST /api/v1/pharmacy/drugs
Rôle: ROLE_PHARMACIST
Body: { studyId, drugName, inn?, dosage?, form, manufacturer?, batchNumber?, expiryDate?, storageConditions?, category, randomizationCode? }
Note: randomizationCode chiffré AES-256 avant stockage
Réponse 201: DrugResponse

### Stocks

#### GET /api/v1/pharmacy/stocks
Rôles: ROLE_PHARMACIST, ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: StockResponse[]

#### POST /api/v1/pharmacy/stocks/receive
Rôle: ROLE_PHARMACIST
Body: { drugId, studyId, quantity, unit, receivedDate, expiryDate, batchNumber, location? }
Note: status initialisé QUARANTINE
Réponse 201: StockResponse

#### PATCH /api/v1/pharmacy/stocks/{id}/status
Rôle: ROLE_PHARMACIST
Body: { status: StockStatus }
Réponse 200: StockResponse

#### POST /api/v1/pharmacy/stocks/import
Rôle: ROLE_PHARMACIST
Body: multipart/form-data, champ "file" (.csv ou .xlsx)
Format CSV: drugId,quantity,unit,receivedDate(YYYY-MM-DD),expiryDate(YYYY-MM-DD),batchNumber,location
Réponse 200: { imported: number, failed: number, errors: string[] }

### Dispensations

#### GET /api/v1/pharmacy/dispensations
Rôles: ROLE_PHARMACIST, ROLE_SUPER_ADMIN
Réponse 200: DispensationResponse[]

#### POST /api/v1/pharmacy/dispensations
Rôle: ROLE_PHARMACIST
Body: { drugId, studyId, patientCode, dispensationDate, pharmacistId, prescriberId, quantity, prescription?, visitNumber?, notes? }
Note: décrémente automatiquement le stock disponible
Réponse 201: DispensationResponse

#### GET /api/v1/pharmacy/dispensations/patient/{patientCode}
Rôles: ROLE_PHARMACIST, ROLE_SUPER_ADMIN
Réponse 200: DispensationResponse[] (historique complet)

### Dashboard & Alertes

#### GET /api/v1/pharmacy/dashboard
Rôles: ROLE_PHARMACIST, ROLE_SUPER_ADMIN
Réponse 200: { totalDrugs, availableStocks, lowStockCount, expiringIn30Days, pendingDispensations, openUnblindings, urgentAlerts[] }

#### GET /api/v1/pharmacy/alerts
Rôles: ROLE_PHARMACIST, ROLE_CTC_MANAGER, ROLE_SUPER_ADMIN
Réponse 200: { alerts: PharmacyAlert[], criticalCount }
Types: LOW_STOCK (<10 unités), EXPIRY_7 (≤7 jours), EXPIRY_30 (≤30 jours)

#### GET /api/v1/pharmacy/reports/inventory
Rôles: ROLE_PHARMACIST, ROLE_SUPER_ADMIN
Réponse 200: application/pdf — rapport d'inventaire Flying Saucer

### Levée d'aveugle d'urgence

#### POST /api/v1/pharmacy/emergency-unblinding
Rôle: ROLE_PHARMACIST
Body: { studyId, patientCode, requestedBy, reason }
Réponse 201: EmergencyUnblindingResponse (sans treatment)

#### PATCH /api/v1/pharmacy/emergency-unblinding/{id}/approve
Rôle: ROLE_PHARMACIST
Param: ?approvedBy={userId}
Note: révèle le treatment via déchiffrement AES du randomizationCode
Réponse 200: EmergencyUnblindingResponse (avec treatment)

## Enums

- **DrugCategory**: IMP | NIMP | PLACEBO
- **DrugForm**: TABLET | CAPSULE | INJECTION | SOLUTION | CREAM | OTHER
- **StockStatus**: QUARANTINE | AVAILABLE | DISPENSED | RETURNED | DESTROYED
- **DrugRegulatoryStatus**: PENDING | APPROVED | EXPIRED | RECALLED
- **BillingStatus**: DRAFT | SENT | PAID | DISPUTED | CANCELLED
- **AlertType**: LOW_STOCK | EXPIRY_30 | EXPIRY_7 | QUARANTINE

## Fonctionnalités spéciales

### Chiffrement AES-256
Le `randomizationCode` est chiffré AES/ECB/PKCS5Padding avant stockage dans `randomization_code_encrypted`. Clé via `PHARMACY_ENCRYPTION_KEY` (32 bytes).

### Alertes @Scheduled
Batch quotidien 06h00 : stocks faibles (<10 unités) + péremptions J-7/J-30.

### Import CSV/XLSX
Apache POI 5.2.5 — `WorkbookFactory.create(inputStream)` gère .csv et .xlsx.

### PDF inventaire
Flying Saucer + OpenPDF — template HTML Java inline → PDF.

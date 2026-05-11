-- ============================================================
-- CliniTrak Pharmacy Service — Initialisation du schéma
-- ============================================================

CREATE TABLE pharmacy_drugs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    drug_name VARCHAR(255) NOT NULL,
    inn VARCHAR(255),
    dosage VARCHAR(100),
    form VARCHAR(50),
    manufacturer VARCHAR(255),
    batch_number VARCHAR(100),
    expiry_date DATE,
    storage_conditions TEXT,
    category VARCHAR(50) NOT NULL,
    regulatory_status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    randomization_code_encrypted TEXT,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE pharmacy_stocks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drug_id UUID REFERENCES pharmacy_drugs(id),
    study_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 0,
    unit VARCHAR(50),
    received_date DATE,
    expiry_date DATE,
    batch_number VARCHAR(100),
    location VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'QUARANTINE',
    temperature_log TEXT,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE pharmacy_dispensations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    drug_id UUID REFERENCES pharmacy_drugs(id),
    study_id VARCHAR(255) NOT NULL,
    patient_code VARCHAR(100) NOT NULL,
    dispensation_date DATE NOT NULL,
    pharmacist_id VARCHAR(255) NOT NULL,
    prescriber_id VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    prescription VARCHAR(255),
    visit_number VARCHAR(50),
    return_date DATE,
    return_quantity INTEGER,
    notes TEXT,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE pharmacy_billing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    billing_period VARCHAR(20),
    total_cost NUMERIC(15,2),
    invoice_number VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    sent_date DATE,
    paid_date DATE,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE pharmacy_emergency_unblinding (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    patient_code VARCHAR(100) NOT NULL,
    request_date TIMESTAMP WITH TIME ZONE NOT NULL,
    requested_by VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    treatment TEXT,
    approved_by VARCHAR(255),
    approved_at TIMESTAMP WITH TIME ZONE,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Indexes pour les performances
CREATE INDEX idx_pharm_drugs_tenant ON pharmacy_drugs(tenant_id, deleted);
CREATE INDEX idx_pharm_drugs_study ON pharmacy_drugs(study_id, tenant_id);
CREATE INDEX idx_pharm_drugs_expiry ON pharmacy_drugs(expiry_date, tenant_id);
CREATE INDEX idx_pharm_stocks_tenant ON pharmacy_stocks(tenant_id, deleted);
CREATE INDEX idx_pharm_stocks_study ON pharmacy_stocks(study_id, tenant_id);
CREATE INDEX idx_pharm_stocks_expiry ON pharmacy_stocks(expiry_date, tenant_id);
CREATE INDEX idx_pharm_dispensations_patient ON pharmacy_dispensations(patient_code, tenant_id);
CREATE INDEX idx_pharm_dispensations_study ON pharmacy_dispensations(study_id, tenant_id);

-- Triggers de mise à jour automatique de updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_pharmacy_drugs_updated_at
    BEFORE UPDATE ON pharmacy_drugs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacy_stocks_updated_at
    BEFORE UPDATE ON pharmacy_stocks
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacy_dispensations_updated_at
    BEFORE UPDATE ON pharmacy_dispensations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacy_billing_updated_at
    BEFORE UPDATE ON pharmacy_billing
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_pharmacy_emergency_unblinding_updated_at
    BEFORE UPDATE ON pharmacy_emergency_unblinding
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

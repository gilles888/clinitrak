-- ============================================================
-- CliniTrak CTC Service — Initialisation du schéma de base
-- V1__init_ctc.sql
-- ============================================================

CREATE TABLE ctc_desk_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    desk_type VARCHAR(50) NOT NULL,
    request_date DATE NOT NULL,
    requestor_name VARCHAR(255) NOT NULL,
    requestor_email VARCHAR(255),
    requestor_organization VARCHAR(255),
    request_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    assigned_to VARCHAR(255),
    priority VARCHAR(50) NOT NULL DEFAULT 'MEDIUM',
    deadline DATE,
    notes TEXT,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE ctc_monitoring_visits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    visit_date DATE NOT NULL,
    visit_type VARCHAR(50) NOT NULL,
    monitor_name VARCHAR(255) NOT NULL,
    findings TEXT,
    correction_deadline DATE,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED',
    report TEXT,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE ctc_financial_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    contract_type VARCHAR(50) NOT NULL,
    contract_date DATE NOT NULL,
    amount NUMERIC(15,2) NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    billing_schedule VARCHAR(50),
    payment_terms TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE ctc_statistics_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    request_date DATE NOT NULL,
    requestor_name VARCHAR(255) NOT NULL,
    deadline DATE NOT NULL,
    analysis_type VARCHAR(50) NOT NULL,
    data_format VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    delivered_date DATE,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE ctc_sponsor_studies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL UNIQUE,
    project_manager_id VARCHAR(255),
    budget_total NUMERIC(15,2),
    budget_spent NUMERIC(15,2) DEFAULT 0,
    milestones JSONB,
    regulatory_status VARCHAR(50) NOT NULL DEFAULT 'IN_PREPARATION',
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE ctc_sponsor_cra_assignments (
    sponsor_study_id UUID REFERENCES ctc_sponsor_studies(id),
    cra_id VARCHAR(255) NOT NULL
);

CREATE TABLE ctc_quality_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    study_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_date DATE NOT NULL,
    severity VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    root_cause TEXT,
    corrective_action TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    closure_date DATE,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    last_modified_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- ============================================================
-- Index pour les performances
-- ============================================================

CREATE INDEX idx_ctc_desk_tenant ON ctc_desk_requests(tenant_id, deleted);
CREATE INDEX idx_ctc_desk_study ON ctc_desk_requests(study_id, tenant_id);
CREATE INDEX idx_ctc_visits_tenant ON ctc_monitoring_visits(tenant_id, deleted);
CREATE INDEX idx_ctc_visits_study ON ctc_monitoring_visits(study_id, tenant_id);
CREATE INDEX idx_ctc_contracts_study ON ctc_financial_contracts(study_id, tenant_id);
CREATE INDEX idx_ctc_stats_tenant ON ctc_statistics_requests(tenant_id, deleted);
CREATE INDEX idx_ctc_quality_study ON ctc_quality_events(study_id, tenant_id);
CREATE INDEX idx_ctc_sponsor_study ON ctc_sponsor_studies(study_id, tenant_id);

-- ============================================================
-- Triggers updated_at
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$ BEGIN NEW.updated_at = NOW(); RETURN NEW; END; $$ language 'plpgsql';

CREATE TRIGGER update_ctc_desk_requests_updated_at
    BEFORE UPDATE ON ctc_desk_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ctc_monitoring_visits_updated_at
    BEFORE UPDATE ON ctc_monitoring_visits
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ctc_financial_contracts_updated_at
    BEFORE UPDATE ON ctc_financial_contracts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ctc_statistics_requests_updated_at
    BEFORE UPDATE ON ctc_statistics_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ctc_sponsor_studies_updated_at
    BEFORE UPDATE ON ctc_sponsor_studies
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_ctc_quality_events_updated_at
    BEFORE UPDATE ON ctc_quality_events
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

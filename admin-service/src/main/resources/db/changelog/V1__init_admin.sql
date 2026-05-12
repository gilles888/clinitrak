-- ============================================================
-- CliniTrak Admin Service — Schéma initial
-- V1__init_admin.sql
-- ============================================================

-- Table des tenants administratifs
CREATE TABLE admin_tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    slug            VARCHAR(100) NOT NULL UNIQUE,
    domain          VARCHAR(255),
    logo_url        VARCHAR(500),
    configuration   JSONB,
    subscription_type VARCHAR(50) NOT NULL DEFAULT 'BASIC',
    status          VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at_local TIMESTAMP,
    -- Colonnes Spring Data JPA Auditing (BaseEntity)
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(255),
    updated_by      VARCHAR(255),
    deleted         BOOLEAN      NOT NULL DEFAULT FALSE,
    version         BIGINT       NOT NULL DEFAULT 0
);

-- Table des modules actifs par tenant
CREATE TABLE admin_tenant_active_modules (
    tenant_id UUID        NOT NULL REFERENCES admin_tenants(id) ON DELETE CASCADE,
    module    VARCHAR(100) NOT NULL,
    PRIMARY KEY (tenant_id, module)
);

-- Journal d'audit système (insert-only, pas de soft-delete ni de version)
CREATE TABLE admin_audit_logs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   VARCHAR(255),
    user_id     VARCHAR(255),
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(100),
    entity_id   VARCHAR(255),
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(50),
    user_agent  TEXT,
    timestamp   TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Index pour accélérer les recherches fréquentes sur les audit logs
CREATE INDEX idx_admin_audit_logs_tenant_id ON admin_audit_logs (tenant_id);
CREATE INDEX idx_admin_audit_logs_user_id   ON admin_audit_logs (user_id);
CREATE INDEX idx_admin_audit_logs_action    ON admin_audit_logs (action);
CREATE INDEX idx_admin_audit_logs_timestamp ON admin_audit_logs (timestamp DESC);
CREATE INDEX idx_admin_tenants_slug         ON admin_tenants (slug);
CREATE INDEX idx_admin_tenants_status       ON admin_tenants (status);

-- ============================================================
-- Données initiales : tenant de référence (Saint-Luc)
-- ============================================================

INSERT INTO admin_tenants (
    id, name, slug, domain, subscription_type, status,
    created_at_local, created_at, updated_at, deleted, version
) VALUES (
    'a0000000-0000-0000-0000-000000000001',
    'Cliniques Universitaires Saint-Luc',
    'saintluc',
    'www.saintluc.be',
    'ENTERPRISE',
    'ACTIVE',
    NOW(),
    NOW(),
    NOW(),
    FALSE,
    0
);

INSERT INTO admin_tenant_active_modules (tenant_id, module) VALUES
    ('a0000000-0000-0000-0000-000000000001', 'STUDIES'),
    ('a0000000-0000-0000-0000-000000000001', 'ETHICS'),
    ('a0000000-0000-0000-0000-000000000001', 'CTC'),
    ('a0000000-0000-0000-0000-000000000001', 'PHARMACY'),
    ('a0000000-0000-0000-0000-000000000001', 'EXCHANGE'),
    ('a0000000-0000-0000-0000-000000000001', 'BILLING'),
    ('a0000000-0000-0000-0000-000000000001', 'DOCUMENTS');

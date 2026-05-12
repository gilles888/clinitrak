-- =============================================================================
-- V1 : Schéma initial du exchange-service
-- =============================================================================

CREATE TABLE exchange_external_users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    organization VARCHAR(255),
    role VARCHAR(50) NOT NULL,
    verified_email BOOLEAN DEFAULT FALSE,
    email_verification_token VARCHAR(255),
    password_hash VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at_local TIMESTAMP,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE exchange_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_user_id UUID REFERENCES exchange_external_users(id),
    target_module VARCHAR(50) NOT NULL,
    request_type VARCHAR(50) NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    submission_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    internal_study_id VARCHAR(255),
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE exchange_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES exchange_requests(id),
    file_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500),
    file_size BIGINT,
    mime_type VARCHAR(100),
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

CREATE TABLE exchange_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    request_id UUID REFERENCES exchange_requests(id),
    sender_id VARCHAR(255) NOT NULL,
    sender_type VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    attachment_path VARCHAR(500),
    sent_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    read_at TIMESTAMP WITH TIME ZONE,
    tenant_id VARCHAR(255) NOT NULL,
    deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    version BIGINT DEFAULT 0
);

-- Index pour les requêtes fréquentes
CREATE INDEX idx_exchange_requests_user ON exchange_requests(external_user_id);
CREATE INDEX idx_exchange_requests_tenant ON exchange_requests(tenant_id, deleted);
CREATE INDEX idx_exchange_messages_request ON exchange_messages(request_id, tenant_id);
CREATE INDEX idx_exchange_docs_request ON exchange_documents(request_id);

-- Triggers de mise à jour automatique du timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_exchange_users_updated_at
    BEFORE UPDATE ON exchange_external_users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exchange_requests_updated_at
    BEFORE UPDATE ON exchange_requests
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exchange_documents_updated_at
    BEFORE UPDATE ON exchange_documents
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_exchange_messages_updated_at
    BEFORE UPDATE ON exchange_messages
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

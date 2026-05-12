-- ============================================================
-- CliniTrak — Document Service — V1 : Schéma initial
-- ============================================================

CREATE TABLE documents (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name           VARCHAR(255)        NOT NULL,
    original_file_name  VARCHAR(255)        NOT NULL,
    content_type        VARCHAR(100),
    file_size           BIGINT,
    bucket_name         VARCHAR(100)        NOT NULL DEFAULT 'clinitrak-documents',
    object_key          VARCHAR(500)        NOT NULL,
    document_type       VARCHAR(50)         NOT NULL,
    module_source       VARCHAR(50)         NOT NULL,
    study_id            UUID,
    version             INT                 NOT NULL DEFAULT 1,
    parent_document_id  UUID                REFERENCES documents(id),
    uploaded_by         VARCHAR(255),
    description         TEXT,
    created_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP           NOT NULL DEFAULT NOW(),
    deleted_at          TIMESTAMP,
    version_lock        BIGINT              DEFAULT 0
);

CREATE INDEX idx_documents_study_id       ON documents(study_id);
CREATE INDEX idx_documents_type           ON documents(document_type);
CREATE INDEX idx_documents_parent         ON documents(parent_document_id);
CREATE INDEX idx_documents_module_source  ON documents(module_source);
CREATE INDEX idx_documents_deleted_at     ON documents(deleted_at);

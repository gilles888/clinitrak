-- ============================================================
-- CliniTrak — V2 : Données initiales (rôles système + tenant démo)
-- ============================================================

-- ----------------------------------------------------------
-- Tenant de démonstration (Cliniques Universitaires Saint-Luc)
-- ----------------------------------------------------------
INSERT INTO tenants (id, slug, name, domain, contact_email, active)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'saintluc',
    'Cliniques Universitaires Saint-Luc',
    'saintluc.clinitrak.be',
    'ctc@saintluc.be',
    TRUE
);

-- ----------------------------------------------------------
-- Permissions de base par module
-- ----------------------------------------------------------
INSERT INTO permissions (name, description, module) VALUES
-- Module Étude
('STUDY:READ',          'Consulter les études',                       'STUDY'),
('STUDY:CREATE',        'Créer une étude',                            'STUDY'),
('STUDY:UPDATE',        'Modifier une étude',                         'STUDY'),
('STUDY:DELETE',        'Archiver une étude',                         'STUDY'),
('STUDY:EXPORT',        'Exporter les données d''une étude',           'STUDY'),
-- Module Éthique
('ETHICS:READ',         'Consulter les dossiers CE',                  'ETHICS'),
('ETHICS:SUBMIT',       'Soumettre un dossier au CE',                 'ETHICS'),
('ETHICS:REVIEW',       'Réviser un dossier CE',                      'ETHICS'),
('ETHICS:APPROVE',      'Approuver/rejeter un dossier CE',            'ETHICS'),
-- Module CTC
('CTC:READ',            'Consulter les dossiers CTC',                 'CTC'),
('CTC:CREATE',          'Créer un dossier CTC',                       'CTC'),
('CTC:MANAGE',          'Gérer les dossiers CTC',                     'CTC'),
-- Module Pharmacie
('PHARMACY:READ',       'Consulter les stocks et dispensations',      'PHARMACY'),
('PHARMACY:DISPENSE',   'Enregistrer une dispensation',               'PHARMACY'),
('PHARMACY:MANAGE',     'Gérer les stocks de médicaments d''étude',   'PHARMACY'),
-- Module Facturation
('BILLING:READ',        'Consulter la facturation',                   'BILLING'),
('BILLING:CREATE',      'Créer des lignes de facturation',            'BILLING'),
('BILLING:VALIDATE',    'Valider et émettre les factures',            'BILLING'),
-- Module Documents
('DOC:READ',            'Consulter les documents',                    'DOCUMENT'),
('DOC:UPLOAD',          'Uploader des documents',                     'DOCUMENT'),
('DOC:DELETE',          'Supprimer des documents',                    'DOCUMENT'),
-- Administration
('ADMIN:USERS',         'Gérer les utilisateurs du tenant',           'ADMIN'),
('ADMIN:TENANTS',       'Gérer les tenants (super-admin)',            'ADMIN'),
('ADMIN:AUDIT',         'Consulter les logs d''audit',                'ADMIN');

-- ----------------------------------------------------------
-- Rôles système globaux (tenant_id = NULL)
-- ----------------------------------------------------------
INSERT INTO roles (id, name, description, tenant_id) VALUES
('10000000-0000-0000-0000-000000000001', 'ROLE_SUPER_ADMIN',    'Administrateur global de la plateforme',         NULL),
('10000000-0000-0000-0000-000000000002', 'ROLE_ADMIN_TENANT',   'Administrateur d''un tenant (institution)',      NULL),
('10000000-0000-0000-0000-000000000003', 'ROLE_CE_SECRETARY',   'Secrétariat du Comité d''Éthique',              NULL),
('10000000-0000-0000-0000-000000000004', 'ROLE_CE_COORDINATOR', 'Coordinateur du Comité d''Éthique',             NULL),
('10000000-0000-0000-0000-000000000005', 'ROLE_CTC_DESK',       'Secrétariat CTC',                               NULL),
('10000000-0000-0000-0000-000000000006', 'ROLE_CTC_CRA',        'Clinical Research Associate',                   NULL),
('10000000-0000-0000-0000-000000000007', 'ROLE_CTC_PM',         'Project Manager CTC',                           NULL),
('10000000-0000-0000-0000-000000000008', 'ROLE_CTC_COFI',       'Coordinateur financier CTC',                    NULL),
('10000000-0000-0000-0000-000000000009', 'ROLE_PHARMACIST',     'Pharmacien responsable de l''étude',            NULL),
('10000000-0000-0000-0000-000000000010', 'ROLE_INVESTIGATOR',   'Investigateur principal / co-investigateur',    NULL),
('10000000-0000-0000-0000-000000000011', 'ROLE_EXTERNAL',       'Collaborateur externe (lecture seule)',         NULL);

-- ----------------------------------------------------------
-- Attribution des permissions aux rôles
-- ----------------------------------------------------------

-- SUPER_ADMIN : toutes les permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000001', id FROM permissions;

-- ADMIN_TENANT : gestion des utilisateurs + consultation complète
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000002', id FROM permissions
WHERE name IN ('ADMIN:USERS', 'ADMIN:AUDIT', 'STUDY:READ', 'ETHICS:READ', 'CTC:READ', 'DOC:READ');

-- CE_SECRETARY : gestion dossiers éthique
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000003', id FROM permissions
WHERE name IN ('ETHICS:READ', 'ETHICS:SUBMIT', 'STUDY:READ', 'DOC:READ', 'DOC:UPLOAD');

-- CE_COORDINATOR : review + approbation éthique
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000004', id FROM permissions
WHERE name IN ('ETHICS:READ', 'ETHICS:REVIEW', 'ETHICS:APPROVE', 'STUDY:READ', 'DOC:READ', 'DOC:UPLOAD', 'ADMIN:AUDIT');

-- CTC_DESK : consultation + création CTC
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000005', id FROM permissions
WHERE name IN ('CTC:READ', 'CTC:CREATE', 'STUDY:READ', 'DOC:READ', 'DOC:UPLOAD');

-- CTC_CRA : gestion complète CTC
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000006', id FROM permissions
WHERE name IN ('CTC:READ', 'CTC:CREATE', 'CTC:MANAGE', 'STUDY:READ', 'STUDY:UPDATE', 'DOC:READ', 'DOC:UPLOAD');

-- CTC_PM : gestion études + CTC + export
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000007', id FROM permissions
WHERE name IN ('STUDY:READ', 'STUDY:CREATE', 'STUDY:UPDATE', 'STUDY:EXPORT',
               'CTC:READ', 'CTC:CREATE', 'CTC:MANAGE',
               'BILLING:READ', 'DOC:READ', 'DOC:UPLOAD', 'ADMIN:AUDIT');

-- CTC_COFI : facturation
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000008', id FROM permissions
WHERE name IN ('BILLING:READ', 'BILLING:CREATE', 'BILLING:VALIDATE', 'STUDY:READ', 'CTC:READ', 'DOC:READ');

-- PHARMACIST : gestion pharmacie
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000009', id FROM permissions
WHERE name IN ('PHARMACY:READ', 'PHARMACY:DISPENSE', 'PHARMACY:MANAGE', 'STUDY:READ', 'DOC:READ');

-- INVESTIGATOR : consultation études + soumission
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000010', id FROM permissions
WHERE name IN ('STUDY:READ', 'ETHICS:READ', 'ETHICS:SUBMIT', 'DOC:READ', 'DOC:UPLOAD');

-- EXTERNAL : lecture seule sur les études
INSERT INTO role_permissions (role_id, permission_id)
SELECT '10000000-0000-0000-0000-000000000011', id FROM permissions
WHERE name IN ('STUDY:READ', 'DOC:READ');

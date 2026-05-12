-- Création des bases de données pour chaque service CliniTrak
-- Ce script est exécuté au premier démarrage du conteneur PostgreSQL

-- La base clinitrak_auth est créée par POSTGRES_DB dans docker-compose
-- On crée les autres ici
CREATE DATABASE clinitrak_study;
CREATE DATABASE clinitrak_ethics;
CREATE DATABASE clinitrak_ctc;
CREATE DATABASE clinitrak_pharmacy;
CREATE DATABASE clinitrak_exchange;
CREATE DATABASE clinitrak_billing;
CREATE DATABASE clinitrak_document;
CREATE DATABASE clinitrak_batch;
CREATE DATABASE clinitrak_notification;
CREATE DATABASE clinitrak_admin;

-- Accorder les droits à l'utilisateur clinitrak
GRANT ALL PRIVILEGES ON DATABASE clinitrak_study TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_ethics TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_ctc TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_pharmacy TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_exchange TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_billing TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_document TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_batch TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_notification TO clinitrak;
GRANT ALL PRIVILEGES ON DATABASE clinitrak_admin TO clinitrak;

SELECT 'Initialisation CliniTrak : toutes les bases créées' AS status;

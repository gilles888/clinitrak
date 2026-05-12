# ADR-003 : Stockage des documents avec MinIO plutôt que filesystem local

**Date** : 2026-05-11
**Statut** : Accepté

## Contexte

CliniTrak gère des volumes importants de documents cliniques : protocoles, consentements éclairés, rapports CE, certificats d'analyse, courriers sponsors. Ces documents doivent être stockés de façon sécurisée, durable et accessible depuis plusieurs services (document-service, ethics-service, pharmacy-service).

Trois approches ont été envisagées :

1. **Filesystem local** : stockage sur le disque du serveur d'application, accessible via chemin
2. **Base de données PostgreSQL** (`bytea` ou Large Objects) : stockage binaire en base
3. **MinIO** : serveur de stockage objet compatible API S3

## Décision

**Approche retenue : MinIO** déployé en sidecar dans le Docker Compose de développement, remplacé par un service S3-compatible (AWS S3, OVH Object Storage, Azure Blob) en production.

Le `document-service` est le seul service à écrire dans MinIO. Les autres services accèdent aux documents via des **URLs signées** (presigned URLs) à durée de vie limitée générées par document-service, sans partage de credentials MinIO.

## Raisons du choix

**Contre le filesystem local** :
- Incompatible avec le déploiement multi-instances (horizontal scaling) : les fichiers ne sont pas partagés entre les pods/conteneurs
- Sauvegarde complexe : les fichiers sont hors PostgreSQL, la cohérence sauvegarde DB + fichiers doit être gérée manuellement
- Pas de versioning natif, pas de cycle de vie automatique (rétention RGPD)
- Permissions OS à gérer manuellement (utilisateur clinitrak non-root dans le conteneur)

**Contre PostgreSQL bytea** :
- Dégrade les performances de la base (VACUUM, WAL, transactions impactés par les blobs)
- Limite pratique : blobs > 100 Mo deviennent problématiques
- PostgreSQL n'est pas optimisé pour le streaming de fichiers volumineux
- Difficultés de migration vers un stockage externe a posteriori

**Pour MinIO** :
- **API S3-compatible** : migration transparente vers AWS S3, OVH, Scaleway, Azure Blob sans changer le code (juste les credentials)
- **URLs signées** : accès temporaire aux documents sans exposer les credentials MinIO — les navigateurs téléchargent directement depuis MinIO, pas via le backend Java
- **Versioning** : historique des versions de documents natif (important pour documents réglementaires)
- **Cycle de vie** : politiques de rétention automatiques (suppression après N ans pour conformité RGPD)
- **Réplication** : MinIO supporte la réplication multi-site pour la haute disponibilité
- **Buckets par tenant** : isolation des documents par institution via bucket naming (`clinitrak-<tenant-slug>-documents`)
- **Développement local** : MinIO est identique à S3 — pas de mock, pas de double configuration

## Conséquences

- `MINIO_ACCESS_KEY` et `MINIO_SECRET_KEY` sont des secrets critiques — ne jamais les committer
- Les URLs signées ont une durée de vie configurée (défaut : 1 heure) — les liens partagés par email expirent
- En production, remplacer le service `minio` du compose par les variables d'endpoint du fournisseur cloud
- Le document-service doit gérer les erreurs de connexion MinIO avec retry (Resilience4j ou Spring Retry)
- Les buckets doivent être créés au démarrage si absents (`@PostConstruct` dans MinioService)
- La taille maximale des uploads doit être configurée côté nginx/gateway (défaut recommandé : 100 Mo)

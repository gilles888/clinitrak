# ADR-001 : Stratégie multi-tenant — TenantContext (ThreadLocal) + header X-Tenant-ID

**Date** : 2026-05-09
**Statut** : Accepté

## Contexte

CliniTrak doit servir plusieurs institutions hospitalières (Saint-Luc, Erasme, ULB, etc.) avec isolation stricte des données cliniques. Trois approches ont été envisagées :

1. **Base de données séparée par tenant** : une instance PostgreSQL ou une base par hôpital
2. **Schéma PostgreSQL séparé par tenant** : un schéma (`search_path`) par hôpital, base commune
3. **Row-Level Security (RLS) applicatif** : une seule base, colonne `tenant_id` sur chaque table, filtrage dans le code Java via `TenantContext`

## Décision

**Approche retenue : Row-Level Security applicatif** avec colonne `tenant_id` sur chaque table métier, propagée via `TenantContext` (ThreadLocal) alimenté par le header HTTP `X-Tenant-ID` (slug) ou la résolution du sous-domaine.

Le `TenantFilter` (Servlet Filter, ordre 1) résout le tenant à l'entrée de chaque requête et appelle `TenantContext.setTenantId()`. Un bloc `finally` garantit le nettoyage de la valeur ThreadLocal à la fin de la requête, même en cas d'exception.

## Raisons du choix

**Contre les bases séparées** :
- Opérationnellement lourd : N instances PostgreSQL à administrer, sauvegarder, mettre à jour
- Complexité de connexion : pool de connexions dynamique par tenant, difficile avec Spring Boot standard
- Coût infrastructure élevé pour un nombre de tenants potentiellement faible (< 20 hôpitaux belges)

**Contre les schémas séparés** :
- Complexité Liquibase : les migrations doivent s'appliquer à N schémas à chaque déploiement
- `search_path` dynamique en Spring Boot nécessite une configuration non-standard du DataSource
- Difficultés avec les requêtes cross-tenant (ex : rapports agrégés pour SUPER_ADMIN)

**Pour le RLS applicatif** :
- Simplicité opérationnelle : une seule base, une seule procédure de sauvegarde/restore
- Migrations Liquibase unifiées : une seule passe par déploiement
- Performance acceptable : les index incluent `tenant_id` comme première colonne (index composites)
- Flexibilité : possibilité d'activer PostgreSQL RLS natif en complément si nécessaire
- Compatible avec les statistiques cross-tenant pour les admins SUPER_ADMIN

## Conséquences

- Chaque entité métier déclare son propre champ `tenant_id` (UUID, NOT NULL, indexed)
- Chaque `@Service` doit appeler `TenantContext.getTenantId()` et l'injecter dans les requêtes
- Les repositories utilisent des méthodes filtrées par `tenantId` ou `@Filter` Hibernate
- Les roles globaux (`ROLE_SUPER_ADMIN`) ont `tenant_id = NULL` — traitement spécial dans le code
- Les logs d'audit et les refresh tokens stockent explicitement le `tenant_id`
- Un test d'intégration `TenantIsolationTest` vérifie l'absence de fuite de données entre tenants

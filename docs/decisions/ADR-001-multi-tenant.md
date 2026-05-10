# ADR-001 : Stratégie Multi-Tenant

**Date** : 2026-05-09
**Statut** : Accepté

## Contexte

CliniTrak doit servir plusieurs institutions hospitalières (Saint-Luc, Erasme, etc.)
avec isolation des données. Trois approches ont été envisagées :

1. **Base de données séparée** par tenant
2. **Schéma PostgreSQL séparé** par tenant
3. **Row-Level Security (RLS)** — une seule base, colonne `tenant_id` sur chaque table

## Décision

**Approche retenue : Row-Level Security (RLS)** avec colonne `tenant_id`.

Le `tenantId` est propagé via `TenantContext` (ThreadLocal) alimenté par le header `X-Tenant-ID`
ou la résolution du sous-domaine. Chaque entité hérite de `BaseEntity` qui ne contient pas
`tenantId` — c'est chaque entité métier qui déclare son propre `tenantId` selon son besoin.

## Raisons

- **Simplicité opérationnelle** : une seule base à administrer, sauvegarder, migrer
- **Évolutivité horizontale** : PostgreSQL RLS natif peut compléter l'approche applicative
- **Cohérence des migrations** : Liquibase gère un seul schéma
- **Flexibilité** : possibilité de migrer vers des schémas séparés par tenant si le besoin émerge

## Conséquences

- Chaque `@Service` doit vérifier `TenantContext.getTenantId()` non-null
- Chaque requête JPA doit filtrer par `tenantId` (via méthodes repository ou `@Filter` Hibernate)
- Le `TenantContext` ThreadLocal **doit** être nettoyé dans un `finally` block après chaque requête
- Les logs d'audit et les tokens de refresh stockent le `tenantId` explicitement
- Les rôles globaux (`tenant_id = NULL`) sont distincts des rôles tenant-spécifiques

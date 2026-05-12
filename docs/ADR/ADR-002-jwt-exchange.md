# ADR-002 : JWT distinct pour les utilisateurs externes (ExchangeJwtService)

**Date** : 2026-05-11
**Statut** : Accepté

## Contexte

Le module `exchange-service` permet à des utilisateurs externes aux hôpitaux (sponsors pharmaceutiques, CROs, sous-traitants) d'accéder à un portail limité pour suivre leurs essais cliniques et échanger des documents. Ces utilisateurs ne sont pas des employés de l'institution et ne doivent pas accéder aux fonctionnalités internes.

Deux approches ont été envisagées :

1. **Réutiliser l'auth-service** : créer un rôle `ROLE_EXTERNAL` dans l'auth-service, émettre les mêmes JWT
2. **JWT distinct dans l'exchange-service** : clé secrète séparée, entité `ExternalUser` dédiée, token non reconnu par les autres services

## Décision

**Approche retenue : JWT distinct** via `ExchangeJwtService` avec une clé `EXCHANGE_JWT_SECRET` séparée de `JWT_SECRET`.

Les utilisateurs externes s'authentifient exclusivement via `/api/v1/exchange/auth/login`. Leur token JWT est signé avec `EXCHANGE_JWT_SECRET` et n'est pas valide pour les endpoints des autres services (study-service, pharmacy-service, etc.) qui valident avec `JWT_SECRET`.

## Raisons du choix

**Isolation de sécurité** :
- Un token externe compromis ne peut pas être utilisé pour accéder aux endpoints internes
- Les deux clés peuvent être révoquées indépendamment (ex : rotation de `EXCHANGE_JWT_SECRET` sans impacter les sessions internes)
- Le gateway peut router les requêtes `/api/v1/exchange/` vers exchange-service sans valider le JWT interne

**Séparation des identités** :
- Les comptes externes (`ExternalUser`) n'ont pas de tenant_id interne — ils appartiennent à une organisation externe
- Le cycle de vie est différent : invitation par email + vérification, pas de création via admin-service
- Les permissions sont structurellement différentes (accès par étude, pas par rôle système)

**Simplicité de l'exchange-service** :
- Pas de dépendance Feign vers auth-service pour chaque requête d'authentification
- L'exchange-service est autonome — peut fonctionner même si l'auth-service est temporairement indisponible
- Tests d'intégration plus simples : pas besoin de mocker l'auth-service

**Contre la réutilisation de l'auth-service** :
- Un bug dans les vérifications de `ROLE_EXTERNAL` pourrait accidentellement ouvrir l'accès interne
- Complexité accrue du modèle de rôles : mélange utilisateurs internes/externes dans la même table
- Difficultés de gestion du multi-tenant pour des utilisateurs sans appartenance hospitalière stricte

## Conséquences

- Deux variables d'environnement JWT : `JWT_SECRET` (interne) et `EXCHANGE_JWT_SECRET` (externe)
- Le gateway doit identifier les routes `/api/v1/exchange/public/` (sans auth) vs `/api/v1/exchange/` (ExchangeJwt)
- Les rapports d'audit du portail externe sont stockés dans exchange-service, séparément des audit_logs internes
- Une documentation claire doit expliquer aux développeurs pourquoi deux JWTs coexistent (ce document)

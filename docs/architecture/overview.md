# CliniTrak — Vue d'ensemble de l'architecture

*Dernière mise à jour : 2026-05-09 (Session 2)*

## Modèle architectural

CliniTrak suit une architecture **microservices multi-tenant** avec :
- Un **API Gateway** central (Spring Cloud Gateway)
- Des **services métier** indépendants (un par domaine fonctionnel)
- Un **service d'authentification** partagé (auth-service)
- Un **frontend SPA** Angular 20

```
                     ┌─────────────────────────────────────────┐
                     │            clinitrak-frontend             │
                     │         Angular 20 + PrimeNG 17          │
                     └──────────────────┬──────────────────────┘
                                        │ HTTP (Bearer JWT)
                                        │ X-Tenant-ID header
                                        ▼
                     ┌─────────────────────────────────────────┐
                     │            clinitrak-gateway              │
                     │      Spring Cloud Gateway (port 8080)    │
                     │   Rate Limiting │ Circuit Breaker │ CORS  │
                     └──────┬──────────────────────────────────┘
                            │
        ┌───────────────────┼─────────────────────┬──────────────────┐
        ▼                   ▼                      ▼                  ▼
┌───────────────┐  ┌────────────────┐  ┌─────────────────┐  ┌──────────────────┐
│  auth-service │  │ study-service  │  │ ethics-service  │  │  (autres svcs)   │
│  (port 8081)  │  │  (port 8082)   │  │  (port 8083)    │  │  8084..8091      │
│  JWT / RBAC   │  │ Études ✅      │  │  Comité Éthique │  │  CTC, Pharma...  │
└───────┬───────┘  └───────┬────────┘  └────────┬────────┘  └────────┬─────────┘
        │                  │                     │                    │
        └──────────────────┴─────────────────────┴────────────────────┘
                                       │
                         ┌─────────────┴──────────────┐
                         ▼                            ▼
              ┌──────────────────┐        ┌──────────────────┐
              │   PostgreSQL 16   │        │     Redis 7       │
              │   (port 5432)     │        │   (port 6379)     │
              │  Base par service │        │  Cache + Sessions  │
              └──────────────────┘        └──────────────────┘
                         │
                         ▼
              ┌──────────────────┐
              │      MinIO        │
              │   (port 9000)     │
              │  Documents / GED  │
              └──────────────────┘
```

## Stratégie multi-tenant

Voir [multi-tenant.md](multi-tenant.md) pour le détail complet.

**Résumé** : isolation par **Row-Level Security** (chaque entité a un `tenantId`).
Le tenant est résolu à chaque requête via :
1. Header `X-Tenant-ID` (slug du tenant, ex: `saintluc`)
2. Sous-domaine (ex: `saintluc.clinitrak.be` → slug `saintluc`)

## Sécurité

- **JWT stateless** : Access token 15min + Refresh token 7j (rotation à chaque refresh)
- **RBAC** : 11 rôles prédéfinis, permissions atomiques (`RESOURCE:ACTION`)
- **BCrypt-12** : hachage des mots de passe
- **Audit trail** : toutes les actions tracées via AOP (@AuditAspect)
- **Lockout** : 5 tentatives échouées → verrouillage 30min

## Décisions d'architecture

- [ADR-001 : Stratégie multi-tenant](../decisions/ADR-001-multi-tenant.md)

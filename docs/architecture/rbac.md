# CliniTrak — Matrice RBAC

*Dernière mise à jour : 2026-05-09*

## Rôles et permissions par module

### Légende

- ✅ Autorisé
- ❌ Non autorisé
- 👁️ Lecture seule

---

## Description des rôles

| Rôle | Code | Description | Modules principaux |
|------|------|-------------|-------------------|
| Super Administrateur | `ROLE_SUPER_ADMIN` | Accès total plateforme, toutes institutions | Tous |
| Admin Tenant | `ROLE_ADMIN_TENANT` | Administrateur de l'institution (scope tenant) | Tous (scope tenant) |
| Secrétaire CE | `ROLE_CE_SECRETARY` | Secrétariat du Comité d'Éthique | Ethics, Studies (lecture) |
| Coordinateur CE | `ROLE_CE_COORDINATOR` | Coordinateur du Comité d'Éthique | Ethics, Studies |
| Secrétariat CTC | `ROLE_CTC_DESK` | Accueil et secrétariat CTC | CTC, Studies (lecture) |
| CRA | `ROLE_CTC_CRA` | Clinical Research Associate | Studies, CTC |
| PM CTC | `ROLE_CTC_PM` | Project Manager CTC | Studies, CTC, Billing |
| COFI CTC | `ROLE_CTC_COFI` | Coordinateur financier CTC | Billing, Studies (lecture) |
| Pharmacien | `ROLE_PHARMACIST` | Pharmacien responsable des médicaments d'étude | Pharmacy, Studies (lecture) |
| Investigateur | `ROLE_INVESTIGATOR` | Investigateur principal ou co-investigateur | Studies, Ethics (soumission) |
| Externe | `ROLE_EXTERNAL` | Collaborateur externe (partenaire, auditeur) | Studies (lecture seule) |

---

## Module Études (study-service)

| Endpoint | SUPER_ADMIN | ADMIN_TENANT | CE_SECRETARY | CE_COORDINATOR | CTC_DESK | CTC_CRA | CTC_PM | CTC_COFI | PHARMACIST | INVESTIGATOR | EXTERNAL |
|----------|:-----------:|:------------:|:------------:|:--------------:|:--------:|:-------:|:------:|:--------:|:----------:|:------------:|:-------:|
| GET /studies | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 👁️ |
| POST /studies | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /studies/{id} | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 👁️ |
| PUT /studies/{id} | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| DELETE /studies/{id} | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PATCH /studies/{id}/status | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /studies/{id}/status-history | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 👁️ |
| GET /studies/{id}/contacts | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 👁️ |
| POST /studies/{id}/contacts | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| PUT /studies/{id}/contacts/{id} | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| DELETE /studies/{id}/contacts/{id} | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /studies/{id}/submissions | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ❌ |
| POST /studies/{id}/submissions | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| PUT /studies/{id}/submissions/{id} | ✅ | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET /studies/{id}/patients | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| POST /studies/{id}/patients | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| PATCH /studies/{id}/patients/{id}/status | ✅ | ✅ | ❌ | ❌ | ❌ | ✅ | ✅ | ❌ | ❌ | ✅ | ❌ |
| DELETE /studies/{id}/patients/{id} | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ | ❌ | ❌ | ❌ | ❌ |
| GET /studies/statistics | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | 👁️ |

---

## Module Auth (auth-service)

| Endpoint | SUPER_ADMIN | ADMIN_TENANT | CE_SECRETARY | CE_COORDINATOR | CTC_DESK | CTC_CRA | CTC_PM | CTC_COFI | PHARMACIST | INVESTIGATOR | EXTERNAL |
|----------|:-----------:|:------------:|:------------:|:--------------:|:--------:|:-------:|:------:|:--------:|:----------:|:------------:|:-------:|
| POST /auth/login | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /auth/logout | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /auth/refresh | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| POST /auth/register | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| GET /auth/users | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |
| PATCH /auth/users/{id}/lock | ✅ | ✅ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ | ❌ |

---

## Permissions atomiques (format RESOURCE:ACTION)

Les permissions atomiques sont gérées dans la table `permissions` de `clinitrak_auth`.
Elles permettent un contrôle fin via `@PreAuthorize("hasAuthority('STUDY:READ')")`.

| Permission | Description |
|------------|-------------|
| `STUDY:READ` | Consulter les études |
| `STUDY:CREATE` | Créer une étude |
| `STUDY:UPDATE` | Modifier une étude |
| `STUDY:DELETE` | Supprimer une étude |
| `STUDY:CHANGE_STATUS` | Changer le statut d'une étude |
| `STUDY:MANAGE_PATIENTS` | Gérer les patients d'une étude |
| `ETHICS:READ` | Consulter les dossiers CE |
| `ETHICS:CREATE` | Créer un dossier CE |
| `ETHICS:UPDATE` | Modifier un dossier CE |
| `ETHICS:REVIEW` | Évaluer un dossier CE |
| `CTC:READ` | Consulter les dossiers CTC |
| `CTC:CREATE` | Créer un dossier CTC |
| `CTC:UPDATE` | Modifier un dossier CTC |
| `PHARMACY:READ` | Consulter les données pharmacie |
| `PHARMACY:MANAGE` | Gérer les médicaments d'étude |
| `BILLING:READ` | Consulter la facturation |
| `BILLING:CREATE` | Créer des éléments de facturation |
| `BILLING:APPROVE` | Approuver des factures |
| `DOCUMENT:READ` | Consulter les documents |
| `DOCUMENT:UPLOAD` | Téléverser des documents |
| `DOCUMENT:DELETE` | Supprimer des documents |
| `ADMIN:READ` | Consulter l'administration |
| `ADMIN:MANAGE` | Gérer les utilisateurs et paramètres |

---

## Notes d'implémentation

### Contrôle d'accès dans Spring Boot

```java
// Exemple de sécurisation d'un endpoint StudyController
@PreAuthorize("hasAnyRole('ROLE_SUPER_ADMIN', 'ROLE_ADMIN_TENANT', 'ROLE_CTC_CRA', 'ROLE_CTC_PM')")
public ResponseEntity<StudyResponse> createStudy(...) { ... }

// Ou via permission atomique
@PreAuthorize("hasAuthority('STUDY:CREATE')")
public ResponseEntity<StudyResponse> createStudy(...) { ... }
```

### Isolation multi-tenant

Chaque vérification de rôle est combinée avec le `TenantContext` :
- Un `ROLE_ADMIN_TENANT` ne peut accéder qu'aux ressources de son propre tenant
- Le `ROLE_SUPER_ADMIN` peut accéder à tous les tenants (pour le support)
- L'isolation est garantie par le filtre `TenantFilter` et le `TenantContext` (ThreadLocal)

### Évolution du RBAC

Pour ajouter un nouveau rôle ou permission :
1. Ajouter l'entrée dans `V2__seed_data.sql` (auth-service)
2. Créer une migration Liquibase `V3__add_role_X.sql`
3. Mettre à jour cette matrice
4. Mettre à jour les `@PreAuthorize` des endpoints concernés

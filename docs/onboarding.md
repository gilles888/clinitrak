# Onboarding développeur — CliniTrak

*Bienvenue dans l'équipe CliniTrak !*

## Lecture obligatoire (dans l'ordre)

1. [`README.md`](../README.md) — Vue d'ensemble du projet
2. [`docs/architecture/overview.md`](architecture/overview.md) — Architecture technique
3. [`docs/deployment/local.md`](deployment/local.md) — Démarrer l'environnement local
4. [`CLAUDE.md`](../CLAUDE.md) — Comment travailler avec Claude Code sur ce projet

## Domaines et responsabilités

| Module | Contact principal |
|--------|------------------|
| auth-service | Backend team |
| study-service | Backend team |
| ethics-service | Backend team |
| clinitrak-frontend | Frontend team |
| infrastructure | DevOps team |

## Conventions de code

### Java (Spring Boot)
- Java 21 features : records, sealed classes, text blocks, pattern matching
- Lombok : `@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor` sur les entités
- **Jamais** de `@Data` sur les entités JPA (risque equals/hashCode avec Hibernate)
- DTOs = records immuables uniquement
- Tests : TestContainers pour l'intégration (vraie DB, pas de H2)

### TypeScript (Angular)
- Standalone components uniquement (pas de NgModule)
- `inject()` dans le corps de la classe, pas dans le constructeur
- Signals pour tout état réactif, Observable uniquement pour les appels HTTP
- Nouvelle syntaxe de template : `@if`, `@for`, `@switch` (pas `*ngIf`, `*ngFor`)

### Git
- Branches : `feature/<module>/<description>`, `fix/<module>/<description>`
- Commits conventionnels : `feat(auth): ...`, `fix(frontend): ...`
- PR obligatoire pour merge sur `main`
- Mettre à jour `CHANGELOG.md` dans chaque PR

## Architecture Decision Records

Les décisions d'architecture importantes sont documentées dans [`docs/decisions/`](decisions/).
Lire les ADRs avant de modifier une décision d'architecture.

## Questions fréquentes

**Q: Comment ajouter un nouveau service ?**
R: Copier le `pom.xml` d'un stub existant, créer le `Application.java`, configurer Liquibase.
Voir `study-service/` comme template (à développer).

**Q: Comment tester l'auth en local ?**
R: Utiliser Swagger UI sur http://localhost:8081/swagger-ui.html avec le header `X-Tenant-ID: saintluc`.

**Q: Comment créer un nouveau tenant ?**
R: INSERT dans la table `tenants` + redémarrage (ou flush du cache Redis si le cache tenants est chaud).

**Q: Où sont les variables d'environnement ?**
R: `.env.example` liste toutes les variables. Copier en `.env` (jamais committer `.env`).

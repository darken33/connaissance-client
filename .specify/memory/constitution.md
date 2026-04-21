# Connaissance Client Architecture Constitution

> Constitution établissant les principes d'architecture, les pratiques de développement, et la gouvernance pour le projet Connaissance Client Backend.

## Core Principles

### I. Domain-Driven Design (DDD) avec Hexagonal Architecture (NON-NEGOTIABLE)

**Chaque fonctionnalité DOIT suivre le modèle DDD + Ports/Adapters :**

- **Domain Module**: Contient la logique métier, les agrégats, les value objects, les exceptions métier, et les ports (interfaces)
- **Adapter Modules**: Chaque intégration technique (MongoDB, Kafka, services externes, API) dans son propre module adapter
- **Domain-Centric**: Le domaine ne dépend d'aucun framework; les adapters dépendent du domaine
- **Exceptions métier**: Toute violation de règle métier génère une exception métier (ex: `AdresseInvalideException`, `ClientInconnuException`), PAS une HTTP 400 générique
- **Agrégats explicites**: Chaque agrégat a une racine avec identifiant unique (UUID), ses valeurs immuables sauf où mentionné, ses règles d'invariants

**Rationale**: Permet évolution indépendante des règles métier et de l'infrastructure; facilite le test et la compréhension; force la clarté du modèle.

### II. API-First avec OpenAPI (NON-NEGOTIABLE)

**La spécification du contrat DOIT précéder l'implémentation :**

- **Single Source of Truth**: Le fichier `connaissance-client-api.yaml` (OpenAPI) est la définition unique du contrat API
- **Génération automatique**: Les DTOs, les contrôleurs stub, et la documentation proviennent de `openapi-generator-maven-plugin`
- **Jamais éditer les générés**: Les fichiers en `target/generated-sources/` ne se modifient pas; implémenter un Delegate ou adapter le YAML puis régénérer
- **Évolution contractuelle**: Les breaking changes DOIVENT être détectés et versionnés (prefix path: `/v1/`, `/v2/`, etc.)
- **Documentation JSON Schema**: Chaque DTO possède description + validation (minLength, pattern, etc.) dans le YAML

**Rationale**: Garantit contrat stable pour les clients; engage à la réflexion sur l'API avant code; réduit les bugs liés aux incohérences.

### III. Test-Driven Development (TDD) par Couches

**TDD est mandatoire à chaque niveau; les tests doivent cibler la couche :**

- **Domain Tests** (unit): Valident logique métier, invariants, exceptions métier; mockent zéro dépendance externe
- **Adapter Tests** (unit): Vérifient la transformation de/vers les modèles externes (MongoDB, Kafka), les appels API externes; mockent les ports
- **Integration Tests** (IT): Validations bout-à-bout avec vrais services (MongoDB, Kafka) lancés via Docker Compose (`env-tests/docker/docker-compose.yml`)
- **Naming**: `*Test.java` = unit, `*IT.java` = integration; exécution séparée (`maven-failsafe-plugin` pour IT)
- **Coverage**: Minimum 80% couverture au-dessus de la racine d'agrégat + adapters; rapports Jacoco (HTML, XML, JSON)
- **No Mocking the Core**: On ne mock jamais le domaine métier dans les tests; on teste le **comportement réel**

**Rationale**: La suite avant implémentation force la clarté d'interface; multi-couches = confiance sans fragilité de la suite; domaine testé en vrai = bugs métier précoces.

### IV. Null Safety et Type Safety

**DOIT utiliser JSpecify et Lombok pour éliminer la classe NullPointerException :**

- **Annotations JSpecify**: `@NonNull`, `@Nullable` décorées sur **tous** les paramètres de méthodes et champs
- **Lombok FieldDefaults**: `@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)` sauf mutation explicite (`@Setter`)
- **Static Factories**: Préférer `MyClass.of(...)` à `new MyClass(...)` pour validation en constructeur
- **Immutabilité par défaut**: Champs final; setter uniquement si invariant métier l'exige
- **Null is Not an Option**: `Optional<T>` seulement si absence métier (ex: "client not found"), jamais pour null par défaut

**Rationale**: Élimine catégories entières de bugs; IDE + compilateur aident à retrouver erreurs; réduit tests défensifs inutiles.

### V. MapStruct pour Mapping Deterministe

**Tous mappages DTO ↔ Domain DOIVENT utiliser MapStruct (pas de constructeur manuel) :**

- **Mappers autoconfigurés**: `@Mapper(componentModel = "spring")` pour intégration Spring
- **Visible et testable**: Mapping code généré mais tracé, pas de "magic" dans constructeur
- **Conversion entre métier et technologie**: Domaine utilise value objects, API utilise DTOs; mapper strict la conversion
- **Pas de conversion inline**: PAS d'`ObjectMapper.readValue()` loose; toujours un Mapper MapStruct

**Rationale**: Cohérence, performance (compile-time), traçabilité; évite les bugs silencieux de serialization.

### VI. Observabilité et Logging Structuré

**DOIT utiliser SLF4J avec Logback et Prometheus pour visibilité :**

- **SLF4J Lombok**: `@Slf4j` sur chaque service; logs au niveau approprié (DEBUG=détails, INFO=jalons, WARN=anomalie, ERROR=panne)
- **Prometheus metrics**: Utiliser `micrometer-registry-prometheus` pour exposer `/actuator/metrics` et `/actuator/prometheus` (collecteur scrape)
- **Structured properties**: Inclure contexte métier dans les logs (clientId, adresse valide/non, succès/échec) sans déballer JSON inline
- **Pas de logs en prod**: Client logs pour dev; prod logs au niveau agrégats + points d'entrée (controller, adapter)

**Rationale**: Diagnostique sans accès au code source; traçabilité d'incident; détection d'anomalies productives.

## Architecture & Technology Stack

**Mandatory Stack**:
- **Java 21** (LTS) - exactement, pas de régression
- **Spring Boot 4.0.1** - pour baseline stabile OpenAPI + Spring Cloud
- **Maven 3.8+** - pas de forks maison; utiliser mvnw fourni
- **MongoDB** (Spring Data) - document-centric, pas de SQL; schéma-less = schéma énergétiquement présenté en code
- **Kafka** - async events; partition by clientId pour ordering per-client; Avro schema (ou JSON Schema) en registry
- **OpenAPI 3.0.3** - exactement, pas Swagger 2.0

**Forbidden Frameworks**:
- Pas de Spring Data JPA (SQL) — MongoDB only
- Pas de `@Component` ou `@Service` sans dépendance injectée (tests deviennent impossibles)
- Pas de singletons statiques `getInstance()` — Spring IoC only
- Pas de Guice, Dagger, Micronaut — Spring boot unified stack only

**Optional / Ecosystem**:
- Spring Cloud (configserver, service discovery, load balancing) — compatible, non-imposé
- Resilience4j pour circuit breaker / retry sur adapters — utiliser à besoin non systématiquement
- Lombok MUST, MapStruct MUST, SLF4J MUST — boilerplate must go

## Development Workflow & Code Review

**Branch & Commit Discipline**:
- Default branch: `main` (protected; require PR)
- Feature branches: `feature/TICKET-ID-kebab-case-title`
- Commit messages: `type(scope): short message` (conventional commits)
  - `feat(domain)`: feature métier
  - `fix(api)`: bug correctif
  - `test(db-adapter)`: test coverage
  - `docs(constitution)`: documentation, constitution
  - `refactor(domain)`: restructuring sans changement fonctionnel

**Pull Request Standards**:
- MUST have ≥1 approvals avant merge
- MUST pass CI (clean build, tests, SonarQube gates)
- MUST demonstrate test coverage increase (Jacoco diff)
- Title reflects intent: `feat: add address validation via postal code service`
- Description includes: motivatioin, architectural decisions, test strategy, know issues

**Code Review Checklist (Auto)**: — reviewer check these before approval
- [ ] Domain logic: Business exception types; invariants testable?
- [ ] DDD respect: Agrégats clair? Ports/Adapters séparés?
- [ ] Tests: Couches OK (unit/integration)? Mock scope correct?
- [ ] Null safety: JSpecify annotations complètes?
- [ ] API contract: YAML modifié? Génération triggered? Evolution compatible?
- [ ] Jacoco delta: Coverage ↗️ o stable, jamais ↘️

## Quality Gates & Compliance

**Automatic Enforcement** (CI checks mandatory pass):
- **Compilation**: Java 21, warnings-as-errors
- **Unit Tests**: 100% pass; failure blocks merge
- **Integration Tests**: Maven Failsafe IT; Docker compose spins up; timeout 5 min max
- **SonarQube**: QualityGate pass; no Security Hotspots left unreviewed; Blocker/Critical < 1
- **Dependency Check**: OWASP dependency scan; known CVE < severity threshold (configuré)
- **JaCoCo Coverage**: Line coverage ≥ 80% (domain + adapters); branch ≥ 75%

**Manual Review** (before merge):
- Architecture review if new adapter type introduced
- Security review if new port/service integrates external data

## Versioning & Release

**Version Scheme**: `MAJOR.MINOR.PATCH-BUILDMETADATA` (semantic versioning)
- **MAJOR**: Breaking domain changes; aggregate structure change; port contract removed
- **MINOR**: New adapters, new ports, new features (backward compatible)
- **PATCH**: Bug fixes, documentation, refactoring (zero functional change)
- **BUILDMETADATA**: `-SNAPSHOT` local dev; `-rc.N` release candidate; released = no suffix

**Release Checklist**:
- Constitution verified aligned
- All tests green (unit + integration)
- JaCoCo passed
- CHANGELOG.md updated
- Git tag: `v2.1.0`; release notes on GitHub Releases
- Docker image pushed: `connaissance-client:v2.1.0` + `latest`

## Governance

**This Constitution is the North Star** for all decisions. Deviations require written justification and amendment proposal.

**Amendment Process**:
1. Raise concern / proposal in ADR (Architecture Decision Record) under `docs/adr/` (or GitHub issue)
2. Technical lead + architect review; consensus required
3. Constitution updated; version bumped per rules above
4. All dependent templates + documentation synchronized (see Sync Impact Report below)
5. Git commit with message: `docs: amend constitution to vX.Y.Z (reason)`

**Recurring Reviews** (per quarter):
- Verify principles vs reality (code audit sample)
- Stack version EOL: bumped or deprecated?
- Test coverage trends
- SonarQube debt evolution
- Team training on principles

**Enforcement**:
- Each PR MUST cite which principle(s) it fulfills (in PR description)
- SonarQube configured to flag: mock usage in domain tests, static initializers, null deref without check
- Linting rules in `pom.xml` (Checkstyle plugin configured) + IDE config committed

---

## Sync Impact Report

<!-- Auto-generated; do not edit manually -->

**Version**: 1.0.0 | **Action**: Initial Constitution Created | **Ratified**: 2026-04-21 | **Last Amended**: 2026-04-21

**New Version**: 1.0.0 (First Baseline Capture)

### Principles Added / Refined (vs. Prior Template):
1. **I. Domain-Driven Design + Hexagonal Architecture** (New, mandatory core)
2. **II. API-First / OpenAPI-Centric** (New, mandatory core)
3. **III. Multi-Layer TDD with Jacoco** (New, mandatory core)
4. **IV. Null Safety via JSpecify & Lombok** (New, mandatory core)
5. **V. MapStruct for DTO Mapping** (New, best practice)
6. **VI. Observability / SLF4J + Prometheus** (New, best practice)

### Sections Added:
- **Architecture & Technology Stack**: Mandatory + Forbidden frameworks listed
- **Development Workflow & Code Review**: Branch discipline, PR standards, checklist
- **Quality Gates & Compliance**: CI automation, SonarQube, Jacoco thresholds
- **Versioning & Release**: Semantic versioning + checklist
- **Governance**: Amendment process, reviews, enforcement

### Templates to Verify Alignment:
- ✅ `.specify/templates/plan-template.md` — review for "Architecture Strategy" section; align design tasks to principles
- ✅ `.specify/templates/spec-template.md` — review for "Acceptance Criteria" section; embed testing layers (unit/int/domain)
- ✅ `.specify/templates/tasks-template.md` — review for task categorization; ensure "test layers," "null safety checks," "mapper review" are task types
- ⚠ `.specify/templates/checklist-template.md` — may need custom checkpoint: "DDD compliance," "Port/Adapter separation," "API-First validated"
- ⚠ `README.md` (root) — consider adding "Architecture Principles" section referencing this constitution

### Known TODOs / Deferred:
- None—constitution baseline complete. Team validation pending before implementation sprints proceed.

---

**Version**: 1.0.0 | **Ratified**: 2026-04-21 | **Last Amended**: 2026-04-21

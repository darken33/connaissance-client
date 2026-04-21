# Connaissance Client — Feature Specifications

Bienvenue ! Ce document indexe toutes les spécifications développées pour le projet Connaissance Client, organisées pour l'atelier Devoxx.

---

## 📚 Spécifications Disponibles

### 1️⃣ [Connaissance Client API - Retrospec (Existing System)](001-connaissance-client-retrospec/spec.md)

**Type**: Reverse Engineering Specification  
**Status**: ✅ Validated  
**Purpose**: Document l'architecture existante et les fonctionnalités actuelles  
**Scope**:
- Architecture DDD + Hexagonal
- 6 endpoints REST (CRUD + address/situation management)
- MongoDB persistence
- Kafka event publishing
- IGN postal code validation avec circuit breaker

**Key Sections**:
- System context & architecture diagrams
- 6 detailed user scenarios (P1-P3)
- 7 functional requirements
- Multi-layer test strategy
- Deployment & operations guidance

**Use Case**: Comprendre l'existant avant de développer de nouvelles fonctionnalités.

**Validation Checklist**: [requirements.md](001-connaissance-client-retrospec/checklists/requirements.md) ✅

---

### 2️⃣ [Workshop Mode - In-Memory Adapters (NEW FEATURE)](002-workshop-mode-inmemory-adapters/spec.md)

**Type**: Feature Specification — Non-Functional Enhancement / DevEx  
**Status**: ✅ Validated  
**Purpose**: Permettre l'exécution de l'API sans Docker pour l'atelier Devoxx et le développement local  
**Problem Statement**:
- ❌ Environnement Docker complexe pour ateliers (bande passante, disponibilité)
- ❌ Startup lent en développement local (docker overhead)
- ✅ Solution: In-memory adapters (ConcurrentHashMap + Spring profiles)

**Scope**:
- Replace MongoDB adapter avec implémentation in-memory  
- Replace Kafka adapter avec implémentation in-memory
- Spring profile configuration (`workshop` vs `prod`)
- Thread-safe storage
- Debug actuator endpoint (optional)

**Key Sections**:
- 4 detailed user scenarios (local dev, workshop setup, state inspection, profile switching)
- 5 functional requirements (FR-1: InMemory ClientRepository, FR-2: InMemory EventService, etc.)
- Architecture decisions with trade-offs
- Complete test strategy (unit + integration)
- Deployment for workshop facilitators

**Benefits**:
- 🎓 Ateliers sans friction Docker
- 🚀 Startup < 10s (vs. Docker overhead)
- ⚡ Fast local iteration
- 🏗️ Hexagonal architecture remains unchanged (ports only; adapters swap)

**Validation Checklist**: [requirements.md](002-workshop-mode-inmemory-adapters/checklists/requirements.md) ✅

---

## 🗂️ Structure des Spécifications

Chaque spécification suit la structure :

```
specs/NNN-feature-name/
├── spec.md                    # Specification principale (documentée à 100%)
├── checklists/
│   └── requirements.md        # Checklist de validation qualité
└── (future: plan.md, tasks.md, etc.)
```

---

## 🎯 Prochaines Étapes

### Pour la Feature "Workshop Mode In-Memory Adapters"

1. ✅ **Specification créée** (spec.md) : Décrit la fonctionnalité, scénarios, FRs, tests
2. ✅ **Validated** (checklist) : Tous les critères de qualité passent ✅
3. ⏭️  **Planning Phase** (à venir) : Générer plan.md avec une stratégie de mise en œuvre
4. ⏭️  **Task Breakdown** (à venir) : Créer tasks.md avec tâches de développement
5. ⏭️  **Implementation** (à venir) : Développer les adapters in-memory

**Prêt à procéder** ? Demandez la seconde étape (`/speckit.plan`) pour générer le plan de mise en œuvre.

---

## 📋 Architecture Constitution

Rappel des principes qui guident tout développement :

**[Constitution](../.specify/memory/constitution.md)** — Version 1.0.0

### Principes NON-NEGOTIABLES ✅
1. **Domain-Driven Design + Hexagonal Architecture** — Domaine central; adapters périphériques
2. **API-First / OpenAPI** — Contrat défini d'abord
3. **Test-Driven Development** — TDD multi-couches  
4. **Null Safety & JSpecify** — Annotations obligatoires
5. **MapStruct for Mapping** — DTO ↔ Domain conversion

### Bonnes Pratiques ✅
6. **MapStruct for Mapping** — Mappers compile-time
7. **Observabilité** — SLF4J + Prometheus

---

## 📚 Comment Naviguer

### Vous êtes ici : Vue d'ensemble (INDEX)

**Pour comprendre l'existant** →
  - Lisez [Retrospec](001-connaissance-client-retrospec/spec.md)
  - Consultez les diagrammes d'architecture

**Pour développer la feature Workshop** →
  - Lisez [Workshop Feature Spec](002-workshop-mode-inmemory-adapters/spec.md)
  - Vérifiez la checklist de validation
  - Demandez la phase de planning pour les tâches

**Pour contribuer** →
  - Respectez la [Constitution](../.specify/memory/constitution.md)
  - Suivez les patterns DDD + TDD
  - Consultez les modèles de test fournis

---

## 🛠️ Ressources Utiles

### Project Structure
```
connaissance-client/
├── .specify/                 # Spec-kit configuration
│   ├── memory/
│   │   └── constitution.md  # Principes d'architecture 📋
│   ├── init-options.json
│   └── extensions.yml
├── specs/                   # Feature specifications
│   ├── 001-connaissance-client-retrospec/
│   ├── 002-workshop-mode-inmemory-adapters/
│   └── README.md (VOUS ÊTES ICI)
├── connaissance-client-*/   # Maven modules
│   ├── domain/
│   ├── api/
│   ├── db-adapter/
│   ├── event-adapter/
│   ├── cp-adapter/
│   └── app/
└── ...
```

### Maven Commands

```bash
# Build with tests
./mvnw clean install -DskipTests=false

# Run locally (will use production profile by default)
./mvnw -pl connaissance-client-app -am spring-boot:run

# Run with workshop profile (will use in-memory adapters once implemented)
./mvnw -pl connaissance-client-app -am spring-boot:run -Dspring.profiles.active=workshop

# Run specific tests
./mvnw test -Dtest=*RepositoryTest
./mvnw failsafe:integration-test
```

---

## 👥 Atelier Devoxx

### Déroulement Prévu

1. **Intro** (5 min) : Présenter spec-kit et DDD
2. **Architecture Overview** (10 min) : Montrer Retrospec + diagrammes
3. **Nouvelle Fonctionnalité** (30 min) : Développer feature "Workshop Mode"
   - Lire spec → Créer plan → Générer tasks
   - Implémentation guidée (adapters in-memory)
4. **Q&A** (10 min)

### Materials

- ✅ Retrospec completed (comprendre l'app)
- ✅ Workshop Feature Spec (nouvelle feature à développer)
- 📄 Constitution (principes à respecter)
- 🖥️ IDE setup: Java 21, Maven, Spring Boot

---

## ✍️ Notes & Questions

**Owner**: Assistant (Copilot, Devoxx)  
**Last Updated**: 2026-04-21  
**Status**: 2/2 Specs Validated ✅ → Ready for Planning Phase

---

**Next Action**: Demandez `/speckit.plan` pour démarrer la phase de planning de la feature "Workshop Mode In-Memory Adapters".


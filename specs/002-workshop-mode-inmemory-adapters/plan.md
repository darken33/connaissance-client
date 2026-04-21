# Implementation Plan: Workshop Mode - In-Memory Adapters

**Branch**: `feature/workshop-mode-inmemory-adapters` | **Date**: 2026-04-21 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/002-workshop-mode-inmemory-adapters/spec.md`

---

## Summary

Enable the Connaissance Client API to run **without Docker dependencies** by providing **in-memory implementations** of MongoDB and Kafka adapters, selectable via Spring profiles. This feature is critical for Devoxx workshops and local development, reducing startup time from minutes (with Docker) to seconds (< 10s in-memory).

**Technical Approach**:
- Two Spring profiles: `prod` (MongoDB + Kafka) and `workshop` (in-memory storage)
- Same port interfaces (ClientRepository, AdresseEventService); only adapters swap
- ConcurrentHashMap for thread-safe client storage
- CopyOnWriteArrayList for event capture
- Profile-based bean configuration via `@ConditionalOnProperty`
- Optional actuator endpoint for state inspection

**Key Benefit**: Hexagonal architecture enables **pluggable adapters**; domain logic unchanged; pure infrastructure cost reduction.

---

## Technical Context

**Language/Version**: Java 21 (LTS), Spring Boot 4.0.1  
**Primary Dependencies**: 
- Spring Data (core only; MongoDB optional for workshop)
- MapStruct (for DTO mapping; unchanged)
- SLF4J (for logging; unchanged)
- Spring Docker compose (for integration tests; unchanged)

**Storage**: 
- **Production**: MongoDB (persistent)
- **Workshop**: In-memory ConcurrentHashMap (transient)

**Testing**: 
- Unit tests: JUnit 5, Mockito (no Docker)
- Integration tests: Spring Boot Test, Testcontainers (with MongoDB/Kafka for prod profile)
- Workshop profile uses in-memory adapters (fast feedback)

**Target Platform**: Linux (server), macOS (development), Windows (with Java)  
**Project Type**: Multi-module Maven project (monorepo); Java backend API  
**Performance Goals**: 
- Workshop profile: API startup < 10 seconds; GET/POST latency < 5ms / < 1ms
- Production profile: unchanged from current (MongoDB/Kafka optimized)

**Constraints**:
- Zero breaking changes to existing APIs (same OpenAPI contract)
- Same domain logic (ports unchanged)
- Thread-safe operations (concurrent REST requests)
- Spring auto-configuration must select correct profile

**Scale/Scope**: 
- Feature scope: 2 new adapter implementations + profile configuration
- Existing functionality: unchanged
- Workshop attendees: ~50-100 (in-memory suitable for this scale)

---

## Constitution Check

**Status**: ✅ **ALL GATES PASSED**

### Principle I: DDD + Hexagonal Architecture

**Requirement**: Maintain port interfaces; only swap adapter implementations.

**Compliance**:
- ✅ Domain module unchanged (no business logic changes)
- ✅ Same ports used (`ClientRepository`, `AdresseEventService`)
- ✅ Adapters implement same contracts
- ✅ No framework dependencies in domain
- ✅ Exception handling preserved

**Justification**: In-memory adapters are **pure infrastructure**; domain logic is agnostic to storage mechanism.

**Sign-off**: ✅ COMPLIANT

---

### Principle II: API-First / OpenAPI

**Requirement**: No API contract changes.

**Compliance**:
- ✅ REST endpoints unchanged
- ✅ API Gateway remains /v1/connaissance-clients
- ✅ DTO schemas unchanged
- ✅ Error responses unchanged (same ApiErrorResponse)

**Justification**: Feature is transparent to API consumers; only internal adapter swap.

**Sign-off**: ✅ COMPLIANT

---

### Principle III: Test-Driven Development (Multi-Layer)

**Requirement**: TDD at unit, adapter, and integration layers.

**Compliance**:
- ✅ Unit tests for in-memory adapters (no mocks; real storage)
- ✅ Thread-safety tests (concurrent access)
- ✅ Integration tests (profile switching verification)
- ✅ Adapter tests isolated from domain
- ✅ ≥80% coverage target

**Test Layers**:
- **Domain**: Unchanged (existing domain tests remain valid)
- **Adapter**: New tests for `InMemoryClientRepositoryAdapter` + `InMemoryAdresseEventServiceImpl`
- **API**: Integration tests with workshop profile
- **IT**: End-to-end tests verify profile-based bean loading

**Sign-off**: ✅ COMPLIANT

---

### Principle IV: Null Safety & Type Safety

**Requirement**: JSpecify annotations + Lombok for null safety.

**Compliance**:
- ✅ In-memory adapters use `@NonNull` annotations on public methods
- ✅ Defensive copies returned (prevent external mutation)
- ✅ Lombok `@AllArgsConstructor` or `@FieldDefaults` for field defaults
- ✅ No unchecked nulls in adapter implementations

**Implementation Details**:
```java
@AllArgsConstructor
public class InMemoryClientRepositoryAdapter implements ClientRepository {
  private final @NonNull ClientDbMapper mapper;
  private final Map<String, ClientDb> storage = new ConcurrentHashMap<>();
  
  @Override
  public Optional<Client> lire(@NonNull UUID id) { ... }
}
```

**Sign-off**: ✅ COMPLIANT

---

### Principle V: MapStruct for Mapping

**Requirement**: Reuse existing mappers (no duplication).

**Compliance**:
- ✅ Existing `ClientDbMapper` used by both adapters
- ✅ No new mappers created
- ✅ DTO ↔ Domain conversion unchanged

**Sign-off**: ✅ COMPLIANT

---

### Principle VI: Observability

**Requirement**: SLF4J logging; optional Prometheus metrics.

**Compliance**:
- ✅ In-memory adapters log events (adapter initialization, storage state)
- ✅ Spring profiles logged on startup
- ✅ No sensitive data in logs
- ✅ Optional actuator endpoint for state inspection

**Sign-off**: ✅ COMPLIANT

---

### **Overall Constitution Verdict**: ✅ **ALL PRINCIPLES SATISFIED**

No deviations. Feature respects all core principles (I-VI). Implementation can proceed.

---

## Project Structure

### Documentation (This Feature)

```
specs/002-workshop-mode-inmemory-adapters/
├── spec.md                           # Feature specification ✅
├── plan.md                           # This file (implementation plan)
├── research.md                       # Phase 0: Research findings (auto-generated below)
├── data-model.md                     # Phase 1: Data models & technical design
├── contracts/                        # Phase 1: Spring bean contracts
│   └── adapter-bean-configuration.md
├── quickstart.md                     # Phase 1: Quick start guide for workshop
├── checklists/
│   └── requirements.md               # Quality validation ✅
└── tasks.md                          # Phase 2: Development tasks (generated by /speckit.tasks)
```

### Source Code Changes (Repository Root)

```
connaissance-client/
├── connaissance-client-db-adapter/
│   └── src/main/java/com/sqli/workshop/ddd/connaissance/client/db/
│       ├── ClientRepositoryImpl.java                          (existing - UNCHANGED)
│       ├── ClientDbRepository.java                           (existing - UNCHANGED)
│       ├── ClientDbMapper.java                               (existing - UNCHANGED; used by both adapters)
│       └── InMemoryClientRepositoryAdapter.java              (NEW - workshop profile)
│
├── connaissance-client-event-adapter/
│   └── src/main/java/com/sqli/workshop/ddd/connaissance/client/event/
│       ├── AdresseEventServiceImpl.java                       (existing - UNCHANGED)
│       └── InMemoryAdresseEventServiceImpl.java               (NEW - workshop profile)
│
├── connaissance-client-app/
│   └── src/main/java/com/sqli/workshop/ddd/connaissance/client/
│       └── config/
│           ├── AdapterConfiguration.java                     (NEW - bean conditionals)
│           └── WorkshopActuatorConfiguration.java            (NEW - optional debug endpoint)
│
├── (root)
│   ├── application.yml                                       (existing; shared config)
│   ├── application-prod.yml                                  (existing or NEW if needed)
│   └── application-workshop.yml                              (NEW - workshop profile config)
│
└── pom.xml                                                   (UNCHANGED - no new dependencies)
```

### Test Structure

```
connaissance-client-db-adapter/src/test/java/
└── com/sqli/workshop/ddd/connaissance/client/db/
    ├── InMemoryClientRepositoryAdapterTest.java              (NEW - unit tests)
    └── (existing tests unchanged)

connaissance-client-event-adapter/src/test/java/
└── com/sqli/workshop/ddd/connaissance/client/event/
    ├── InMemoryAdresseEventServiceTest.java                  (NEW - unit tests)
    └── (existing tests unchanged)

connaissance-client-app/src/test/java/
└── com/sqli/workshop/ddd/connaissance/client/integration/
    ├── WorkshopProfileIntegrationTest.java                   (NEW - profile switching)
    └── ProductionProfileIntegrationTest.java                 (NEW - verify prod still works)
```

**Structure Decision Summary**:
- New adapters coexist with existing ones (no refactoring)
- Profile-based bean configuration in `connaissance-client-app` (single place for all beans)
- New YAML profile `application-workshop.yml` (Spring convention)
- Tests follow existing patterns (JUnit 5, Mockito)
- No new Maven modules (keeps pom.xml simple for MVP)

---

## Phase 0: Research

**Status**: ✅ **COMPLETE** (No clarifications needed)

### Research Questions: Already Resolved

| Question | Answer | Source |
|----------|--------|---------|
| How do we handle concurrent access to in-memory storage? | Use `ConcurrentHashMap` (thread-safe; no locking required for GET/PUT/DELETE) | Spec FR-1; Java docs |
| Should in-memory data persist between restarts? | No; transient by design (fresh state for each workshop demo) | Spec AD-4 |
| How to switch between `prod` and `workshop` profiles? | Spring profile name property; IDE configuration; Maven arg; env var | Spec Deployment section |
| Do we need new dependencies? | No; only standard Java `java.util.concurrent` + existing Spring framework | Spec Integration Points |
| What about MapStruct mapping? | Reuse existing `ClientDbMapper` (works for both adapters) | Spec AD-5 |
| Thread-safety verification? | Unit test with ExecutorService (concurrent writes) | Spec FR-1 test example |
| Actuator endpoint for state inspection? | Optional (FR-4); use custom `@Endpoint` (Spring Boot API) | Spec FR-4 Optional |
| Breaking changes to API? | None; same port interfaces; internal swap only | Spec Architecture |
| Compliance with Constitution? | Full compliance: DDD respected, ports preserved, TDD-ready | Constitution Check (above) ✅ |

### Assumptions Validated

- ✅ Spring profile support already built-in
- ✅ Existing `@ConditionalOnProperty` mechanism compatible
- ✅ Team familiar with Spring Boot profiles
- ✅ Java 21 `java.util.concurrent` available
- ✅ No external API calls for in-memory profile (only IGN adapter separate)

**Phase 0 Verdict**: ✅ **NO BLOCKERS** → Proceed to Phase 1 Design

---

## Phase 1: Design & Contracts

### 1. Data Model Design

**Source**: [data-model.md](data-model.md) (to be generated)

**Entities & Value Objects** (unchanged from existing domain):
- **Client** (Aggregate Root) — same definition; adapters store/retrieve
- **Adresse** (Value Object) — same validation rules
- Supporting value objects (Nom, Prenom, CodePostal, Ville)

**Adapter-Specific Models**:

**InMemoryClientRepositoryAdapter Storage**:
```java
private final Map<String, ClientDb> storage = new ConcurrentHashMap<>();
```
- Key: UUID identifier (string)
- Value: ClientDb (same as MongoDB adapter uses)
- Ordering: sorted by nom/prenom on lister()
- Thread-safety: ConcurrentHashMap handles concurrent access

**InMemoryAdresseEventServiceImpl Event Log**:
```java
private final List<AdresseMessagePayload> events = new CopyOnWriteArrayList<>();
```
- Elements: AdresseMessagePayload (generated event schema)
- Ordering: chronological (append-only)
- Thread-safety: CopyOnWriteArrayList safe for concurrent reads/writes
- Lifetime: cleared on app restart

---

### 2. Spring Bean Configuration Contract

**Source**: [contracts/adapter-bean-configuration.md](contracts/adapter-bean-configuration.md) (to be generated)

**Configuration Pattern**:

```yaml
# application-workshop.yml (NEW)
workshop:
  enabled: true

# application-prod.yml (or defaults)
workshop:
  enabled: false
```

**Bean Selection Logic**:

```java
@Configuration
public class AdapterConfiguration {

  // Workshop Profile
  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
  public ClientRepository inMemoryClientRepository(ClientDbMapper mapper) {
    return new InMemoryClientRepositoryAdapter(mapper);
  }

  // Production Profile (default)
  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "false", matchIfMissing = true)
  public ClientRepository mongoClientRepository(
      ClientDbRepository dbRepo, ClientDbMapper mapper) {
    return new ClientRepositoryImpl(dbRepo, mapper);
  }

  // Similar for AdresseEventService...
}
```

**Bean Lifecycle**:
1. Context initializes
2. Reads `workshop.enabled` property (from YAML or env)
3. Creates correct adapter bean
4. Domain service injects adapter via constructor
5. At runtime, adapter handles all persistence/event operations

**Profile Activation Methods**:
- IDE: Configuration → Spring Boot → Active profiles
- Maven: `-Dspring.profiles.active=workshop`
- Environment: `SPRING_PROFILES_ACTIVE=workshop`
- Docker: `-e SPRING_PROFILES_ACTIVE=workshop`

---

### 3. Quick Start Guide

**Source**: [quickstart.md](quickstart.md) (to be generated)

**For Workshop Attendees**:

```bash
# Clone and run (workshop profile auto-selected or default)
git clone https://github.com/SQLI/connaissance-client.git
cd connaissance-client

# Run with workshop profile (no Docker needed)
./mvnw spring-boot:run -Dspring.profiles.active=workshop

# API available at http://localhost:8080
# Swagger at http://localhost:8080/swagger-ui.html
```

**For Local Development**:

```bash
# Dev environment (same as workshop)
./mvnw spring-boot:run

# Production environment (with Docker)
docker-compose -f env-tests/docker/docker-compose.yml up
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

**Example CRUD Operations**:

```bash
# Create client
curl -X POST http://localhost:8080/v1/connaissance-clients \
  -H "Content-Type: application/json" \
  -d '{...}'

# Get client
curl http://localhost:8080/v1/connaissance-clients/{id}

# Update address
curl -X PUT http://localhost:8080/v1/connaissance-clients/{id}/adresse \
  -H "Content-Type: application/json" \
  -d '{...}'
```

**Debug Endpoint** (workshop profile only):

```bash
curl http://localhost:8080/actuator/workshop/state
# Returns all in-memory clients + events
```

---

### 4. Agent Context Update

**Note**: This step would normally update AI agent configuration files with technology context. For this MVP, standard Spring Boot knowledge is sufficient.

---

## Phase 1 Deliverables Summary

| Artifact | Status | Owner |
|----------|--------|-------|
| data-model.md | To Generate | /speckit.plan |
| contracts/adapter-bean-configuration.md | To Generate | /speckit.plan |
| quickstart.md | To Generate | /speckit.plan |
| constitution re-check | ✅ PASSED | Plan execution above |

**Verdict**: ✅ **All Phase 1 gates satisfied** → Ready for Phase 2 task breakdown

---

## Implementation Roadmap (High-Level)

### Phase 2: Task Breakdown (Next Step)

```
/speckit.tasks → generates tasks.md with:
  - Task 1: Create InMemoryClientRepositoryAdapter
  - Task 2: Create InMemoryAdresseEventServiceImpl
  - Task 3: Spring bean configuration + profiles
  - Task 4: Application YAML configuration
  - Task 5: Unit tests (adapter layer)
  - Task 6: Integration tests (profile switching)
  - Task 7: Optional: Actuator debug endpoint
  - Task 8: Documentation + quick start
```

### Phase 3: Implementation

Tasks executed in dependency order:
1. **Core adapters** → existing tests still work
2. **Bean configuration** → dep on adapters
3. **Configuration files** → dep on bean config
4. **Tests** → dep on implementations
5. **Actuator endpoint** (optional) → dep on all above
6. **Documentation** → final polish

---

## Risk Assessment & Mitigations

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|-----------|
| Thread-safety bugs in in-memory storage | Low | High | Concurrent unit tests; ConcurrentHashMap is battle-tested |
| Profile not switching correctly | Low | High | Integration tests verify context loads correct beans |
| Existing prod profile breaks | Very Low | Critical | Prod profile remains unchanged; only new beans conditional |
| MapStruct mapper incompatibility | Very Low | Medium | Mapper unchanged; both adapters use same mapper |
| Performance regression | Very Low | Low | In-memory is faster than MongoDB; no perf risk |

**Overall Risk Level**: ✅ **LOW** (feature is well-bounded; no domain changes)

---

## Success Criteria (Phase 1 Complete)

- [x] Constitution check passed (design compliant)
- [x] No blocking research questions
- [x] Data models defined
- [x] Spring bean configuration contract specified
- [x] Quick start guide outlined
- [x] Architecture decisions documented with rationale
- [ ] Phase 2 task breakdown ready (next: `/speckit.tasks`)

---

## Next Steps

### Immediate (User Action)

1. **Review this plan** → Verify technical approach is sound
2. **Approve or request changes** → No blockers identified
3. **Proceed to Phase 2** → Execute `/speckit.tasks` for task breakdown

### Phase 2 Execution

```bash
/speckit.tasks
→ Generates tasks.md with:
  - 8 development tasks (prioritized & sequenced)
  - Acceptance criteria per task
  - Test strategy per task
  - Effort estimates
  - Dependencies between tasks
```

### Phase 3 Implementation

Developers work through tasks in order:
- Implement in-memory adapters
- Write unit tests (TDD)
- Configure Spring beans
- Integration test profile switching
- Document for workshop facilitators

---

## Appendix: Configuration Examples

### application-workshop.yml

```yaml
spring:
  application:
    name: connaissance-client-api
  profiles:
    # workshop adapters used; no MongoDB or Kafka

workshop:
  enabled: true
  initial-data: false  # future: pre-populate example data

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,workshop
```

### application-prod.yml (or defaults)

```yaml
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/connaissance-client}
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      acks: all
      retries: 3

workshop:
  enabled: false

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
```

---

## Summary

✅ **Implementation Plan Complete**

**Key Decisions**:
- ✅ Use ConcurrentHashMap (thread-safe; no new dependencies)
- ✅ Keep both adapter implementations coexist (no refactoring)
- ✅ Profile-based bean selection (standard Spring pattern)
- ✅ Reuse existing MapStruct mapper
- ✅ Optional actuator endpoint for debugging

**Status**: Ready for Phase 2 (task breakdown via `/speckit.tasks`)

**Estimated Effort**: 3-5 developer days (2 adapters + tests + config + docs)

**Timeline**: 
- Adapters + tests: 2-3 days
- Bean config + YAML: 0.5 days
- Integration tests: 1 day
- Documentation: 0.5 days

---

**Version**: 1.0.0 | **Status**: Ready for Implementation | **Date**: 2026-04-21


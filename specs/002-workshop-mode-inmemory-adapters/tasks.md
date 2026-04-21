# Implementation Tasks: Workshop Mode - In-Memory Adapters

**Feature**: Support Mode Workshop avec Adapters In-Memory (No Docker Required)  
**From**: [plan.md](plan.md) & [spec.md](spec.md)  
**Branch**: `002-workshop-mode-inmemory-adapters`  
**Date**: 2026-04-21  
**Status**: Generated (v1.0.0)  

---

## Overview

This document breaks down the [implementation plan](plan.md) into actionable, sequenced development tasks. Each task is mapped to a user story, includes acceptance criteria, test strategy, and dependencies.

**Total Tasks**: 20  
**Phases**: 7 (Setup → Polish)  
**Total Est. Effort**: 3-5 developer days  
**Parallel Opportunities**: 3 (unit tests can run in parallel)  

---

## Task Execution Strategy

**Sequential Phases**:
1. ✅ **Phase 1 (Setup)**: Project & agent context verification
2. → **Phase 2 (Foundational)**: Spring configuration (blocking prerequisite)
3. → **Phase 3 (US1)**: In-memory adapters + unit tests
4. → **Phase 4 (US2)**: Verify workshop setup works
5. → **Phase 5 (US3)**: Optional debug endpoint
6. → **Phase 6 (US4)**: Profile switching verification
7. → **Phase 7 (Polish)**: Final docs & validation

**Parallel Opportunities**:
- {T007, T009, T010} can run in parallel after Phase 2 (independent test files)
- {T016, T017} can run in parallel with {T014, T015}

**TDD Approach**: Test tasks (T007, T009, T010, T015, T016, T017, T019) are UNIT tests that should be written BEFORE implementation in many cases.

---

## Phase 1: Setup & Verification

### User Story Goal
Verify project structure is ready for Workshop Mode feature development; validate Maven build environment.

### Independent Test Criteria
- ✅ Maven builds successfully (`mvn clean verify`)
- ✅ Feature branch `002-workshop-mode-inmemory-adapters` is active
- ✅ All existing tests pass (baseline)

---

- [ ] **T001** Verify project structure and baseline test suite
  - **Description**: In `connaissance-client` root, run `mvn clean verify` to confirm all existing modules build and tests pass
  - **File Path**: N/A (verification task; no code changes)
  - **Acceptance Criteria**:
    - ✅ All modules build without errors: `connaissance-client-domain`, `-db-adapter`, `-event-adapter`, `-api`, `-app`
    - ✅ All existing unit tests pass (baseline ≥80% coverage)
    - ✅ GIT branch confirmed: `git branch` shows `* 002-workshop-mode-inmemory-adapters`
  - **Test Strategy**: Run Maven build; check exit code = 0; verify test output shows "BUILD SUCCESS"
  - **Effort**: 15 min

- [ ] **T002** Update agent context with Workshop Mode technologies
  - **Description**: Register Spring profiles, in-memory adapters, and conditional beans in agent context (already done; verify)
  - **File Path**: `.github/copilot-instructions.md`
  - **Acceptance Criteria**:
    - ✅ File contains: "Java 21 (LTS), Spring Boot 4.0.1"
    - ✅ File contains: "Workshop Mode" mention or "in-memory adapters"
    - ✅ Copilot can recognize Workshop Mode technologies on next invocation
  - **Test Strategy**: Read file; grep for keywords; verify no syntax errors
  - **Effort**: 10 min

---

## Phase 2: Foundational Infrastructure (Blocking Prerequisites)

### User Story Goal
Set up Spring bean configuration and profile YAML files so in-memory vs. production adapters can be selected at runtime.

### Independent Test Criteria
- ✅ Spring context loads with `workshop.enabled=true`
- ✅ Spring context loads with `workshop.enabled=false` (prod defaults)
- ✅ No configuration errors in logs

---

- [ ] **T003** Create Spring bean configuration class (AdapterConfiguration)
  - **Description**: In `connaissance-client-app/src/main/java/.../config/`, create `AdapterConfiguration.java` with `@ConditionalOnProperty` beans for ClientRepository and AdresseEventService
  - **File Path**: `connaissance-client-app/src/main/java/com/sqli/workshop/ddd/connaissance/client/config/AdapterConfiguration.java`
  - **Acceptance Criteria**:
    - ✅ Class marked `@Configuration`
    - ✅ Method: `inMemoryClientRepository(ClientDbMapper mapper)` with `@Bean` + `@ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")`
    - ✅ Method: `mongoClientRepository(…)` with `@ConditionalOnProperty(…havingValue="false", matchIfMissing=true)`
    - ✅ Similar methods for AdresseEventService (in-memory vs Kafka)
    - ✅ Constructor injection for all dependencies (no autowiring on fields)
    - ✅ Compiles without errors
  - **Test Strategy**: Compile; grep class for `@Configuration`, `@Bean`, `@ConditionalOnProperty` annotations; verify method names
  - **Implementation Notes**: 
    - Refer to [contracts/adapter-bean-configuration.md](contracts/adapter-bean-configuration.md) for exact bean signatures
    - Non-null annotations on all constructor parameters
  - **Effort**: 1 hour

- [ ] **T004** Create application-workshop.yml profile configuration
  - **Description**: In `connaissance-client-app/src/main/resources/`, create `application-workshop.yml` with `workshop.enabled: true` and management endpoints
  - **File Path**: `connaissance-client-app/src/main/resources/application-workshop.yml`
  - **Acceptance Criteria**:
    - ✅ Property `workshop.enabled: true` set
    - ✅ Management endpoints include: `health,info,metrics,prometheus,workshop`
    - ✅ Spring MongoDB auto-index-creation: false
    - ✅ Spring Kafka bootstrap-servers set to placeholder (not used by in-memory adapter)
    - ✅ Logging level: `com.sqli.workshop.ddd: INFO`
    - ✅ Valid YAML syntax (Spring can parse without errors)
  - **Test Strategy**: Parse YAML syntax; start Spring context with `spring.profiles.active=workshop`; verify `workshop.enabled` property available
  - **Implementation Notes**: 
    - See [contracts/adapter-bean-configuration.md - application-workshop.yml](contracts/adapter-bean-configuration.md#application-workshop-yml-new) for exact content
  - **Effort**: 30 min

- [ ] **T005** [P] Create application-prod.yml profile configuration
  - **Description**: In `connaissance-client-app/src/main/resources/`, create or update `application-prod.yml` with `workshop.enabled: false` and production MongoDB/Kafka config
  - **File Path**: `connaissance-client-app/src/main/resources/application-prod.yml`
  - **Acceptance Criteria**:
    - ✅ Property `workshop.enabled: false` set (or omitted; defaults to false via matchIfMissing=true)
    - ✅ Spring MongoDB URI configured (from env var or manual connection string)
    - ✅ Spring Kafka bootstrap-servers configured
    - ✅ Management endpoints include: `health,info,metrics,prometheus`
    - ✅ Valid YAML syntax
  - **Test Strategy**: Parse YAML; start Spring context with `spring.profiles.active=prod`; verify production properties loaded
  - **Implementation Notes**: 
    - This may already exist; verify or update
    - See [contracts/adapter-bean-configuration.md - application-prod.yml](contracts/adapter-bean-configuration.md#application-prod-yml-new-or-use-defaults) for reference
  - **Effort**: 30 min

---

## Phase 3: Core User Story 1 (P1) - Local Development without Docker

### User Story Goal
Enable developers to run the API locally without Docker dependencies; all CRUD operations use in-memory storage.

### Independent Test Criteria
- ✅ InMemoryClientRepositoryAdapter creates, reads, updates, deletes clients
- ✅ Thread-safe concurrent access (tested via ExecutorService)
- ✅ InMemoryAdresseEventServiceImpl captures events in-memory
- ✅ API integration test confirms workshop profile beans loaded

---

- [ ] **T006** Implement InMemoryClientRepositoryAdapter
  - **Description**: In `connaissance-client-db-adapter/src/main/java/.../`, create `InMemoryClientRepositoryAdapter.java` implementing `ClientRepository` port with `ConcurrentHashMap<String, ClientDb>` storage
  - **File Path**: `connaissance-client-db-adapter/src/main/java/com/sqli/workshop/ddd/connaissance/client/db/adapter/InMemoryClientRepositoryAdapter.java`
  - **Acceptance Criteria**:
    - ✅ Class implements `ClientRepository` interface (same contract as `ClientRepositoryImpl`)
    - ✅ Storage: private `ConcurrentHashMap<String, ClientDb> storage = new ConcurrentHashMap<>()`
    - ✅ Constructor: receives `ClientDbMapper mapper` (non-null annotation)
    - ✅ Method `lister()`: returns all clients sorted by nom/prenom
    - ✅ Method `lire(UUID id)`: returns `Optional<Client>` (uses mapper to convert ClientDb → Client)
    - ✅ Method `enregistrer(Client client)`: inserts/updates; returns persisted Client
    - ✅ Method `supprimer(UUID id)`: removes from map
    - ✅ All methods decorated with `@NonNull` on parameters
    - ✅ Defensive copies of domain objects (no external mutation)
    - ✅ Compiles without errors
  - **Test Strategy**: Compile; verify class structure via reflection; confirm all port methods implemented
  - **Implementation Notes**:
    - Refer to [data-model.md - InMemoryClientRepositoryAdapter Storage](data-model.md#inmemoryclientrepositatoryadapter-storage)
    - Reuse existing `ClientDbMapper` for DTO conversions
    - Sorting: implement Comparable or use Stream.sorted()
  - **Effort**: 2 hours

- [ ] **T007** [P] Implement unit tests for InMemoryClientRepositoryAdapter
  - **Description**: In `connaissance-client-db-adapter/src/test/java/.../`, create `InMemoryClientRepositoryAdapterTest.java` with comprehensive unit tests
  - **File Path**: `connaissance-client-db-adapter/src/test/java/com/sqli/workshop/ddd/connaissance/client/db/adapter/InMemoryClientRepositoryAdapterTest.java`
  - **Acceptance Criteria**:
    - ✅ Test: `testListEmpty()` — empty collection returns empty list
    - ✅ Test: `testEnregistrerAndRead()` — create client, retrieve by ID, verify data intact
    - ✅ Test: `testUpdateClient()` — modify client, verify changes persisted
    - ✅ Test: `testSupprimerRemovesClient()` — delete client, verify not found
    - ✅ Test: `testListerSortedByNomPrenom()` — verify list returns sorted results
    - ✅ Test: `testThreadSafety_ConcurrentWrites()` — 50+ concurrent PUT/DELETE operations; all succeed without exceptions
    - ✅ Test: `testOptionalNotFound()` — read non-existent ID returns empty Optional
    - ✅ All tests use JUnit 5; no Spring context needed (unit tests; fast)
    - ✅ Coverage ≥80% for this adapter
  - **Test Strategy**: Run with `mvn test -Dtest=InMemoryClientRepositoryAdapterTest`; verify all tests pass
  - **Implementation Notes**:
    - Use `ExecutorService` + `CountDownLatch` for thread-safety test
    - Use `@InjectMocks` and `@Mock` for mapper (or real mapper if preferred)
    - No database or Spring context required
  - **Effort**: 2 hours

- [ ] **T008** Implement InMemoryAdresseEventServiceImpl
  - **Description**: In `connaissance-client-event-adapter/src/main/java/.../`, create `InMemoryAdresseEventServiceImpl.java` implementing `AdresseEventService` port with `CopyOnWriteArrayList<AdresseMessagePayload>` event log
  - **File Path**: `connaissance-client-event-adapter/src/main/java/com/sqli/workshop/ddd/connaissance/client/event/adapter/InMemoryAdresseEventServiceImpl.java`
  - **Acceptance Criteria**:
    - ✅ Class implements `AdresseEventService` interface
    - ✅ Storage: private `CopyOnWriteArrayList<AdresseMessagePayload> events = new CopyOnWriteArrayList<>()`
    - ✅ Method `sendEvent(UUID clientId, Destinataire dest, Adresse addr)`: append event; return true
    - ✅ Constructor: no external dependencies required (pure in-memory)
    - ✅ Events stored with timestamp added
    - ✅ Method to retrieve events (for actuator endpoint later): `List<AdresseMessagePayload> getEvents()`
    - ✅ All public methods have proper null-safety annotations
    - ✅ Compiles without errors
  - **Test Strategy**: Compile; verify class structure; check event append logic
  - **Implementation Notes**:
    - See [data-model.md - InMemoryAdresseEventServiceImpl design](data-model.md#inmemoryaddresseeventserviceimpl-design)
    - Events are append-only (no deletion)
    - Timestamp should be captured on sendEvent() call
  - **Effort**: 1.5 hours

- [ ] **T009** [P] Implement unit tests for InMemoryAdresseEventServiceImpl
  - **Description**: In `connaissance-client-event-adapter/src/test/java/.../`, create `InMemoryAdresseEventServiceTest.java`
  - **File Path**: `connaissance-client-event-adapter/src/test/java/com/sqli/workshop/ddd/connaissance/client/event/adapter/InMemoryAdresseEventServiceTest.java`
  - **Acceptance Criteria**:
    - ✅ Test: `testSendEventIsAppended()` — send event, verify in list
    - ✅ Test: `testMultipleEventsOrdered()` — send 5 events, verify chronological order
    - ✅ Test: `testEventPayloadAccurate()` — verify event contains correct client ID, destinataire, address data
    - ✅ Test: `testSendEventReturnsTrue()` — confirm return value is true
    - ✅ Test: `testThreadSafety_ConcurrentSends()` — 50+ concurrent sendEvent() calls; all events captured
    - ✅ All tests use JUnit 5; no Spring context
    - ✅ Coverage ≥80%
  - **Test Strategy**: Run with `mvn test -Dtest=InMemoryAdresseEventServiceTest`
  - **Implementation Notes**:
    - No Kafka producer; no async publishing
    - All events immediately appended
  - **Effort**: 1.5 hours

- [ ] **T010** [P] Create WorkshopProfileIntegrationTest (verify in-memory adapters loaded)
  - **Description**: In `connaissance-client-app/src/test/java/.../`, create `WorkshopProfileIntegrationTest.java` to verify Spring context loads correct adapters
  - **File Path**: `connaissance-client-app/src/test/java/com/sqli/workshop/ddd/connaissance/client/integration/WorkshopProfileIntegrationTest.java`
  - **Acceptance Criteria**:
    - ✅ Spring context loads with `properties = "workshop.enabled=true"`
    - ✅ Autowire `ClientRepository` bean → verify it's instance of `InMemoryClientRepositoryAdapter`
    - ✅ Autowire `AdresseEventService` bean → verify it's instance of `InMemoryAdresseEventServiceImpl`
    - ✅ Test: `testWorkshopBeanContextLoads()` — context initializes without errors
    - ✅ Test: `testInMemoryRepositoryBeanLoaded()` — correct adapter wired
    - ✅ Test: `testInMemoryEventServiceBeanLoaded()` — correct event service wired
    - ✅ Use `@SpringBootTest` annotation
  - **Test Strategy**: Run with `mvn test -Dtest=WorkshopProfileIntegrationTest`; verify beans are correct types
  - **Implementation Notes**:
    - Use `assertThat(bean).isInstanceOf(ExpectedClass.class)`
    - DO NOT start full application server (no webEnvironment needed for this test)
  - **Effort**: 1 hour

---

## Phase 4: Core User Story 2 (P1) - Devoxx Workshop Setup

### User Story Goal
Verify workshop attendees can start the API and perform CRUD operations without infrastructure knowledge.

### Independent Test Criteria
- ✅ API starts with `mvn spring-boot:run -Dspring.profiles.active=workshop` in <15 seconds
- ✅ All REST endpoints respond to requests
- ✅ Example CURL commands work as documented

---

- [ ] **T011** Verify API endpoints work with workshop profile (end-to-end)
  - **Description**: Start app with workshop profile; verify all REST endpoints functional (GET /v1/clients, POST /v1/clients, PUT, DELETE, PATCH /adresse)
  - **File Path**: N/A (manual integration test; document results in test report)
  - **Acceptance Criteria**:
    - ✅ `mvn spring-boot:run -Dspring.profiles.active=workshop` completes in <15 seconds
    - ✅ Health check: `curl http://localhost:8080/actuator/health` returns 200 + UP status
    - ✅ GET /v1/connaissance-clients returns 200 + empty list initially
    - ✅ POST /v1/connaissance-clients with valid client data returns 201 + created ID
    - ✅ GET /v1/connaissance-clients/{id} returns 200 + client data
    - ✅ PUT /v1/connaissance-clients/{id} with updated data returns 200 + updated client
    - ✅ PATCH /v1/connaissance-clients/{id}/adresse returns 200 + address updated
    - ✅ DELETE /v1/connaissance-clients/{id} returns 204 (no content)
    - ✅ Swagger UI available at `/swagger-ui.html`
  - **Test Strategy**: Manual testing with CURL commands; verify HTTP status codes; check response bodies
  - **Implementation Notes**:
    - See [quickstart.md](quickstart.md) for CURL command examples
    - Document startup time and any warnings
  - **Effort**: 1.5 hours (includes troubleshooting)

- [ ] **T012** Create example CURL scripts for workshop attendees
  - **Description**: Create shell script with ready-to-use CURL commands for workshop demo; save in `scripts/workshop-demo.sh`
  - **File Path**: `scripts/workshop-demo.sh`
  - **Acceptance Criteria**:
    - ✅ Script includes: create client, list clients, get single client, update client, update address, delete client
    - ✅ Each command is well-commented with expected output
    - ✅ Script is executable: `chmod +x scripts/workshop-demo.sh`
    - ✅ Script can be run: `./scripts/workshop-demo.sh` and all operations succeed
    - ✅ Output is readable (formatted JSON responses)
  - **Test Strategy**: Run script; verify exit code = 0; check all operations succeed
  - **Implementation Notes**:
    - Refer to [quickstart.md - Full Workflow Example](quickstart.md#full-workflow-complete-example)
    - Use `jq` for JSON formatting (or plain curl output)
  - **Effort**: 1 hour

- [ ] **T013** Test full CRUD workflow end-to-end
  - **Description**: Create integration test verifying complete client lifecycle (create → read → update → delete)
  - **File Path**: `connaissance-client-app/src/test/java/com/sqli/workshop/ddd/connaissance/client/integration/WorkshopCrudWorkflowTest.java`
  - **Acceptance Criteria**:
    - ✅ Test: `testFullClientLifecycle_CreateReadUpdateDelete()` — create client, retrieve, modify, delete; verify each step succeeds
    - ✅ Test uses `@SpringBootTest` with workshop profile
    - ✅ Uses `TestRestTemplate` or `MockMvc` to invoke REST endpoints
    - ✅ Verifies HTTP status codes and response bodies
    - ✅ Test passes consistently
  - **Test Strategy**: Run with `mvn test -Dtest=WorkshopCrudWorkflowTest`
  - **Implementation Notes**:
    - Start Spring Boot web context for this test
    - Use `webEnvironment = WebEnvironment.RANDOM_PORT`
  - **Effort**: 1.5 hours

---

## Phase 5: Core User Story 3 (P2) - In-Memory Storage Inspection

### User Story Goal
Provide debug endpoint for inspecting in-memory state (clients, events) during workshop/development.

### Independent Test Criteria
- ✅ Actuator endpoint `/actuator/workshop/state` available in workshop profile
- ✅ Returns JSON with all clients and events
- ✅ Endpoint not available in production profile

---

- [ ] **T014** Implement WorkshopStateEndpoint (custom actuator endpoint)
  - **Description**: In `connaissance-client-app/src/main/java/.../config/`, create `WorkshopStateEndpoint.java` exposing workshop state via actuator
  - **File Path**: `connaissance-client-app/src/main/java/com/sqli/workshop/ddd/connaissance/client/config/WorkshopStateEndpoint.java`
  - **Acceptance Criteria**:
    - ✅ Class marked `@Component` + `@Endpoint(id = "workshop")`
    - ✅ Method `getState()` marked `@ReadOperation`
    - ✅ Returns `WorkshopState` POJO with fields: profile, timestamp, totalClients, totalEvents, clients[], events[]
    - ✅ Only active when `workshop.enabled=true` (use `@ConditionalOnProperty`)
    - ✅ Safely casts `AdresseEventService` to `InMemoryAdresseEventServiceImpl` (null check for safety)
    - ✅ Response serializes to JSON correctly
  - **Test Strategy**: Start app with workshop profile; call `curl http://localhost:8080/actuator/workshop/state`; verify JSON response
  - **Implementation Notes**:
    - Create companion `WorkshopState.java` DTO class
    - See [contracts/adapter-bean-configuration.md - WorkshopActuatorConfiguration](contracts/adapter-bean-configuration.md#optional-workshopactuatorconfiguration)
  - **Effort**: 1.5 hours

- [ ] **T015** [P] Create integration test for actuator endpoint
  - **Description**: In test suite, create `WorkshopActuatorTest.java` verifying endpoint returns correct data
  - **File Path**: `connaissance-client-app/src/test/java/com/sqli/workshop/ddd/connaissance/client/integration/WorkshopActuatorTest.java`
  - **Acceptance Criteria**:
    - ✅ Test: `testWorkshopStateEndpointIsAccessible()` — GET /actuator/workshop/state returns 200
    - ✅ Test: `testWorkshopStateReturnsCorrectStructure()` — response has profile, timestamp, clients[], events[] fields
    - ✅ Test: `testWorkshopStateReflectsStoredClients()` — create client via API, call endpoint, verify client in response
    - ✅ Test: `testEndpointNotAvailableInProdProfile()` — with prod profile, endpoint returns 404
    - ✅ Use `@SpringBootTest` with webEnvironment
  - **Test Strategy**: Run with `mvn test -Dtest=WorkshopActuatorTest`
  - **Implementation Notes**:
    - Use `TestRestTemplate` to invoke endpoint
  - **Effort**: 1.5 hours

---

## Phase 6: Core User Story 4 (P2) - Switch Between Profiles

### User Story Goal
Verify developers can seamlessly switch between workshop (in-memory) and production (MongoDB + Kafka) profiles without code changes.

### Independent Test Criteria
- ✅ Production profile loads MongoDB adapter (existing behavior unchanged)
- ✅ Switching profiles requires only app restart (no code recompilation)
- ✅ All tests pass with both profiles

---

- [ ] **T016** [P] Create ProductionProfileIntegrationTest (verify prod profile unchanged)
  - **Description**: Create `ProductionProfileIntegrationTest.java` ensuring production adapters still load correctly
  - **File Path**: `connaissance-client-app/src/test/java/com/sqli/workshop/ddd/connaissance/client/integration/ProductionProfileIntegrationTest.java`
  - **Acceptance Criteria**:
    - ✅ Spring context loads with `properties = "workshop.enabled=false"` or default (no workshop property)
    - ✅ Autowire `ClientRepository` bean → verify it's instance of `ClientRepositoryImpl` (not in-memory)
    - ✅ Autowire `AdresseEventService` bean → verify it's instance of `AdresseEventServiceImpl` (not in-memory)
    - ✅ Test: `testProdBeanContextLoads()` — context initializes without errors
    - ✅ Test: `testMongoRepositoryBeanLoaded()` — verify production adapter
    - ✅ Test: `testKafkaEventServiceBeanLoaded()` — verify Kafka adapter
    - ✅ Use `@SpringBootTest` annotation
  - **Test Strategy**: Run with `mvn test -Dtest=ProductionProfileIntegrationTest`
  - **Implementation Notes**:
    - This test requires MongoDB/Kafka services running (or Testcontainers setup)
    - May need to be skipped in CI if no services available (use `@EnabledIfEnvironmentVariable` or similar)
  - **Effort**: 1 hour

- [ ] **T017** [P] Create profile switching verification test
  - **Description**: Document and test the process of stopping app and restarting with different profile; verify no code changes needed
  - **File Path**: N/A (manual documentation test; document procedure in test report)
  - **Acceptance Criteria**:
    - ✅ Step 1: `mvn spring-boot:run -Dspring.profiles.active=workshop` → API starts with in-memory adapters
    - ✅ Step 2: CRUD operations work; data persists during session
    - ✅ Step 3: Ctrl+C to stop app
    - ✅ Step 4: `mvn spring-boot:run -Dspring.profiles.active=prod` → API starts with MongoDB adapters
    - ✅ Step 5: Same CRUD endpoints work; data persists in MongoDB
    - ✅ No Java code recompilation between profiles (only Spring profile change)
  - **Test Strategy**: Manual testing; document steps and results
  - **Implementation Notes**:
    - This is a workflow verification; not an automated test
  - **Effort**: 1 hour

---

## Phase 7: Polish & Final Validation

### User Story Goal
Complete documentation, verify all tests pass, and prepare for workshop delivery.

### Independent Test Criteria
- ✅ All tasks complete; all tests pass
- ✅ Test coverage ≥80% for new adapters
- ✅ Quickstart guide ready for workshop facilitators
- ✅ No warnings in Maven build

---

- [ ] **T018** Create comprehensive quickstart guide
  - **Description**: Finalize [quickstart.md](quickstart.md) with all necessary information for workshop attendees; ensure it's accurate and complete
  - **File Path**: `specs/002-workshop-mode-inmemory-adapters/quickstart.md`
  - **Acceptance Criteria**:
    - ✅ 60-second startup section with exact commands
    - ✅ Basic API workflow (CRUD examples with exact CURL commands)
    - ✅ Debug endpoint documentation
    - ✅ Troubleshooting section with solutions to common issues
    - ✅ Complete end-to-end workflow script
    - ✅ IDE configuration instructions (IntelliJ + VS Code)
    - ✅ All code examples tested and verified to work
    - ✅ Document is readable and well-formatted
  - **Test Strategy**: Have another team member follow quickstart guide; verify all steps work
  - **Implementation Notes**:
    - Treat as documentation artifact (not code)
    - File already created in Phase 1; this task refines and verifies completeness
  - **Effort**: 1 hour

- [ ] **T019** Verify all tests pass and coverage ≥80%
  - **Description**: In project root, run `mvn clean verify` and `mvn jacoco:report` to confirm all tests pass and coverage threshold met
  - **File Path**: N/A (validation task)
  - **Acceptance Criteria**:
    - ✅ Command `mvn clean verify` exits with code 0 (all tests pass)
    - ✅ All 7 new test files execute (InMemoryClientRepositoryAdapterTest, etc.)
    - ✅ JaCoCo coverage report generated: `target/site/jacoco/index.html`
    - ✅ Coverage for new adapter classes (InMemoryClientRepositoryAdapter, InMemoryAdresseEventServiceImpl) ≥80%
    - ✅ No test failures or warnings
    - ✅ Build summary shows "BUILD SUCCESS"
  - **Test Strategy**: Run Maven commands; parse output; check report
  - **Implementation Notes**:
    - May need to configure JaCoCo plugin in pom.xml if not present
    - Check `target/site/jacoco/` for HTML coverage report
  - **Effort**: 30 min

- [ ] **T020** Final documentation review and release checklist
  - **Description**: Review all feature documentation (spec.md, plan.md, contracts/, quickstart.md, tasks.md); verify completeness and consistency; prepare branch for merge
  - **File Path**: All docs in `specs/002-workshop-mode-inmemory-adapters/`
  - **Acceptance Criteria**:
    - ✅ spec.md up-to-date and consistent with implementation
    - ✅ plan.md reflects actual design decisions made
    - ✅ contracts/adapter-bean-configuration.md documents actual bean configuration
    - ✅ data-model.md matches actual storage implementation
    - ✅ quickstart.md tested and verified
    - ✅ tasks.md complete with all tasks marked as [x] completed
    - ✅ No broken internal links
    - ✅ All code examples in docs match actual codebase
    - ✅ GIT branch is ready for PR/merge (no uncommitted changes)
  - **Test Strategy**: Review files; test all links; verify docs vs code
  - **Implementation Notes**:
    - This is the final gate before merge
    - Create merge request to main branch
  - **Effort**: 1 hour

---

## Dependency Graph

```
T001 (Verify project) ──┐
T002 (Agent context)    │ (parallelizable)
                        ↓
      ┌─────────────────────────────────────┐
      │  Phase 2: Foundational Blocking     │
      └────┬────────────────────────────┬───┘
           │                            │
      T003 (AdapterConfiguration)  T004 (application-workshop.yml)
           │                            │
           └─────────────┬──────────────┘
                         │
                         ↓
      ┌─────────────────────────────────────┐       ┌──────────────┐
      │  Phase 3: US1 (Core Adapters)       │ (P)   │  Phase 5: P2  │
      ├─────────────┬──────────────────┬────┤       │  (Optional)   │
      │             │                  │    │       └──────────────┘
   T006         T008                T010  T005        T014, T015
 (Repository) (EventService)  (Integration)  (Prod)   (Actuator)
                                                          ↓
   T007, T009  ──────────────────────────────────────────┴─────
 (Unit Tests)                                           │
   (parallel)  ─────────────────────────────→  T016, T017 (Phase 6)
                                                (Profile Switching)
                ┌─T011──T012──T013───────────────────────────┐
                │  Phase 4: US2 (Workshop Setup)             │
                └────────────────────────────┬────────────────┘
                                             │
                                             ↓
                      ┌────────────────────────────────────┐
                      │ Phase 7: Polish & Documentation    │
                      ├─────────┬────────────┬─────────────┤
                      │         │            │             │
                     T018     T019         T020          (End)
                (Quickstart) (Testing)   (Review)
```

---

## Parallel Execution Strategy

### Scenario 1: Maximum Parallelism (After T003, T004)

```bash
# Terminal 1: T006 + T008 (Adapter implementations)
# Terminal 2: T007 + T009 (Unit tests - wait for T006/T008)
# Terminal 3: T010 (Integration test - waits for both)
# Terminal 4: T011 + T012 + T013 (Manual testing)
# Terminal 5: T014 + T015 (Optional actuator)
# Terminal 6: T016 + T017 (Profile switching)
```

**Timeline with Parallelism**:
- Phase 1: 30 min (T001, T002 parallel)
- Phase 2: 1.5 hours (T003, T004, T005 sequential; config is prerequisite)
- Phase 3: 4 hours (T006, T008 parallel 2h; T007, T009 parallel 2h after; T010 1h after)
- Phase 4: 2 hours (T011, T012, T013 can share time)
- Phase 5: 2 hours (T014, T015 parallel)
- Phase 6: 1 hour (T016, T017 parallel)
- Phase 7: 1.5 hours (T018, T019 parallel; T020 last)

**Total with Parallelism**: ~11 hours (1.5 days)
**Total Sequential**: ~15 hours (2 days)

---

## Success Metrics

| Metric | Target | Validation |
|--------|--------|-----------|
| All tasks completed | 20/20 | All checkboxes checked |
| Test coverage | ≥80% | JaCoCo report in target/site/jacoco/ |
| Build success | 0 errors, 0 warnings | `mvn clean verify` exits 0 |
| Manual verification | All workflows pass | CURL tests + IDE testing documented |
| Documentation complete | 100% | All .md files reviewed; no broken links |
| Performance (workshop) | <15s startup | Measured on standard laptop (no SSD bias) |
| No breaking changes | 100% | Prod profile tests pass; existing tests pass |

---

## Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|------------|-----------|
| ConcurrentHashMap race conditions | Low | Concurrent unit test (T007) + ExecutorService |
| Profile not switching | Low | Integration tests (T010, T016, T017) verify bean loading |
| Bean autowiring fails | Low | Test bean presence in T010 + T016 |
| Breaking existing tests | Very Low | Run baseline `mvn verify` in T001 |
| Documentation out of sync | Low | T020 final review checks docs vs code |

---

## File Summary

**New Files Created During Tasks**:

| Task | File | Type |
|------|------|------|
| T003 | AdapterConfiguration.java | Java source |
| T004 | application-workshop.yml | YAML config |
| T005 | application-prod.yml | YAML config |
| T006 | InMemoryClientRepositoryAdapter.java | Java source |
| T007 | InMemoryClientRepositoryAdapterTest.java | Test |
| T008 | InMemoryAdresseEventServiceImpl.java | Java source |
| T009 | InMemoryAdresseEventServiceTest.java | Test |
| T010 | WorkshopProfileIntegrationTest.java | Test |
| T011 | (manual testing; no file) | N/A |
| T012 | scripts/workshop-demo.sh | Shell script |
| T013 | WorkshopCrudWorkflowTest.java | Test |
| T014 | WorkshopStateEndpoint.java | Java source |
| T014 | WorkshopState.java | Java source (DTO) |
| T015 | WorkshopActuatorTest.java | Test |
| T016 | ProductionProfileIntegrationTest.java | Test |
| T017 | (manual testing; no file) | N/A |
| T018 | (review/refine existing) | Markdown |
| T019 | (validation; no new file) | N/A |
| T020 | (review; no new file) | N/A |

**Total New Files**: ~14 Java/Test classes + 2 YAML configs + 1 shell script

---

## Implementation Notes

### Before Starting

1. Ensure you're on branch `002-workshop-mode-inmemory-adapters` (confirmed in T001)
2. All existing tests pass (baseline in T001)
3. Review [specification](spec.md) and [plan.md](plan.md) before starting

### During Implementation

- **Test-Driven Development**: For each implementation task, write tests FIRST (failing tests), then implement to make them pass
- **Code Review**: Have team member review each task before moving to next phase
- **Continuous Build**: Run `mvn clean compile` frequently to catch errors early
- **Documentation**: Update this tasks.md as you complete each task (check [x] boxes)

### After Completion

1. Merge branch to main: `git merge --no-ff 002-workshop-mode-inmemory-adapters`
2. Tag release: `git tag -a v1.0.0-workshop-mode -m "Workshop Mode with In-Memory Adapters"`
3. Notify team; update deployment runbook

---

## Version History

| Version | Date | Status | Changes |
|---------|------|--------|---------|
| 1.0.0 | 2026-04-21 | Generated | Initial task breakdown from plan.md & spec.md |

---

**Generated by**: `/speckit.tasks` agent  
**Status**: Ready for Implementation  
**Next Step**: Start with Phase 1 (T001, T002); then Phase 2 as blocking prerequisite


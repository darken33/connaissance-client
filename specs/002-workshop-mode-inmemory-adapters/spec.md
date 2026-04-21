# Feature Specification: Support Mode Workshop avec Adapters In-Memory

**Feature Name**: Workshop-Ready Architecture (No Docker Required)  
**Feature Branch**: `feature/workshop-mode-inmemory-adapters`  
**Created**: 2026-04-21  
**Status**: Draft  
**Type**: Non-Functional Enhancement / DevEx Improvement  
**Target Audience**: Devoxx Workshop, Local Development, CI/CD Fast Feedback  

---

## Executive Summary

Enable the Connaissance Client API to run **without Docker dependencies** by providing **in-memory implementations** of the MongoDB and Kafka adapters. This is critical for:

- 🎓 **Devoxx Workshop** : Attendees run locally without Docker (bandwidth, system constraints)
- 🚀 **Local Development** : Fast iteration without infrastructure overhead
- ⚡ **CI/CD** : Quick builds and tests; Docker still available for integration tests

**Architecture Benefit**: Hexagonal architecture enables **pluggable adapters** — same port interfaces, different implementations based on Spring profile (`prod` vs `workshop`).

---

## User Scenarios & Testing

### Scenario 1: Local Development without Docker (P1)

**User Journey**: Developer wants to run the API locally for feature development without managing Docker containers.

**Flow**:
1. Clone repository
2. Run: `./mvnw spring-boot:run`
3. API starts immediately (no MongoDB, no Kafka services needed)
4. All CRUD operations work with in-memory storage
5. Address validation still works (via IGN adapter or mock)
6. Events are captured in-memory (viewable via actuator endpoint)

**Success Criteria**:
- ✅ API starts within 10 seconds (no Docker spin-up)
- ✅ All endpoints functional (GET, POST, PUT, DELETE)
- ✅ Data persists during session (cleared on restart)
- ✅ Events logged/viewable (not published to Kafka)
- ✅ No MongoDB or Kafka required

**Independent Test**: Start app with default profile → CRUD operations work → no infrastructure errors

---

### Scenario 2: Devoxx Workshop Atelier Setup (P1)

**User Journey**: Workshop attendees (non-experts) need API running without infrastructure knowledge.

**Prerequisites**:
- Java 21 installed (project already specifies this)
- No Docker required
- Network: No external API calls (use mock for IGN)

**Flow**:
1. Attendees clone repository
2. Open IDE (VS Code, IntelliJ)
3. Run: `./mvnw spring-boot:run -Dspring.profiles.active=workshop`
4. API is live on `http://localhost:8080`
5. Swagger UI available at `/swagger-ui.html`
6. Attendees focus on spec-kit workflow, not infrastructure

**Success Criteria**:
- ✅ Zero Docker commands required
- ✅ App starts in < 15 seconds on standard laptop
- ✅ All CRUD endpoints available
- ✅ Example data can be populated (POST /connaissance-clients)
- ✅ Attendees can demonstrate feature implementation without infrastructure issues

**Independent Test**: Fresh checkout → `mvn spring-boot:run` → API responds to requests

---

### Scenario 3: In-Memory Storage Inspection (P2)

**User Journey**: During development/debugging, developer wants to inspect the in-memory state (all clients, events published).

**Flow**:
1. After creating/updating clients via REST
2. Access debug endpoint (e.g., `GET /actuator/workshop/state` or similar)
3. View:
   - All stored clients (serialized as JSON)
   - All published events (address change history)
4. Use for manual testing and demo purposes

**Success Criteria**:
- ✅ Debug endpoint available (workshop profile only)
- ✅ Shows current in-memory state
- ✅ Helpful for demo/validation

**Independent Test**: Create client → GET debug endpoint → verify stored data

---

### Scenario 4: Switch Between Profiles at Runtime (P2)

**User Journey**: Developer can toggle between `workshop` (in-memory) and `prod` (MongoDB + Kafka) profiles without code changes.

**Flow**:
1. Start with `workshop` profile: `mvn spring-boot:run -Dspring.profiles.active=workshop`
2. All adapters use in-memory implementations
3. Stop app, restart with `prod` profile: `mvn spring-boot:run -Dspring.profiles.active=prod`
4. Same code; different adapters loaded (MongoDB + Kafka)

**Success Criteria**:
- ✅ Profile-based configuration via `application-workshop.yml` and `application-prod.yml`
- ✅ IoC container auto-wires correct implementations (@ConditionalOnProperty or profile-based @Bean)
- ✅ No code changes needed (only config)

**Independent Test**: Run with workshop profile → verify in-memory; run with prod profile → verify MongoDB

---

## Functional Requirements

### FR-1: In-Memory ClientRepository Implementation

**Requirement**: Create an in-memory implementation of `ClientRepository` port that stores clients in a thread-safe Map.

**Details**:
- **Class Name**: `InMemoryClientRepositoryAdapter` (or similar)
- **Storage**: `ConcurrentHashMap<String, ClientDb>` (Thread-safe for concurrent requests)
- **Methods Implemented**:
  - `lister()` — return all clients (sorted by name/prenom)
  - `lire(UUID)` — return Optional<Client> (mock MapStruct conversion)
  - `enregistrer(Client)` — insert/update; return persisted Client
  - `supprimer(UUID)` — remove from map
- **Key Differences from MongoDB**:
  - No query language (simple key-based lookup)
  - No indices (O(n) for list operations, acceptable for demos)
  - Data is lost on app restart (expected for workshop)
  - No transactions (in-memory is atomic per operation)
- **Lifecycle**: Data cleared on app startup (fresh state for each demo)

**Testing**:
- Unit test: `InMemoryClientRepositoryAdapterTest`
  - testListEmpty()
  - testEnregistrerAndRead()
  - testSupprimerRemovesClient()
  - testThreadSafety() [concurrent calls]

---

### FR-2: In-Memory AdresseEventService Implementation

**Requirement**: Create an in-memory implementation of `AdresseEventService` that captures published events in a list (for inspection/testing).

**Details**:
- **Class Name**: `InMemoryAdresseEventServiceImpl` (or similar)
- **Storage**: `List<AdresseMessagePayload>` (in-memory event log)
- **Methods Implemented**:
  - `sendEvent(UUID, Destinataire, Adresse)` — append event to list; return true
- **Behavior**:
  - Events are NOT published to Kafka
  - Events are captured in-memory with timestamp for inspection
  - Debug endpoint exposes event history (optional; Scenario 3)
- **Thread Safety**: `CopyOnWriteArrayList<>` for concurrent access

**Testing**:
- Unit test: `InMemoryAdresseEventServiceTest`
  - testSendEventIsAppended()
  - testMultipleEventsOrdered()
  - testEventPayloadAccurate()

---

### FR-3: Spring Profile Configuration

**Requirement**: Use Spring profiles to conditionally load in-memory vs. production adapters.

**Details**:

**Configuration Files**:

1. **application.yml** (default/shared):
```yaml
spring:
  application:
    name: connaissance-client-api
  jpa:
    hibernate:
      ddl-auto: none
```

2. **application-workshop.yml** (new; workshop profile):
```yaml
# In-memory adapters; no MongoDB, no Kafka
spring:
  data:
    mongodb:
      auto-index-creation: false
  kafka:
    bootstrap-servers: localhost:9092  # not used; adapter mocked

# Features
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus,workshop  # expo debug endpoint
        
# Custom property to enable workshop mode
workshop:
  enabled: true
  initial-data: true  # optionally pre-populate with example clients
```

3. **application-prod.yml** (existing; production profile):
```yaml
# MongoDB + Kafka (as currently configured)
spring:
  data:
    mongodb:
      uri: ${MONGODB_URI}
  kafka:
    bootstrap-servers: ${KAFKA_BROKERS}
```

**Bean Configuration**:

Use Spring `@ConditionalOnProperty` or `@Profile` to activate implementations:

```java
// In config class
@Configuration
public class AdapterConfiguration {

  // Workshop profile
  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
  public ClientRepository inMemoryClientRepository() {
    return new InMemoryClientRepositoryAdapter();
  }

  // Production profile
  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "false", matchIfMissing = true)
  public ClientRepository mongoClientRepository(ClientDbRepository dbRepo, ClientDbMapper mapper) {
    return new ClientRepositoryImpl(dbRepo, mapper);
  }

  // Similar for AdresseEventService
  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
  public AdresseEventService inMemoryAdresseEventService() {
    return new InMemoryAdresseEventServiceImpl();
  }

  @Bean
  @ConditionalOnProperty(name = "workshop.enabled", havingValue = "false", matchIfMissing = true)
  public AdresseEventService kafkaAdresseEventService(IDefaultServiceEventsProducer producer) {
    return new AdresseEventServiceImpl(producer);
  }
}
```

**Activation**:
- Default (no profile): loads production adapters (MongoDB + Kafka)
- `mvn spring-boot:run -Dspring.profiles.active=workshop`: loads in-memory adapters
- Environment variable: `SPRING_PROFILES_ACTIVE=workshop`
- IDE: Set active profile in IDE run configuration

---

### FR-4 (Optional): Actuator Endpoint for Workshop State Inspection

**Requirement**: Expose an actuator endpoint (workshop mode only) to view current in-memory state.

**Details**:
- **Endpoint**: `GET /actuator/workshop/state` (custom actuator endpoint)
- **Access**: Workshop profile only; returns 404 in production
- **Response Schema**:
```json
{
  "profile": "workshop",
  "timestamp": "2026-04-21T14:30:00Z",
  "clients": [
    {
      "id": "uuid1",
      "nom": "Dupont",
      "prenom": "Jean",
      ...
    }
  ],
  "events": [
    {
      "timestamp": "2026-04-21T14:29:00Z",
      "clientId": "uuid1",
      "destinataire": "Dupont Jean",
      "adresse": { ... }
    }
  ]
}
```

**Implementation**:
- Custom `@Endpoint` (Spring Boot Actuator)
- Queries in-memory storage (InMemoryClientRepositoryAdapter, InMemoryAdresseEventServiceImpl)
- Serializes state to JSON

**Testing**: Integration test verifies endpoint availability and response format.

---

### FR-5: Maven Module Structure (Optional Refactoring)

**Requirement**: Optionally organize in-memory adapters into separate modules for clarity.

**Details**:

Option A: **Keep in existing modules** (simpler; recommended for MVP)
- Add `InMemoryClientRepositoryAdapter` to `connaissance-client-db-adapter` module
- Add `InMemoryAdresseEventServiceImpl` to `connaissance-client-event-adapter` module
- Profile-based configuration selects implementation

Option B: **Separate modules** (cleaner; future refactor)
- Create `connaissance-client-db-adapter-inmemory` module
- Create `connaissance-client-event-adapter-inmemory` module
- Depends on domain ports only
- `connaissance-client-app` includes both modules; config selects one

**Recommendation for MVP**: Option A (minimal changes to existing structure).

---

## Key Entities & Data Models

### Entity: InMemoryClientRepositoryAdapter

**Responsibility**: Provide in-memory storage for Client aggregates (replaces MongoDB).

**Structure**:
```java
@Component
public class InMemoryClientRepositoryAdapter implements ClientRepository {
  
  private final Map<String, ClientDb> storage = new ConcurrentHashMap<>();
  private final ClientDbMapper mapper;  // MapStruct mapper to convert to/from domain
  
  @Override
  public List<Client> lister() {
    // Return all clients, sorted by nom/prenom
  }
  
  @Override
  public Optional<Client> lire(UUID id) {
    // Key-based lookup
  }
  
  @Override
  public Client enregistrer(Client client) {
    // Insert or update; generate UUID if needed
  }
  
  @Override
  public void supprimer(UUID id) {
    // Remove from map
  }
}
```

**Invariants**:
- Thread-safe (ConcurrentHashMap)
- Keys are UUID strings
- Data is transient (lost on restart)
- Sorting: clients ordered by nom, then prenom (for consistent demo output)

---

### Entity: InMemoryAdresseEventServiceImpl

**Responsibility**: Capture address change events in-memory (replaces Kafka).

**Structure**:
```java
@Component
public class InMemoryAdresseEventServiceImpl implements AdresseEventService {
  
  private final List<AdresseMessagePayload> events = new CopyOnWriteArrayList<>();
  
  @Override
  public boolean sendEvent(UUID id, Destinataire destinataire, Adresse adresse) {
    AdresseMessagePayload payload = ... // build from params
    events.add(payload);  // capture in-memory
    return true;  // always succeeds
  }
  
  public List<AdresseMessagePayload> getEvents() {
    return new ArrayList<>(events);  // defensive copy
  }
  
  public void clearEvents() {
    events.clear();
  }
}
```

**Invariants**:
- Thread-safe (CopyOnWriteArrayList)
- Events are appended with order preserved
- Timestamp captured at send time
- No outside dependencies (no Kafka needed)

---

## Success Criteria & Measurable Outcomes

### Functional Success Metrics

1. **Local Development Experience**
   - ✅ App starts in < 10 seconds (without Docker overhead)
   - ✅ All CRUD operations work (GET, POST, PUT, DELETE)
   - ✅ No database connection errors
   - ✅ No Kafka connectivity errors
   - ✅ Data persists during session

2. **Profile Switching**
   - ✅ Application context loads correctly with `workshop` profile
   - ✅ Correct adapters injected (in-memory vs. MongoDB/Kafka)
   - ✅ No exceptions on bean instantiation
   - ✅ Same domain service works with both profiles

3. **Workshop Execution**
   - ✅ Attendees can start API without infrastructure knowledge
   - ✅ Swagger/OpenAPI docs available immediately
   - ✅ Example data can be created and retrieved
   - ✅ Demonstration scenarios run without delays

4. **State Inspection (Optional)**
   - ✅ Actuator endpoint available in workshop mode
   - ✅ Exposes accurate in-memory state (clients + events)
   - ✅ Useful for manual testing and validation

---

### Non-Functional Success Metrics

1. **Performance**
   - ✅ GET /connaissance-clients (100 clients): < 5ms (compared to MongoDB > 50ms)
   - ✅ POST (create client): < 1ms (compared to MongoDB + Validation > 100ms)
   - ✅ All operations have consistent low latency

2. **Compatibility**
   - ✅ Same port interfaces used by both implementations (zero changes to domain)
   - ✅ Same MapStruct mappers work for both (DTO ↔ Domain)
   - ✅ Same API contracts (OpenAPI spec unchanged)

3. **Maintainability**
   - ✅ In-memory adapters follow same patterns as MongoDB/Kafka adapters
   - ✅ Clear documentation on profile-based configuration
   - ✅ No magic; straightforward Spring bean lifecycle

4. **Testing**
   - ✅ Unit tests for in-memory adapters (no Docker needed)
   - ✅ Integration tests can use workshop profile (fast feedback)
   - ✅ Production tests still run with MongoDB/Kafka (testcontainers)

---

## Architecture Decisions & Rationale

### AD-1: Keep Port Interfaces Unchanged

**Decision**: Use existing `ClientRepository` and `AdresseEventService` ports; only swap implementations.

**Rationale**:
- **Hexagonal Architecture**: Ports define the boundary; adapters implement the contract
- **Minimal Changes**: No domain logic modified; pure adapter swap
- **Type Safety**: Interface segregation forces implementations to conform
- **Testing**: Domain tests don't care which adapter is used

**Alternative Rejected**: Create new ports (e.g., `InMemoryRepository`) → violates DIP; creates adapter proliferation.

---

### AD-2: ConcurrentHashMap for Thread-Safe Storage

**Decision**: Use `ConcurrentHashMap<UUID, ClientDb>` for in-memory storage (not simple HashMap).

**Rationale**:
- **Concurrency**: REST API receives multiple concurrent requests; map must be thread-safe
- **No Locks**: ConcurrentHashMap uses segment locks (more efficient than synchronized)
- **Performance**: Reads and writes don't block each other (unlike synchronized)
- **Familiar**: Standard Java util; no external dependencies

**Trade-off**: Slight memory overhead vs. safety guarantee.

---

### AD-3: Profile-Based Bean Configuration

**Decision**: Use Spring `@ConditionalOnProperty` (or `@Profile`) to activate implementations.

**Rationale**:
- **Declarative**: Configuration is clear and testable (no runtime conditionals)
- **Externalized**: Profile can be set via environment, system properties, or application.yml
- **Standard**: Aligns with Spring best practices for multi-environment deployments
- **Testable**: Can write tests for each profile independently

**Alternative Rejected**: Runtime if-statements in service constructors (tight coupling, hard to test, violates SRP).

---

### AD-4: Transient Data (No Persistence Between Restarts)

**Decision**: In-memory storage is cleared on app startup (fresh state for each demo).

**Rationale**:
- **Workshop Use**: Each demo starts fresh; no stale data from previous run
- **Clarity**: Attendees understand that in-memory is ephemeral (different from DB)
- **Simplicity**: No marshaling/unmarshaling to disk (not needed for workshops)

**Future Enhancement**: Optional file-based initialization (load example data from JSON on startup).

---

## Testing Strategy

### Test Layers

```
        /\
       /  \
      / E2E \      (Optional; verify profile switching works)
     /______ \
     
      /    \
     / API /       (Workshop profile integration tests; verify endpoints work)
    /______ \
    
     /      \
    / Unit /        (InMemoryClientRepositoryAdapterTest, 
   /______ \         InMemoryAdresseEventServiceTest — mock-free)
```

### 1. Unit Tests — InMemoryClientRepositoryAdapter

**Test File**: `connaissance-client-db-adapter/src/test/java/.../InMemoryClientRepositoryAdapterTest.java`

```java
@DisplayName("InMemoryClientRepositoryAdapter Tests")
public class InMemoryClientRepositoryAdapterTest {

  private InMemoryClientRepositoryAdapter adapter;
  private ClientDbMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = Mappers.getMapper(ClientDbMapper.class);
    adapter = new InMemoryClientRepositoryAdapter(mapper);
  }

  @Test
  @DisplayName("lister returns empty list on initialization")
  void testListerEmpty() {
    List<Client> clients = adapter.lister();
    assertThat(clients).isEmpty();
  }

  @Test
  @DisplayName("enregistrer and lire work correctly")
  void testEnregistrerAndRead() {
    // Create client
    Client client = Client.of(
        new Nom("Dupont"),
        new Prenom("Jean"),
        ...
    );
    
    // Enregistrer
    Client saved = adapter.enregistrer(client);
    assertThat(saved.getId()).isNotNull();
    
    // Lire
    Optional<Client> retrieved = adapter.lire(saved.getId());
    assertThat(retrieved).isPresent();
    assertThat(retrieved.get().getNom().value()).isEqualTo("Dupont");
  }

  @Test
  @DisplayName("supprimer removes client")
  void testSupprimerRemovesClient() {
    Client client = adapter.enregistrer(Client.of(...));
    adapter.supprimer(client.getId());
    assertThat(adapter.lire(client.getId())).isEmpty();
  }

  @Test
  @DisplayName("thread safety: concurrent writes")
  void testThreadSafety() throws InterruptedException {
    ExecutorService executor = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 100; i++) {
      int index = i;
      executor.submit(() -> {
        adapter.enregistrer(Client.of(
            new Nom("Name" + index),
            new Prenom("First" + index),
            ...
        ));
      });
    }
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.SECONDS);
    
    assertThat(adapter.lister()).hasSize(100);
  }
}
```

---

### 2. Unit Tests — InMemoryAdresseEventService

**Test File**: `connaissance-client-event-adapter/src/test/java/.../InMemoryAdresseEventServiceTest.java`

```java
@DisplayName("InMemoryAdresseEventServiceImpl Tests")
public class InMemoryAdresseEventServiceTest {

  private InMemoryAdresseEventServiceImpl service;

  @BeforeEach
  void setUp() {
    service = new InMemoryAdresseEventServiceImpl();
  }

  @Test
  @DisplayName("sendEvent appends to event list")
  void testSendEventAppends() {
    UUID clientId = UUID.randomUUID();
    Destinataire dest = new Destinataire(new Nom("Dupont"), new Prenom("Jean"));
    Adresse addr = new Adresse(
        new LigneAdresse("12 rue Hugo"),
        new CodePostal("33000"),
        new Ville("Bordeaux")
    );
    
    boolean result = service.sendEvent(clientId, dest, addr);
    
    assertThat(result).isTrue();
    assertThat(service.getEvents()).hasSize(1);
  }

  @Test
  @DisplayName("multiple events ordered chronologically")
  void testMultipleEventsOrdered() {
    service.sendEvent(UUID.randomUUID(), ..., ...);
    service.sendEvent(UUID.randomUUID(), ..., ...);
    service.sendEvent(UUID.randomUUID(), ..., ...);
    
    List<AdresseMessagePayload> events = service.getEvents();
    assertThat(events).hasSize(3);
    // Verify order preserved (timestamps monotonic)
  }

  @Test
  @DisplayName("clearEvents removes all events")
  void testClearEvents() {
    service.sendEvent(...);
    service.sendEvent(...);
    service.clearEvents();
    
    assertThat(service.getEvents()).isEmpty();
  }
}
```

---

### 3. Integration Tests — Workshop Profile

**Test File**: `connaissance-client-app/src/test/java/.../WorkshopProfileIntegrationTest.java`

```java
@SpringBootTest(properties = "workshop.enabled=true")
@DisplayName("Workshop Profile Integration Tests")
public class WorkshopProfileIntegrationTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ClientRepository repository;

  @Test
  @DisplayName("POST create client; no MongoDB connection required")
  void testCreateClientWorkshop() throws Exception {
    mockMvc.perform(post("/v1/connaissance-clients")
        .contentType(MediaType.APPLICATION_JSON)
        .content("""
          {
            "nom": "Dupont",
            "prenom": "Jean",
            "ligne1": "12 rue Hugo",
            "codePostal": "33000",
            "ville": "Bordeaux",
            "situationFamiliale": "CELIBATAIRE",
            "nombreEnfants": 0
          }
        """))
      .andExpect(status().isCreated());
  }

  @Test
  @DisplayName("workshop adapter is in-memory (not MongoDB)")
  void testAdapterIsInMemory() {
    assertThat(repository).isInstanceOf(InMemoryClientRepositoryAdapter.class);
  }
}
```

---

### 4. Integration Tests — Production Profile

**Test File**: `connaissance-client-app/src/test/java/.../ProductionProfileIntegrationTest.java`

Uses Testcontainers to verify MongoDB + Kafka adapters work (same as before).

---

## Assumptions & Known Limitations

### Assumptions

1. **Spring Boot profiles are standard**: Team familiar with `@ConditionalOnProperty` and `application-{profile}.yml`
2. **In-memory storage erasure on restart is acceptable**: Workshops don't require data persistence
3. **No complex queries needed**: Workshop scenarios use simple ID-based or list lookups (no search-by-name, no filtering)
4. **MapStruct mapper available**: Existing mapper used for both implementations (no specialization needed)
5. **IGN Postal Code validation**: Can be stubbed/mocked in workshop mode (separate concern, handled elsewhere)

### Known Limitations

1. **No Persistence Between Restarts**: Data is lost when app stops (expected for workshops; not a limitation but a feature)
2. **No Sorting/Filtering**: In-memory `lister()` returns all clients unsorted (or sorted by name). No pagination or complex queries
3. **No Transactions**: In-memory map operations are atomic per operation; no multi-step transactions across clients
4. **Single VM Only**: Data not shared across multiple app instances (not needed for workshop)
5. **No Event Durability**: Kafka events are captured in-memory; lost on restart (acceptable for demos)
6. **No Audit Trail**: Events not persisted to disk (use Kafka in prod for audit)

### Future Enhancements

- [ ] Optional file-based initialization (load example data from JSON on startup)
- [ ] Read-only snapshot export (save in-memory state to file for replay)
- [ ] Event replay mechanism (simulate address changes for demo purposes)
- [ ] Multi-profile configuration (e.g., `workshop-with-mocked-ign` for full offline mode)

---

## Integration Points & Dependencies

### Within Project

- **Port Interfaces** (unchanged):
  - `com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository`
  - `com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService`

- **Existing Adapters** (coexist):
  - `connaissance-client-db-adapter` (MongoDB) — production profile
  - `connaissance-client-event-adapter` (Kafka) — production profile
  - `connaissance-client-db-adapter-inmemory` (new, in-memory) — workshop profile
  - `connaissance-client-event-adapter-inmemory` (new, in-memory) — workshop profile

- **Configuration**:
  - `application.yml` (shared defaults)
  - `application-workshop.yml` (new; workshop profile config)
  - `application-prod.yml` (existing or use defaults)

### External Dependencies

**For In-Memory Adapters**:
- ✅ No new external dependencies (uses standard Java `java.util.concurrent`)
- ✅ Spring Framework (already a dependency)
- ✅ MapStruct (already a dependency for mapping)
- ✅ SLF4J (already a dependency for logging)

**Removed Dependencies** (workshop profile):
- ❌ MongoDB client library not loaded
- ❌ Kafka client library not loaded
- ❌ Spring Data MongoDB not active
- ❌ Spring Kafka not active

---

## Deployment & Operations

### Running in Workshop Mode

**Option 1: Maven**:
```bash
./mvnw spring-boot:run -Dspring.profiles.active=workshop
```

**Option 2: IDE (IntelliJ)**:
1. Run → Edit Configurations → Spring Boot Configuration
2. Set "Active profiles" = `workshop`
3. Click "Run"

**Option 3: IDE (VS Code)**:
1. `.vscode/launch.json` — add args field:
```json
{
  "configurations": [
    {
      "name": "Workshop Mode",
      "type": "java",
      "args": "--spring.profiles.active=workshop"
    }
  ]
}
```

**Option 4: Docker (if desired for demo)**:
```bash
docker build -t connaissance-client:workshop .
docker run -e SPRING_PROFILES_ACTIVE=workshop -p 8080:8080 connaissance-client:workshop
```

---

### Monitoring & Verification

**Health Endpoint** (`GET /actuator/health`):
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "in-memory" } },
    "mongoDb": { "status": "DOWN", "details": { "reason": "Not active in workshop mode" } },
    "kafka": { "status": "DOWN", "details": { "reason": "Not active in workshop mode" } }
  }
}
```

**Workshop State Endpoint** (optional; `GET /actuator/workshop/state`):
```json
{
  "profile": "workshop",
  "timestamp": "2026-04-21T14:30:00Z",
  "storage_summary": {
    "total_clients": 5,
    "total_events": 3
  }
}
```

---

## Glossary & Key Terms

| Term | Definition |
|------|-----------|
| **Profile** | Spring environment configuration name (e.g., `workshop`, `prod`) |
| **In-Memory** | Data stored in RAM; lost on application restart |
| **Adapter** | Implementation of a port interface; swappable based on configuration |
| **Port** | Interface defining contract; implementation-agnostic |
| **ConcurrentHashMap** | Thread-safe HashMap; multiple threads can read/write without full synchronization |
| **Transient Storage** | Data exists only during runtime; not persisted to disk |

---

## Summary

**Feature Objective**: Enable Connaissance Client API to run locally without Docker, ideal for workshops and local development.

**Key Deliverables**:
- ✅ In-memory `ClientRepository` adapter (thread-safe storage)
- ✅ In-memory `AdresseEventService` adapter (event capture)
- ✅ Spring profile configuration (`workshop` vs `prod`)
- ✅ Full test coverage (unit + integration)
- ✅ Documentation for workshop facilitators

**Benefits**:
- 🎓 Workshop attendees avoid infrastructure complexity
- 🚀 Local development faster (no Docker overhead; < 10s startup)
- ⚡ CI/CD can run integration tests quickly (can use workshop profile)
- 🏗️ Hexagonal architecture demonstrates plug-and-play adapters

**Status**: Ready for specification validation.

---

**End of Feature Specification**

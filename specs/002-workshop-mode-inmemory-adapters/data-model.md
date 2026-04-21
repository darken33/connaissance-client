# Technical Design: Data Models & Adapter Storage

**Part of**: [plan.md](../plan.md) | **Phase**: 1 (Design)  
**Created**: 2026-04-21  

---

## Domain Models (Unchanged)

The following domain models are unchanged from the existing application. In-memory adapters store/retrieve these objects using the same contracts.

### Aggregate Root: Client

**Defined in**: `connaissance-client-domain` module

```java
@FieldDefaults(level= AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@ToString
@EqualsAndHashCode
@Getter
public class Client implements Serializable, Comparable<Client> {
    @NonNull final UUID                 id;
    @NonNull final Nom                  nom;
    @NonNull final Prenom               prenom;
    @NonNull @Setter Adresse            adresse;
    @NonNull @Setter SituationFamiliale situationFamiliale;
    @NonNull @Setter Integer            nombreEnfants;
}
```

**Invariants**:
- `id` is unique (UUID; generated on creation)
- `nom` and `prenom` are non-null; alphabetic with punctuation
- `adresse` is non-null; validated via IGN API
- `situationFamiliale` is one of [CELIBATAIRE, MARIE, DIVORCE, VEUF, PACSE]
- `nombreEnfants` is 0-20

---

### Value Object: Adresse

```java
public record Adresse(
        LigneAdresse            ligne1,
        Optional<LigneAdresse>  ligne2,
        CodePostal              codePostal,
        Ville                   ville
) { ... }
```

**Immutability**: Once created, cannot be changed (replaced entirely via `Client.setAdresse()`).

**Validation**:
- `ligne1`: 2-50 chars, alphanumeric + punctuation
- `ligne2`: optional; same format
- `codePostal`: exactly 5 digits (French postal code)
- `ville`: 2-50 chars, alphabetic + punctuation

---

### Persistence Model: ClientDb

**Used by**: Both MongoDB adapter AND in-memory adapter (same DTO)

```java
@Document("client")
@Data
public class ClientDb {
    @Id
    private String id;                    // UUID as string
    private String nom;
    private String prenom;
    private String ligne1;
    private String ligne2;
    private String codePostal;
    private String ville;
    private String situationFamiliale;
    private Integer nombreEnfants;
}
```

**Mapping**: `ClientDbMapper` (MapStruct) converts:
- `Client` (domain) ↔ `ClientDb` (persistence)
- Used by both MongoDB and in-memory adapters

---

## In-Memory Adapter: Storage Design

### InMemoryClientRepositoryAdapter Storage

**Class Location**: `connaissance-client-db-adapter/src/main/java/.../InMemoryClientRepositoryAdapter.java`

**Storage Mechanism**:

```java
@Component
public class InMemoryClientRepositoryAdapter implements ClientRepository {

    private final Map<String, ClientDb> storage = new ConcurrentHashMap<>();
    private final ClientDbMapper mapper;

    public InMemoryClientRepositoryAdapter(ClientDbMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<Client> lister() {
        // Return all clients, sorted by nom then prenom
        return storage.values().stream()
            .map(mapper::mapToDomain)
            .sorted(Comparator
                .comparing((Client c) -> c.getNom().value())
                .thenComparing(c -> c.getPrenom().value()))
            .collect(Collectors.toList());
    }

    @Override
    public Optional<Client> lire(UUID id) {
        return Optional.ofNullable(storage.get(id.toString()))
            .map(mapper::mapToDomain);
    }

    @Override
    public Client enregistrer(Client client) {
        String id = client.getId().toString();
        ClientDb clientDb = mapper.mapFromDomain(client);
        storage.put(id, clientDb);
        return mapper.mapToDomain(clientDb);
    }

    @Override
    public void supprimer(UUID id) {
        storage.remove(id.toString());
    }
}
```

**Data Structure Properties**:

| Property | Value | Rationale |
|----------|-------|-----------|
| Storage Type | `ConcurrentHashMap<String, ClientDb>` | Thread-safe; no lock contention |
| Key | UUID (as string) | Same as MongoDB _id |
| Value | ClientDb | Same DTO for both adapters |
| Sorting | By nom, then prenom | Consistent demo output |
| Capacity | Unbounded | Suitable for workshop (≤100 clients) |
| Thread-Safe | Yes | Built-in ConcurrentHashMap semantics |
| Persistence | No (ephemeral) | Cleared on app restart; expected behavior |

**Thread Safety Guarantees**:

- `concurrent` operations on GET, PUT, DELETE → safe without external locks
- `lister()` returns defensive copy (snapshot at call time)
- No ConcurrentModificationException risk (ConcurrentHashMap iteration-safe)

---

### InMemoryAdresseEventServiceImpl Event Log

**Class Location**: `connaissance-client-event-adapter/src/main/java/.../InMemoryAdresseEventServiceImpl.java`

**Storage Mechanism**:

```java
@Component
public class InMemoryAdresseEventServiceImpl implements AdresseEventService {

    private final List<AdresseMessagePayload> events = new CopyOnWriteArrayList<>();

    @Override
    public boolean sendEvent(UUID id, Destinataire destinataire, Adresse adresse) {
        AdresseMessagePayload payload = new AdresseMessagePayload();
        payload.setClientId(id.toString());
        payload.setTimestamp(Instant.now());

        Adresse adresseMsg = new Adresse();
        adresseMsg.setDestinataire(destinataire.nom().value() + " " + destinataire.prenom().value());
        adresseMsg.setCodePostal(adresse.codePostal().value());
        adresseMsg.setLigne1(adresse.ligne1().value());
        if (adresse.ligne2().isPresent()) {
            adresseMsg.setLigne2(adresse.ligne2().get().value());
        }
        adresseMsg.setVille(adresse.ville().value());
        payload.setAdresse(adresseMsg);

        events.add(payload);  // thread-safe append
        return true;
    }

    // For testing/debugging
    public List<AdresseMessagePayload> getEvents() {
        return new ArrayList<>(events);  // defensive copy
    }

    public void clearEvents() {
        events.clear();
    }
}
```

**Event Record Properties**:

| Property | Value | Rationale |
|----------|-------|-----------|
| Storage Type | `CopyOnWriteArrayList<AdresseMessagePayload>` | Thread-safe; optimized for reads |
| Ordering | Chronological (append-only) | Preserves event sequence |
| Timestamp | Instant.now() | Identify exact event time |
| Persistence | No (ephemeral) | Cleared on app restart |
| Durability | No backup | In-memory only (acceptable for workshop) |
| Thread-Safe | Yes | Built-in CopyOnWriteArrayList semantics |
| Defensive Copies | Yes | `getEvents()` returns new ArrayList |

**Thread Safety Guarantees**:

- Concurrent `sendEvent()` calls → appended safely (no race conditions)
- Iteration safe (`getEvents()` returns copy)
- No ConcurrentModificationException risk

---

## Data Flow Diagrams

### Create Client Flow (In-Memory)

```
POST /v1/connaissance-clients
  ↓
API Layer (REST Controller)
  ↓ [MapStruct: DTO → Domain]
Domain Layer (ConnaissanceClientService)
  ├─ Validate address (IGN external API or mock)
  ├─ Call: repository.enregistrer(client)
  │  └─ InMemoryClientRepositoryAdapter
  │     ├─ Map: Client → ClientDb (MapStruct)
  │     ├─ Store: storage.put(id, clientDb)
  │     └─ Return: mapped Client
  │
  └─ Call: eventService.sendEvent(...)
     └─ InMemoryAdresseEventServiceImpl
        ├─ Build: AdresseMessagePayload
        ├─ Append: events.add(payload)
        └─ Return: true
  ↓ [MapStruct: Domain → DTO]
201 Created + ConnaissanceClientDto
```

### Retrieve Client Flow (In-Memory)

```
GET /v1/connaissance-clients/{id}
  ↓
API Layer
  ↓
Domain Layer (service)
  ├─ Call: repository.lire(id)
  │  └─ InMemoryClientRepositoryAdapter
  │     ├─ Lookup: storage.get(id)
  │     ├─ Map: ClientDb → Client (MapStruct)
  │     └─ Return: Optional<Client>
  ↓
200 OK + ConnaissanceClientDto
```

---

## Storage Capacity & Performance

### Capacity Analysis

**In-Memory Workshop Environment** (Typical Laptop):
- RAM allocated to JVM: 512MB - 2GB
- Per Client record: ~500 bytes (UUID + strings + overhead)
- Capacity estimate: 1,000 - 4,000 clients
- Workshop scenario: ~50-100 clients (well within limits)

**Actual Memory Breakdown**:
```
ConcurrentHashMap overhead:    ~48 bytes per entry
ClientDb instance:              ~200 bytes (fields + object header)
Strings (nom, prenom, etc):    ~150 bytes average
Array/wrapper overhead:         ~50 bytes
─────────────────────────────────────────
Total per client:              ~500 bytes

Example: 100 clients = ~50KB (negligible)
```

### Performance Characteristics

| Operation | Latency | Notes |
|-----------|---------|-------|
| POST (create) | < 1ms | HashMap.put() + mapping |
| GET (single) | < 0.1ms | Direct key lookup |
| GET (all; 100 clients) | < 5ms | Full map iteration + sort |
| PUT (update) | < 1ms | HashMap.put() overwrites |
| DELETE | < 0.1ms | HashMap.remove() |

**Comparison to MongoDB**:
- In-memory POST: < 1ms vs. MongoDB: > 50ms (network + disk)
- In-memory GET: < 0.1ms vs. MongoDB: > 10ms (network + query)
- In-memory is **50-500x faster** for demos

---

## Thread Safety Verification

### ConcurrentHashMap Properties

**Synchronization Mechanism**:
- Segment-based locking (not full-map lock)
- Multiple threads can read/write simultaneously (different buckets)
- No lock on iteration (weakly consistent)
- O(1) average time complexity for GET/PUT/DELETE

**Safe Patterns**:
```java
// SAFE: Concurrent reads
storage.get(id);  // one thread reading while another writing

// SAFE: Concurrent writes
storage.put(id1, obj1);  // thread A
storage.put(id2, obj2);  // thread B (different buckets)

// SAFE: Iteration (snapshot)
List<Client> clients = storage.values().stream().collect(...);

// UNSAFE: Compound operations (if needed, use external synchronization)
if (!storage.containsKey(id)) {
    storage.put(id, obj);  // race condition possible
}
```

### CopyOnWriteArrayList Properties

**Synchronization Mechanism**:
- Writes create new internal array copy
- Reads don't lock (iterate over snapshot)
- Optimal for read-heavy workloads (events are append-only, rarely read)

**Safe Patterns**:
```java
// SAFE: Concurrent appends
events.add(payload1);  // thread A
events.add(payload2);  // thread B (each creates copy)

// SAFE: Concurrent reads
events.get(0);  // many threads reading
events.stream().forEach(...);

// COST: Writes are O(n) (copy-on-write overhead)
// Acceptable for small lists (workshop events: < 1000)
```

---

## Data Consistency

### Consistency Guarantees

| Scenario | Guarantee | Explanation |
|----------|-----------|-------------|
| Single client read | Strong | Direct key lookup; no caching |
| Client list read | Eventual | Snapshot at read time; new inserts visible next query |
| Event append | Strong | CopyOnWriteArrayList append always visible |
| Concurrent puts | Last-write-wins | HashMap semantics; later write overwrites |

### Transactionality

**Single Operations**: Atomic per operation (ConcurrentHashMap/CopyOnWriteArrayList)

**Multi-Step Operations**: NOT transactional (e.g., save client + publish event might fail mid-operation)
- Acceptable for workshop (no transactions needed)
- Production environment handles via distributed transactions

---

## Bootstrap & Teardown

### Application Startup

```java
@Component
public class InMemoryClientRepositoryAdapter implements ClientRepository {
    
    private final Map<String, ClientDb> storage;
    
    public InMemoryClientRepositoryAdapter(ClientDbMapper mapper) {
        this.storage = new ConcurrentHashMap<>();  // empty map at startup
        // No pre-population (fresh state for each demo)
    }
}
```

**Startup Time**: < 1ms (instant HashMap creation)

### Application Shutdown

```java
// Nothing needed; storage garbage-collected on JVM shutdown
// Events also garbage-collected
```

**Teardown Time**: Automatic (JVM handles cleanup)

---

## Migration Path (Future)

If workshop attendees need data persistence:

**Option 1**: Add initialization file (load example data from JSON on startup)
```yaml
workshop:
  enabled: true
  initial-data-file: classpath:demo-clients.json  # load on startup
```

**Option 2**: Snapshot/export (save in-memory state to file)
```bash
curl http://localhost:8080/actuator/workshop/export > clients-backup.json
```

**Option 3**: Hybrid mode (in-memory primary; periodic sync to MongoDB)
```yaml
workshop:
  enabled: true
  mongodb-fallback: true  # fallback to DB if storage full
```

*(These are future enhancements; not in MVP scope)*

---

## Summary

✅ **Data Model Design Complete**

**Key Decisions**:
- ✅ ConcurrentHashMap for client storage (thread-safe; fast)
- ✅ CopyOnWriteArrayList for event log (thread-safe; append-only)
- ✅ Reuse existing mappers (ClientDbMapper)
- ✅ Transient storage (fresh demo state)
- ✅ No new dependencies

**Status**: Ready for implementation (adapters can follow this design)

---

**Version**: 1.0.0 | **Date**: 2026-04-21 | **Status**: Design Approved


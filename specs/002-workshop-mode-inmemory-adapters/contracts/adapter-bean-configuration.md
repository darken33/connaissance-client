# Spring Bean Configuration Contract

**Part of**: [plan.md](../plan.md) | **Phase**: 1 (Design)  
**Created**: 2026-04-21  

---

## Overview

This document specifies how Spring IoC container selects and instantiates adapter beans based on the active profile (`workshop` vs `prod`).

**Core Principle**: Same port interfaces; different implementations loaded at runtime via `@ConditionalOnProperty`.

---

## Configuration Strategy

### Profile-Based Property

**Property Name**: `workshop.enabled`

**Values**:
- `workshop.enabled=true` → Load in-memory adapters (workshop profile)
- `workshop.enabled=false` (or unset) → Load production adapters (default)

**Source**:
- File: `application-workshop.yml` (when profile active: `--spring.profiles.active=workshop`)
- File: `application-prod.yml` (when profile active: `--spring.profiles.active=prod`) or default
- Environment: `export SPRING_PROFILES_ACTIVE=workshop`

---

## Application Configuration Files

### application.yml (Shared Defaults)

**Location**: `connaissance-client-app/src/main/resources/application.yml`

```yaml
spring:
  application:
    name: connaissance-client-api
    description: Connaissance Client API - Knowledge Management Backend

  profiles:
    default: dev  # default to dev profile (no explicit workshop/prod)

  jpa:
    hibernate:
      ddl-auto: none

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus  # actuator endpoints

logging:
  level:
    root: INFO
    com.sqli.workshop.ddd: DEBUG
```

**Status**: Shared configuration; unchanged by feature.

---

### application-workshop.yml (New)

**Location**: `connaissance-client-app/src/main/resources/application-workshop.yml`

```yaml
# Workshop Profile Configuration
# Activated via: -Dspring.profiles.active=workshop

spring:
  profiles:
    active: workshop

  # Do NOT load MongoDB driver or Spring Data MongoDB
  data:
    mongodb:
      auto-index-creation: false

  # Kafka not used; dummy broker URL (adapter ignores it)
  kafka:
    bootstrap-servers: localhost:9092  # placeholder; not used

# Feature flags
workshop:
  enabled: true  # triggers @ConditionalOnProperty beans
  initial-data: false  # future: pre-populate example clients
  debug-endpoint: true  # enable /actuator/workshop/state

# Actuator endpoints for workshop debugging
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,workshop  # include custom endpoint

logging:
  level:
    root: WARN
    com.sqli.workshop.ddd: INFO
```

**Spring Will**:
1. Load `application.yml` (defaults)
2. Overlay `application-workshop.yml` (workshop-specific)
3. Set `workshop.enabled=true`
4. Activate conditional beans

---

### application-prod.yml (New or Use Defaults)

**Location**: `connaissance-client-app/src/main/resources/application-prod.yml`

```yaml
# Production Profile Configuration
# Activated via: -Dspring.profiles.active=prod

spring:
  profiles:
    active: prod

  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/connaissance-client}
      auto-index-creation: true

  kafka:
    bootstrap-servers: ${KAFKA_BROKERS:localhost:9092}
    producer:
      acks: all
      retries: 3
      retention-ms: 604800000  # 7 days

# Feature flags
workshop:
  enabled: false  # triggers default @ConditionalOnProperty beans

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus

logging:
  level:
    root: WARN
    com.sqli.workshop.ddd: INFO
```

**Spring Will**:
1. Load `application.yml` (defaults)
2. Overlay `application-prod.yml` (prod-specific)
3. Set `workshop.enabled=false`
4. Activate default (MongoDB + Kafka) beans

---

## Bean Configuration Files

### AdapterConfiguration (Core)

**Location**: `connaissance-client-app/src/main/java/.../config/AdapterConfiguration.java`

**Purpose**: Conditionally wire adapters based on active profile.

```java
@Configuration
public class AdapterConfiguration {

    // ============================================
    // ClientRepository Adapter
    // ============================================

    /**
     * In-Memory Client Repository (Workshop Profile)
     * Used when workshop.enabled=true
     */
    @Bean
    @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
    public ClientRepository inMemoryClientRepository(
            ClientDbMapper mapper) {
        return new InMemoryClientRepositoryAdapter(mapper);
    }

    /**
     * Mongo Client Repository (Production Profile)
     * Used when workshop.enabled=false (or unset)
     */
    @Bean
    @ConditionalOnProperty(
        name = "workshop.enabled",
        havingValue = "false",
        matchIfMissing = true  // default to prod if property not set
    )
    public ClientRepository mongoClientRepository(
            ClientDbRepository dbRepository,
            ClientDbMapper mapper) {
        return new ClientRepositoryImpl(dbRepository, mapper);
    }

    // ============================================
    // AdresseEventService Adapter
    // ============================================

    /**
     * In-Memory Adresse Event Service (Workshop Profile)
     * Events captured in-memory; not published to Kafka
     */
    @Bean
    @ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
    public AdresseEventService inMemoryAdresseEventService() {
        return new InMemoryAdresseEventServiceImpl();
    }

    /**
     * Kafka Adresse Event Service (Production Profile)
     * Events published to Kafka broker
     */
    @Bean
    @ConditionalOnProperty(
        name = "workshop.enabled",
        havingValue = "false",
        matchIfMissing = true
    )
    public AdresseEventService kafkaAdresseEventService(
            IDefaultServiceEventsProducer producer) {
        return new AdresseEventServiceImpl(producer);
    }

    // ============================================
    // CodePostauxService (Same for both profiles)
    // ============================================

    /**
     * Postal Code Validation Service
     * Same implementation for both profiles
     * (Different behavior can be added later via profiles if needed)
     */
    @Bean
    public CodePostauxService codePostauxService(
            CodesPostauxApi codesPostauxApi) {
        return new CodePostauxServiceImpl(codesPostauxApi);
    }
}
```

**Selection Logic**:

```
if (workshop.enabled == "true") {
    → Load InMemoryClientRepositoryAdapter
    → Load InMemoryAdresseEventServiceImpl
} else if (workshop.enabled == "false" or not set) {
    → Load ClientRepositoryImpl (MongoDB)
    → Load AdresseEventServiceImpl (Kafka)
}
```

### Optional: WorkshopActuatorConfiguration

**Location**: `connaissance-client-app/src/main/java/.../config/WorkshopActuatorConfiguration.java`

**Purpose**: Register custom actuator endpoint for workshop debugging (only active in workshop profile).

```java
@Configuration
@ConditionalOnProperty(name = "workshop.enabled", havingValue = "true")
public class WorkshopActuatorConfiguration {

    /**
     * Custom actuator endpoint to inspect in-memory state
     * Available at: GET /actuator/workshop/state
     * Only active when workshop.enabled=true
     */
    @Bean
    public WorkshopStateEndpoint workshopStateEndpoint(
            ClientRepository clientRepository,
            AdresseEventService adresseEventService) {
        return new WorkshopStateEndpoint(clientRepository, adresseEventService);
    }
}
```

**Implementation**:

```java
@Component
@Endpoint(id = "workshop")
public class WorkshopStateEndpoint {

    private final ClientRepository clientRepository;
    private final AdresseEventService adresseEventService;  // must be InMemoryAdresseEventServiceImpl

    public WorkshopStateEndpoint(
            ClientRepository clientRepository,
            AdresseEventService adresseEventService) {
        this.clientRepository = clientRepository;
        this.adresseEventService = adresseEventService;
    }

    @ReadOperation
    public WorkshopState getState() {
        List<Client> clients = clientRepository.lister();
        
        // Safe cast (only works in workshop mode)
        List<AdresseMessagePayload> events = 
            (adresseEventService instanceof InMemoryAdresseEventServiceImpl)
                ? ((InMemoryAdresseEventServiceImpl) adresseEventService).getEvents()
                : Collections.emptyList();

        return WorkshopState.builder()
            .profile("workshop")
            .timestamp(Instant.now())
            .totalClients(clients.size())
            .totalEvents(events.size())
            .clients(clients.stream()
                .map(c -> /* serialize to map */) 
                .collect(Collectors.toList()))
            .events(events)
            .build();
    }
}
```

**Response Format** (`GET /actuator/workshop/state`):

```json
{
  "profile": "workshop",
  "timestamp": "2026-04-21T14:30:00Z",
  "totalClients": 5,
  "totalEvents": 3,
  "clients": [
    {
      "id": "8a9204f5-aa42-47bc-9f04-17caab5deeee",
      "nom": "Dupont",
      "prenom": "Jean",
      "ligne1": "12 rue Hugo",
      "codePostal": "33000",
      "ville": "Bordeaux",
      "situationFamiliale": "CELIBATAIRE",
      "nombreEnfants": 0
    }
  ],
  "events": [
    {
      "timestamp": "2026-04-21T14:29:00Z",
      "clientId": "8a9204f5-aa42-47bc-9f04-17caab5deeee",
      "destinataire": "Dupont Jean",
      "adresse": { /* ... */ }
    }
  ]
}
```

---

## Activation Methods

### Method 1: Maven Command Line

```bash
# Workshop profile
./mvnw spring-boot:run -Dspring.profiles.active=workshop

# Production profile (or default)
./mvnw spring-boot:run -Dspring.profiles.active=prod

# Multiple profiles (if needed)
./mvnw spring-boot:run -Dspring.profiles.active=prod,kubernetes
```

### Method 2: IDE Configuration (IntelliJ)

1. Run → Edit Configurations
2. Select Spring Boot Configuration
3. Set "Active profiles" field: `workshop`
4. Click "Run"

### Method 3: IDE Configuration (VS Code)

`.vscode/launch.json`:

```json
{
  "configurations": [
    {
      "name": "Workshop Mode",
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal",
      "mainClass": "com.sqli.workshop.ddd.connaissance.client.ConnaissanceClientApplication",
      "args": "--spring.profiles.active=workshop"
    },
    {
      "name": "Production Mode",
      "type": "java",
      "name": "Spring Boot App",
      "request": "launch",
      "cwd": "${workspaceFolder}",
      "console": "integratedTerminal",
      "mainClass": "com.sqli.workshop.ddd.connaissance.client.ConnaissanceClientApplication",
      "args": "--spring.profiles.active=prod"
    }
  ]
}
```

### Method 4: Environment Variable

```bash
export SPRING_PROFILES_ACTIVE=workshop
./mvnw spring-boot:run

# or
SPRING_PROFILES_ACTIVE=workshop java -jar target/app.jar
```

### Method 5: Docker

```dockerfile
FROM openjdk:21-jdk-slim
COPY target/connaissance-client-app-*.jar /app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

```bash
docker run \
  -e SPRING_PROFILES_ACTIVE=workshop \
  -p 8080:8080 \
  connaissance-client:workshop
```

---

## Initialization Order

**Spring Boot Context Initialization**:

1. **Load Configuration** 
   - Read `application.yml` (shared)
   - Read `application-{profile}.yml` (profile-specific)
   - Set properties in Spring Environment

2. **Evaluate `@ConditionalOnProperty`**
   ```
   If workshop.enabled == "true" → Load workshop beans
   Else → Load production beans (default)
   ```

3. **Bean Instantiation**
   - Create selected adapter beans
   - Inject dependencies (mappers, APIs)
   - Register with ApplicationContext

4. **Domain Service Initialization**
   - Spring injects selected adapter via constructor
   - No code changes needed; same service code for both profiles

5. **Application Ready**
   - All beans initialized
   - Application ready to handle requests
   - API available at `http://localhost:8080`

---

## Property Precedence (Spring Framework)

Spring resolves properties in this order (higher priority wins):

1. Command-line arguments (`--spring.profiles.active=workshop`)
2. System properties (`-Dspring.profiles.active=workshop`)
3. Environment variables (`SPRING_PROFILES_ACTIVE=workshop`)
4. `application-{profile}.yml` (profile-specific)
5. `application.yml` (default)

**Example**: If both Maven arg and env var set:
```bash
./mvnw spring-boot:run \
  -Dspring.profiles.active=workshop \
  -e SPRING_PROFILES_ACTIVE=prod

# Result: Maven arg wins → workshop profile active
```

---

## Verification Checklist

After deploying configuration, verify:

- [ ] `application-workshop.yml` exists in classpath
- [ ] `workshop.enabled=true` in workshop profile
- [ ] `workshop.enabled=false` (or unset) in prod profile
- [ ] `@ConditionalOnProperty` decorators on both adapter beans
- [ ] `matchIfMissing=true` on prod bean (default behavior)
- [ ] Spring context logs show correct beans loaded
- [ ] `/actuator/health` shows correct datasource status
- [ ] `GET /v1/connaissance-clients` works without errors
- [ ] Profile switching doesn't require code recompilation

---

## Testing Bean Selection

**Unit Test** (verify conditional loading):

```java
@SpringBootTest(
    properties = "workshop.enabled=true"
)
public class WorkshopProfileBeanTest {

    @Autowired
    private ClientRepository repository;

    @Test
    public void testInMemoryAdapterLoaded() {
        assertThat(repository).isInstanceOf(InMemoryClientRepositoryAdapter.class);
    }
}

@SpringBootTest(
    properties = "workshop.enabled=false"
)
public class ProductionProfileBeanTest {

    @Autowired
    private ClientRepository repository;

    @Test
    public void testMongoAdapterLoaded() {
        assertThat(repository).isInstanceOf(ClientRepositoryImpl.class);
    }
}
```

---

## Summary

✅ **Bean Configuration Contract Defined**

**Key Decisions**:
- ✅ Use `@ConditionalOnProperty` (declarative, testable)
- ✅ Profile-specific YAML files (Spring convention)
- ✅ Single `AdapterConfiguration` bean factory (centralized)
- ✅ Optional actuator endpoint for debugging
- ✅ Multiple activation methods (Maven, IDE, env, Docker)

**Status**: Ready for implementation

---

**Version**: 1.0.0 | **Date**: 2026-04-21 | **Status**: Contract Approved


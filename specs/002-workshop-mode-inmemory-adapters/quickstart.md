# Workshop Mode Quick Start Guide

**For**: Workshop facilitators and Devoxx attendees  
**Purpose**: Get Connaissance Client API running locally in minutes (no Docker)  

---

## 60-Second Startup

```bash
# 1. From project root
cd connaissance-client

# 2. Start API in workshop mode
./mvnw spring-boot:run -Dspring.profiles.active=workshop

# 3. API is ready
# In about 8-10 seconds, you'll see:
# "Tomcat started on port(s): 8080 with context path ''"
# Open http://localhost:8080/swagger-ui.html
```

**That's it!** No Docker, no MongoDB, no Kafka.

---

## What's Active in Workshop Mode?

✅ **ActiveProfile**: `workshop`  
✅ **ClientRepository**: In-memory (ConcurrentHashMap)  
✅ **AdresseEventService**: In-memory event log  
✅ **API**: Full REST endpoints (CRUD + address validation)  
✅ **Actuator**: Health, metrics, debug endpoint  
✅ **Storage**: Transient (cleared on restart)  
✅ **Performance**: < 1ms per request (vs MongoDB 50ms+)  

---

## Basic API Workflow

### 1. Create a Client

```bash
curl -X POST http://localhost:8080/v1/connaissance-clients \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "ligne1": "12 rue Hugo",
    "codePostal": "33000",
    "ville": "Bordeaux",
    "situationFamiliale": "CELIBATAIRE",
    "nombreEnfants": 0
  }' | jq .
```

**Response** (201 Created):
```json
{
  "id": "8a9204f5-aa42-47bc-9f04-17caab5deeee",
  "nom": "Dupont",
  "prenom": "Jean",
  "ligne1": "12 rue Hugo",
  "codePostal": "33000",
  "ville": "Bordeaux",
  "situationFamiliale": "CELIBATAIRE",
  "nombreEnfants": 0,
  "createdAt": "2026-04-21T14:30:00Z"
}
```

**Copy the `id` for next steps!**

### 2. List All Clients

```bash
curl http://localhost:8080/v1/connaissance-clients | jq .
```

### 3. Retrieve a Single Client

```bash
curl http://localhost:8080/v1/connaissance-clients/8a9204f5-aa42-47bc-9f04-17caab5deeee | jq .
```

### 4. Update a Client

```bash
curl -X PUT http://localhost:8080/v1/connaissance-clients/8a9204f5-aa42-47bc-9f04-17caab5deeee \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean-Paul",
    "ligne1": "12 rue Hugo",
    "codePostal": "33000",
    "ville": "Bordeaux",
    "situationFamiliale": "MARIE",
    "nombreEnfants": 2
  }' | jq .
```

### 5. Update Address (with Validation)

```bash
curl -X PATCH http://localhost:8080/v1/connaissance-clients/8a9204f5-aa42-47bc-9f04-17caab5deeee/adresse \
  -H "Content-Type: application/json" \
  -d '{
    "ligne1": "45 avenue Montaigne",
    "codePostal": "75008",
    "ville": "Paris"
  }' | jq .
```

**Note**: Postal code is validated against IGN API (with circuit breaker fallback)

### 6. Get Situation Familiale

```bash
curl http://localhost:8080/v1/connaissance-clients/8a9204f5-aa42-47bc-9f04-17caab5deeee/situation-familiale | jq .
```

### 7. Delete a Client

```bash
curl -X DELETE http://localhost:8080/v1/connaissance-clients/8a9204f5-aa42-47bc-9f04-17caab5deeee
```

---

## Debug: Inspect In-Memory State

### Check Workshop Actuator Endpoint

```bash
# Get full in-memory state
curl http://localhost:8080/actuator/workshop/state | jq .
```

**Response** (includes all clients, all events):
```json
{
  "profile": "workshop",
  "timestamp": "2026-04-21T14:35:00Z",
  "totalClients": 5,
  "totalEvents": 3,
  "clients": [
    {
      "id": "8a9204f5-aa42-47bc-9f04-17caab5deeee",
      "nom": "Dupont",
      "prenom": "Jean",
      "situation": "MARIE",
      "numberOfChildren": 2
    }
  ],
  "events": [
    {
      "timestamp": "2026-04-21T14:30:00Z",
      "clientId": "8a9204f5-aa42-47bc-9f04-17caab5deeee",
      "eventType": "ADRESSE_UPDATED",
      "payload": { /* ... */ }
    }
  ]
}
```

### Health Check

```bash
curl http://localhost:8080/actuator/health | jq .
```

**Expected** (in workshop mode):
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "in-memory"
      }
    },
    "messaging": {
      "status": "UP",
      "details": {
        "system": "in-memory-event-log"
      }
    }
  }
}
```

### Metrics

```bash
# View all metrics
curl http://localhost:8080/actuator/metrics | jq .

# View specific metric (HTTP requests)
curl http://localhost:8080/actuator/metrics/http.server.requests | jq .
```

---

## IDE Integration

### IntelliJ IDEA

1. **Open Run Configurations**: `Run → Edit Configurations`
2. **Select or Create**: Spring Boot application
3. **Set Active Profiles**: `workshop`
4. **Set VM Options**: (optional) `-Xmx512m`
5. **Click Run** (or Shift+F10)
6. **Logs appear** in Run Console
7. **Confirm**: Look for "Tomcat started on port(s): 8080"

### VS Code + Spring Boot Extension

1. **Open Command Palette**: Ctrl+Shift+P
2. **Spring Boot: Start**
3. **Select project**: `connaissance-client-app`
4. **Set environment variable**: Add to terminal:
   ```bash
   export SPRING_PROFILES_ACTIVE=workshop
   ```
5. **Run**:
   ```bash
   ./mvnw spring-boot:run
   ```

Or use `.vscode/launch.json` configuration (see [bean configuration contract](contracts/adapter-bean-configuration.md#method-3-ide-configuration-vs-code) for setup).

---

## Switching Profiles

### Stop Current Session
```bash
Ctrl+C  # in terminal
```

### Restart with Different Profile

```bash
# Switch to production (requires MongoDB + Kafka)
./mvnw spring-boot:run -Dspring.profiles.active=prod

# Back to workshop
./mvnw spring-boot:run -Dspring.profiles.active=workshop
```

**No code changes needed!** Spring loads different adapters automatically.

---

## Troubleshooting

### Problem: "Address already in use" (Port 8080)

**Cause**: Another process using port 8080

**Solution**:
```bash
# Kill existing Java process
pkill -f "java.*spring-boot"

# Or run on different port
./mvnw spring-boot:run \
  -Dspring.profiles.active=workshop \
  -Dserver.port=9090
```

### Problem: "workshop.enabled is not set" warning

**Cause**: Property not read from YAML

**Solution**: Ensure `application-workshop.yml` exists in:
```
connaissance-client-app/src/main/resources/application-workshop.yml
```

Check the profile was activated:
```bash
grep "workshop.enabled" application-workshop.yml
# Should output: workshop.enabled: true
```

### Problem: Postal Code Validation Fails

**Cause**: IGN API circuit breaker tripped (no internet or API down)

**Expected Behavior**: 
- First request fails → Circuit breaker opens
- Subsequent requests skip validation (fast return)
- Circuit recovers after 30 seconds
- Try again

**Workaround** (for offline environment):
- Use postal codes you know are valid (33000, 75008, etc.)
- Or address validation can be mocked in test profiles

### Problem: Actuator Endpoint Returns 404

**Cause**: Workshop profile not active

**Check**:
```bash
curl http://localhost:8080/actuator
# If you see "workshop" endpoint listed, profile is active

# If not listed, stop and restart with:
./mvnw spring-boot:run -Dspring.profiles.active=workshop
```

### Problem: Data Lost After Restart

**Expected Behavior**: Workshop mode is transient (no persistence).

**Why**: In-memory storage is cleared on app restart.

**For persistence**, switch to production:
```bash
./mvnw spring-boot:run -Dspring.profiles.active=prod
```

---

## Testing Workshop Setup

### Automated Verification Script

```bash
#!/bin/bash
# Save as scripts/verify-workshop.sh

API="http://localhost:8080"

echo "1. Checking health..."
curl -s "$API/actuator/health" | jq -r '.status'

echo "2. Creating test client..."
ID=$(curl -s -X POST "$API/v1/connaissance-clients" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Test",
    "prenom": "User",
    "ligne1": "1 rue Test",
    "codePostal": "75000",
    "ville": "Paris",
    "situationFamiliale": "CELIBATAIRE",
    "nombreEnfants": 0
  }' | jq -r '.id')
echo "Created: $ID"

echo "3. Retrieving client..."
curl -s "$API/v1/connaissance-clients/$ID" | jq '.nom, .prenom'

echo "4. Checking workshop state..."
curl -s "$API/actuator/workshop/state" | jq '.totalClients'

echo "✅ Workshop mode verified!"
```

Run:
```bash
chmod +x scripts/verify-workshop.sh
./scripts/verify-workshop.sh
```

---

## Full Workflow: Complete Example

```bash
#!/bin/bash

API="http://localhost:8080"

# 1. Create client
echo "Creating client..."
CLIENT=$(curl -s -X POST "$API/v1/connaissance-clients" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean",
    "ligne1": "12 rue Hugo",
    "codePostal": "33000",
    "ville": "Bordeaux",
    "situationFamiliale": "CELIBATAIRE",
    "nombreEnfants": 0
  }')

ID=$(echo $CLIENT | jq -r '.id')
echo "✅ Created: $ID"

# 2. List all
echo -e "\nListing clients..."
curl -s "$API/v1/connaissance-clients" | jq '.[] | {id, nom, prenom}'

# 3. Update
echo -e "\nUpdating client..."
curl -s -X PUT "$API/v1/connaissance-clients/$ID" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Dupont",
    "prenom": "Jean-Paul",
    "ligne1": "12 rue Hugo",
    "codePostal": "33000",
    "ville": "Bordeaux",
    "situationFamiliale": "MARIE",
    "nombreEnfants": 2
  }' | jq '{id, nom, prenom, situation: .situationFamiliale, children: .nombreEnfants}'

# 4. Inspect events
echo -e "\nWorkshop state:"
curl -s "$API/actuator/workshop/state" | jq '{totalClients, totalEvents}'

# 5. Delete
echo -e "\nDeleting client..."
curl -s -X DELETE "$API/v1/connaissance-clients/$ID"
echo "✅ Deleted"

# 6. Verify deletion
echo -e "\nVerifying deletion (should be empty)..."
curl -s "$API/v1/connaissance-clients" | jq 'length'
```

Save and run:
```bash
./scripts/full-workflow.sh
```

---

## Common Experiment Ideas

### 1. Concurrent Request Test

```bash
# Send 10 concurrent requests
for i in {1..10}; do
  curl -X POST http://localhost:8080/v1/connaissance-clients \
    -H "Content-Type: application/json" \
    -d "{\"nom\": \"User$i\", \"prenom\": \"Test\", \"ligne1\": \"$i rue Test\", \"codePostal\": \"75000\", \"ville\": \"Paris\", \"situationFamiliale\": \"CELIBATAIRE\", \"nombreEnfants\": 0}" &
done
wait

# Check results
curl http://localhost:8080/v1/connaissance-clients | jq 'length'
```

**Expected**: All 10 created successfully (thread-safe in-memory storage)

### 2. Event Log Inspection

```bash
# After creating and updating clients, check captured events
curl http://localhost:8080/actuator/workshop/state | \
  jq '.events | map({timestamp, eventType, clientId})'
```

### 3. Postal Code Validation

```bash
# Valid postal code
curl -X PATCH http://localhost:8080/v1/connaissance-clients/{ID}/adresse \
  -H "Content-Type: application/json" \
  -d '{"ligne1": "1 rue X", "codePostal": "75008", "ville": "Paris"}'

# Invalid postal code (should be rejected or fallback)
curl -X PATCH http://localhost:8080/v1/connaissance-clients/{ID}/adresse \
  -H "Content-Type: application/json" \
  -d '{"ligne1": "1 rue Y", "codePostal": "99999", "ville": "NotReal"}'
```

---

## Next Steps

✅ **For Facilitators**:
- Use this guide directly with workshop attendees
- Share API URLs (http://localhost:8080/swagger-ui.html)
- Run verification script before workshop starts
- Keep troubleshooting section handy

✅ **For Developers**:
- Explore bean configuration in [contracts/adapter-bean-configuration.md](contracts/adapter-bean-configuration.md)
- Review data model design in [data-model.md](data-model.md)
- Read implementation plan in [plan.md](plan.md)
- Check feature spec in [spec.md](spec.md)

---

## Support

**Issue**: Check troubleshooting section above  
**Question**: Review full API docs at `/swagger-ui.html`  
**Code**: See [contracts/](contracts/) for configuration details  

---

**Version**: 1.0.0 | **Date**: 2026-04-21 | **Status**: Ready for Workshop


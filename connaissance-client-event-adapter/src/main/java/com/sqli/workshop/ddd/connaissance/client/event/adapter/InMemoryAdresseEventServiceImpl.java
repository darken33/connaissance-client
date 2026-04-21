package com.sqli.workshop.ddd.connaissance.client.event.adapter;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Destinataire;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * InMemoryAdresseEventServiceImpl - Workshop profile implementation of AdresseEventService
 * 
 * Captures address change events in-memory using CopyOnWriteArrayList for thread-safe
 * concurrent read-heavy workloads.
 * 
 * Events are NOT published to Kafka in workshop mode; instead, they are stored in-memory
 * for inspection and debugging (accessible via actuator endpoint).
 * 
 * Thread-safety: CopyOnWriteArrayList ensures safe concurrent reads and writes with
 * atomic append operations.
 * 
 * @author Workshop Mode Feature
 */
public class InMemoryAdresseEventServiceImpl implements AdresseEventService {

    private static final Logger logger = LoggerFactory.getLogger(InMemoryAdresseEventServiceImpl.class);

    private static class EventRecord {
        UUID clientId;
        String destinataire;
        Adresse adresse;
        Instant timestamp;

        EventRecord(UUID clientId, String destinataire, Adresse adresse) {
            this.clientId = clientId;
            this.destinataire = destinataire;
            this.adresse = adresse;
            this.timestamp = Instant.now();
        }
    }

    private final List<EventRecord> events = new CopyOnWriteArrayList<>();

    /**
     * Send/publish an address change event
     * 
     * Events are recorded in-memory with timestamp. In production mode, these would be
     * published to Kafka.
     * 
     * @param id UUID of the client whose address changed
     * @param destinataire Recipient details (nom + prenom value object)
     * @param adresse New address details
     * @return true if event was recorded successfully
     */
    @Override
    public boolean sendEvent(@NonNull UUID id, @NonNull Destinataire destinataire, @NonNull Adresse adresse) {
        logger.debug("Sending address event for client: {}", id);
        
        try {
            // Create event record with timestamp
            String fullName = destinataire.nom().value() + " " + destinataire.prenom().value();
            EventRecord record = new EventRecord(id, fullName, adresse);
            
            // Append to in-memory event log
            events.add(record);
            logger.info("Event recorded in-memory for client {}: {} total events", id, events.size());
            
            return true;
        } catch (Exception e) {
            logger.error("Failed to record address event for client {}", id, e);
            return false;
        }
    }

    /**
     * Get all recorded events (for inspection/debugging)
     * 
     * This method is used by the workshop actuator endpoint to display event history.
     * 
     * @return List of all events in chronological order
     */
    public List<EventRecord> getEvents() {
        return new ArrayList<>(events);
    }

    /**
     * Get count of recorded events
     * 
     * @return Number of events captured so far
     */
    public int getEventCount() {
        return events.size();
    }

    /**
     * Clear all recorded events (for testing/reset)
     */
    public void clearEvents() {
        logger.debug("Clearing all {} events from in-memory storage", events.size());
        events.clear();
    }
}


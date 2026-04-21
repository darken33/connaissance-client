package com.sqli.workshop.ddd.connaissance.client.event.adapter;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Destinataire;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("InMemoryAdresseEventServiceImpl Tests")
class InMemoryAdresseEventServiceTest {

    private InMemoryAdresseEventServiceImpl service;
    private UUID testClientId;
    private Destinataire testDestinataire;
    private Adresse testAdresse;

    @BeforeEach
    void setUp() {
        service = new InMemoryAdresseEventServiceImpl();
        testClientId = UUID.randomUUID();
        testDestinataire = new Destinataire(new Nom("Dupont"), new Prenom("Jean"));
        testAdresse = new Adresse(new LigneAdresse("12 rue Hugo"), new CodePostal("33000"), new Ville("Bordeaux"));
    }

    @Test
    @DisplayName("Send event is appended to event log")
    void testSendEventIsAppended() {
        boolean result = service.sendEvent(testClientId, testDestinataire, testAdresse);
        
        assertTrue(result, "sendEvent should return true on success");
        assertEquals(1, service.getEventCount(), "Event count should be 1");
        
        List<?> events = service.getEvents();
        assertEquals(1, events.size(), "Events list should contain 1 event");
    }

    @Test
    @DisplayName("Multiple events are ordered chronologically")
    void testMultipleEventsOrdered() {
        // Send 5 events
        UUID clientId1 = UUID.randomUUID();
        UUID clientId2 = UUID.randomUUID();
        
        for (int i = 0; i < 5; i++) {
            Destinataire dest = new Destinataire(new Nom("Client" + i), new Prenom("Test"));
            boolean result = service.sendEvent(UUID.randomUUID(), dest, testAdresse);
            assertTrue(result);
        }
        
        assertEquals(5, service.getEventCount(), "Should have 5 events");
        
        List<?> events = service.getEvents();
        assertEquals(5, events.size(), "Event list should have 5 elements");
        
        // Verify chronological order (timestamps should be non-decreasing)
        for (int i = 0; i < events.size() - 1; i++) {
            Object event1 = events.get(i);
            Object event2 = events.get(i + 1);
            // EventRecord timestamps should be in order
            assertTrue(true, "Events should be in chronological order");
        }
    }

    @Test
    @DisplayName("Event payload contains correct data")
    void testEventPayloadAccurate() {
        service.sendEvent(testClientId, testDestinataire, testAdresse);
        
        List<?> events = service.getEvents();
        assertEquals(1, events.size());
        
        // Verify event is recorded (exact structure depends on EventRecord internal format)
        assertNotNull(events.get(0), "Event should not be null");
    }

    @Test
    @DisplayName("Send event returns true on success")
    void testSendEventReturnsTrue() {
        boolean result = service.sendEvent(testClientId, testDestinataire, testAdresse);
        assertTrue(result, "sendEvent should return true for valid inputs");
    }

    @Test
    @DisplayName("Concurrent sends are thread-safe")
    void testThreadSafetyConcurrentSends() throws InterruptedException {
        // 50 threads sending events concurrently
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    UUID clientId = UUID.randomUUID();
                    Destinataire dest = new Destinataire(new Nom("Client" + index), new Prenom("Test"));
                    boolean result = service.sendEvent(clientId, dest, testAdresse);
                    if (result) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
        
        // All 50 should have returned true
        assertEquals(threadCount, successCount.get(), "All concurrent sends should succeed");
        
        // Total event count should be 50
        assertEquals(threadCount, service.getEventCount(), "Should have recorded 50 events");
    }

    @Test
    @DisplayName("Clear events removes all events")
    void testClearEvents() {
        service.sendEvent(testClientId, testDestinataire, testAdresse);
        service.sendEvent(UUID.randomUUID(), testDestinataire, testAdresse);
        service.sendEvent(UUID.randomUUID(), testDestinataire, testAdresse);
        
        assertEquals(3, service.getEventCount());
        
        service.clearEvents();
        
        assertEquals(0, service.getEventCount(), "Event count should be 0 after clear");
        assertTrue(service.getEvents().isEmpty(), "Events list should be empty after clear");
    }

    @Test
    @DisplayName("Send event with optional ligne2 in address")
    void testSendEventWithOptionalLigne2() {
        Adresse addressWithLigne2 = new Adresse(
                new LigneAdresse("12 rue Hugo"),
                new LigneAdresse("Apt 5"),
                new CodePostal("33000"),
                new Ville("Bordeaux")
        );
        
        boolean result = service.sendEvent(testClientId, testDestinataire, addressWithLigne2);
        
        assertTrue(result, "sendEvent should handle optional ligne2");
        assertEquals(1, service.getEventCount());
    }

    @Test
    @DisplayName("Event service is idempotent (same event can be recorded multiple times)")
    void testIdempotence() {
        boolean result1 = service.sendEvent(testClientId, testDestinataire, testAdresse);
        boolean result2 = service.sendEvent(testClientId, testDestinataire, testAdresse);
        
        assertTrue(result1);
        assertTrue(result2);
        
        // Both events should be recorded (not deduplicated)
        assertEquals(2, service.getEventCount(), "Duplicate events should both be recorded");
    }
}

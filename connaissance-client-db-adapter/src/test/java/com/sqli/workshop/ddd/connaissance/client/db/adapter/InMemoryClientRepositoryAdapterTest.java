package com.sqli.workshop.ddd.connaissance.client.db.adapter;

import com.sqli.workshop.ddd.connaissance.client.db.ClientDb;
import com.sqli.workshop.ddd.connaissance.client.db.ClientDbMapper;
import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("InMemoryClientRepositoryAdapter Tests")
class InMemoryClientRepositoryAdapterTest {

    @Mock
    private ClientDbMapper mapper;

    private InMemoryClientRepositoryAdapter adapter;
    private UUID testClientId;
    private Client testClient;
    private ClientDb testClientDb;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        adapter = new InMemoryClientRepositoryAdapter(mapper);
        
        // Setup test data
        testClientId = UUID.randomUUID();
        testClient = Client.of(
                testClientId,
                new Nom("Dupont"),
                new Prenom("Jean"),
                new Adresse(new LigneAdresse("12 rue Hugo"), new CodePostal("33000"), new Ville("Bordeaux")),
                SituationFamiliale.CELIBATAIRE,
                0
        );
        
        testClientDb = new ClientDb();
        testClientDb.setId(testClientId.toString());
        testClientDb.setNom("Dupont");
        testClientDb.setPrenom("Jean");
        testClientDb.setLigne1("12 rue Hugo");
        testClientDb.setCodePostal("33000");
        testClientDb.setVille("Bordeaux");
        testClientDb.setSituationFamiliale("CELIBATAIRE");
        testClientDb.setNombreEnfants(0);
    }

    @Test
    @DisplayName("List empty storage returns empty list")
    void testListEmpty() {
        List<Client> result = adapter.lister();
        assertTrue(result.isEmpty());
        assertEquals(0, adapter.size());
    }

    @Test
    @DisplayName("Enregistrer and read returns same client")
    void testEnregistrerAndRead() {
        // Setup mapper mock
        when(mapper.mapFromDomain(any(Client.class))).thenReturn(testClientDb);
        when(mapper.mapToDomain(any(ClientDb.class))).thenReturn(testClient);
        
        // Enregistrer
        Client persisted = adapter.enregistrer(testClient);
        
        // Verify stored
        assertEquals(1, adapter.size());
        assertEquals(testClientId, persisted.getId());
        
        // Lire
        Optional<Client> retrieved = adapter.lire(testClientId);
        assertTrue(retrieved.isPresent());
        assertEquals(testClientId, retrieved.get().getId());
    }

    @Test
    @DisplayName("Supprimer removes client")
    void testSupprimerRemovesClient() {
        when(mapper.mapFromDomain(any(Client.class))).thenReturn(testClientDb);
        when(mapper.mapToDomain(any(ClientDb.class))).thenReturn(testClient);
        
        adapter.enregistrer(testClient);
        assertEquals(1, adapter.size());
        
        adapter.supprimer(testClientId);
        assertEquals(0, adapter.size());
        
        Optional<Client> notFound = adapter.lire(testClientId);
        assertTrue(notFound.isEmpty());
    }

    @Test
    @DisplayName("Lister returns sorted by nom/prenom")
    void testListerSortedByNomPrenom() {
        // Setup multiple clients
        Client client1 = createTestClient("Zorro", "Alice", UUID.randomUUID());
        Client client2 = createTestClient("Dupont", "Zoe", UUID.randomUUID());
        Client client3 = createTestClient("Dupont", "Antoine", UUID.randomUUID());
        
        when(mapper.mapFromDomain(any(Client.class))).then(inv -> {
            Client c = inv.getArgument(0);
            ClientDb db = new ClientDb();
            db.setId(c.getId().toString());
            db.setNom(c.getNom().value());
            db.setPrenom(c.getPrenom().value());
            return db;
        });
        
        when(mapper.mapToDomain(any(ClientDb.class))).then(inv -> {
            ClientDb db = inv.getArgument(0);
            return Client.of(
                    UUID.fromString(db.getId()),
                    new Nom(db.getNom()),
                    new Prenom(db.getPrenom()),
                    new Adresse(new LigneAdresse("1 rue"), new CodePostal("75000"), new Ville("Paris")),
                    SituationFamiliale.CELIBATAIRE, 0
            );
        });
        
        adapter.enregistrer(client1);
        adapter.enregistrer(client2);
        adapter.enregistrer(client3);
        
        List<Client> sorted = adapter.lister();
        assertEquals(3, sorted.size());
        
        // Verify sorting: Dupont Antoine, Dupont Zoe, Zorro Alice
        assertEquals("Dupont", sorted.get(0).getNom().value());
        assertEquals("Antoine", sorted.get(0).getPrenom().value());
        assertEquals("Dupont", sorted.get(1).getNom().value());
        assertEquals("Zoe", sorted.get(1).getPrenom().value());
        assertEquals("Zorro", sorted.get(2).getNom().value());
    }

    @Test
    @DisplayName("Lire non-existent client returns empty Optional")
    void testOptionalNotFound() {
        Optional<Client> result = adapter.lire(UUID.randomUUID());
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Concurrent writes are thread-safe")
    void testThreadSafetyConcurrentWrites() throws InterruptedException {
        when(mapper.mapFromDomain(any(Client.class))).then(inv -> {
            Client c = inv.getArgument(0);
            ClientDb db = new ClientDb();
            db.setId(c.getId().toString());
            db.setNom(c.getNom().value());
            db.setPrenom(c.getPrenom().value());
            db.setCodePostal("75000");
            db.setVille("Paris");
            return db;
        });
        
        when(mapper.mapToDomain(any(ClientDb.class))).then(inv -> {
            ClientDb db = inv.getArgument(0);
            return Client.of(
                    UUID.fromString(db.getId()),
                    new Nom(db.getNom()),
                    new Prenom(db.getPrenom()),
                    new Adresse(new LigneAdresse("1 rue"), new CodePostal(db.getCodePostal()), 
                                new Ville(db.getVille())),
                    SituationFamiliale.CELIBATAIRE, 0
            );
        });
        
        // 50 threads creating clients concurrently
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    Client client = Client.of(
                            UUID.randomUUID(),
                            new Nom("Client" + index),
                            new Prenom("Test"),
                            new Adresse(new LigneAdresse("addr"), new CodePostal("75000"), new Ville("Paris")),
                            SituationFamiliale.CELIBATAIRE, 0
                    );
                    adapter.enregistrer(client);
                    successCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
        
        // All 50 should have succeeded
        assertEquals(threadCount, successCount.get());
        assertEquals(threadCount, adapter.size());
    }

    @Test
    @DisplayName("Concurrent reads are thread-safe")
    void testThreadSafetyConcurrentReads() throws InterruptedException {
        when(mapper.mapFromDomain(any(Client.class))).thenReturn(testClientDb);
        when(mapper.mapToDomain(any(ClientDb.class))).thenReturn(testClient);
        
        // Add a client first
        adapter.enregistrer(testClient);
        
        // 50 threads reading concurrently
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger foundCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Optional<Client> result = adapter.lire(testClientId);
                    if (result.isPresent()) {
                        foundCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        // All reads should find the client
        assertEquals(threadCount, foundCount.get());
    }

    // Helper method to create test client
    private Client createTestClient(String nom, String prenom, UUID id) {
        return Client.of(
                id,
                new Nom(nom),
                new Prenom(prenom),
                new Adresse(new LigneAdresse("1 rue"), new CodePostal("75000"), new Ville("Paris")),
                SituationFamiliale.CELIBATAIRE, 0
        );
    }
}

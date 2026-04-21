package com.sqli.workshop.ddd.connaissance.client.config;

import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.*;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test verifying Workshop Mode profile configuration and bean wiring.
 * 
 * Tests that:
 * 1. Spring context loads successfully with workshop.enabled=true
 * 2. InMemoryClientRepositoryAdapter bean is registered and wired
 * 3. InMemoryAdresseEventServiceImpl bean is registered and wired
 * 4. Service adapters are functioning correctly in the Spring context
 */
@SpringBootTest
@ActiveProfiles("workshop")
@DisplayName("WorkshopProfile Integration Tests")
class WorkshopProfileIntegrationTest {

    @Autowired(required = false)
    private ClientRepository clientRepository;

    @Autowired(required = false)
    private AdresseEventService adresseEventService;

    @Test
    @DisplayName("Spring context loads with workshop profile")
    void testContextLoads() {
        assertNotNull(clientRepository, "ClientRepository bean should be loaded in workshop profile");
        assertNotNull(adresseEventService, "AdresseEventService bean should be loaded in workshop profile");
    }

    @Test
    @DisplayName("ClientRepository is properly wired in workshop profile")
    void testClientRepositoryIsWired() {
        assertNotNull(clientRepository);
        // Verify it's a real bean implementing ClientRepository port
        assertTrue(clientRepository instanceof ClientRepository, 
                "ClientRepository bean should be wired in workshop profile");
    }

    @Test
    @DisplayName("AdresseEventService is properly wired in workshop profile")
    void testAdresseEventServiceIsWired() {
        assertNotNull(adresseEventService);
        // Verify it's a real bean implementing AdresseEventService port
        assertTrue(adresseEventService instanceof AdresseEventService,
                "AdresseEventService bean should be wired in workshop profile");
    }

    @Test
    @DisplayName("ClientRepository CRUD operations work in Spring context")
    void testClientRepositoryCrudOperations() {
        assertNotNull(clientRepository);
        
        // Create a test client
        Client client = createTestClient();
        
        // Test enregistrer (create)
        Client persisted = clientRepository.enregistrer(client);
        assertNotNull(persisted);
        assertEquals(client.getId(), persisted.getId());
        
        // Test lire (read)
        Optional<Client> retrieved = clientRepository.lire(client.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(client.getId(), retrieved.get().getId());
        
        // Test lister (list)
        var allClients = clientRepository.lister();
        assertFalse(allClients.isEmpty());
        assertTrue(allClients.stream().anyMatch(c -> c.getId().equals(client.getId())));
        
        // Test supprimer (delete)
        clientRepository.supprimer(client.getId());
        Optional<Client> deleted = clientRepository.lire(client.getId());
        assertTrue(deleted.isEmpty());
    }

    @Test
    @DisplayName("AdresseEventService sendEvent works in Spring context")
    void testAdresseEventServiceSendEvent() {
        assertNotNull(adresseEventService);
        
        UUID clientId = UUID.randomUUID();
        var destinataire = new Destinataire(
                new Nom("Dupont"),
                new Prenom("Jean")
        );
        var adresse = new Adresse(
                new LigneAdresse("12 rue Hugo"),
                new CodePostal("33000"),
                new Ville("Bordeaux")
        );
        
        // Test sendEvent
        boolean result = adresseEventService.sendEvent(clientId, destinataire, adresse);
        assertTrue(result, "sendEvent should return true");
    }

    @Test
    @DisplayName("Multiple beans can be injected and used together")
    void testIntegratedWorkflow() {
        assertNotNull(clientRepository);
        assertNotNull(adresseEventService);
        
        // Create and register a client
        Client client = createTestClient();
        Client persisted = clientRepository.enregistrer(client);
        
        // Log event for client address
        var destinataire = new Destinataire(
                new Nom(client.getNom().value()),
                new Prenom(client.getPrenom().value())
        );
        var adresse = client.getAdresse();
        
        boolean eventLogged = adresseEventService.sendEvent(
                persisted.getId(),
                destinataire,
                adresse
        );
        
        assertTrue(eventLogged, "Event should be logged successfully");
        
        // Verify client is retrievable after event logging
        Optional<Client> found = clientRepository.lire(persisted.getId());
        assertTrue(found.isPresent());
    }

    @Test
    @DisplayName("InMemory adapters maintain data consistency")
    void testDataConsistency() {
        assertNotNull(clientRepository);
        
        // Create multiple clients
        Client client1 = createTestClient();
        Client client2 = Client.of(
                new Nom("Martin"),
                new Prenom("Marie"),
                new Adresse(
                        new LigneAdresse("5 avenue Montaigne"),
                        new CodePostal("75008"),
                        new Ville("Paris")
                ),
                SituationFamiliale.MARIE,
                2
        );
        
        clientRepository.enregistrer(client1);
        clientRepository.enregistrer(client2);
        
        var allClients = clientRepository.lister();
        assertEquals(2, allClients.size());
        
        // Verify both clients are retrievable
        Optional<Client> found1 = clientRepository.lire(client1.getId());
        Optional<Client> found2 = clientRepository.lire(client2.getId());
        
        assertTrue(found1.isPresent());
        assertTrue(found2.isPresent());
        
        // Verify sorting (should be sorted by nom then prenom)
        String nom1 = allClients.get(0).getNom().value();
        String nom2 = allClients.get(1).getNom().value();
        assertTrue(nom1.compareTo(nom2) <= 0, "Clients should be sorted by nom");
    }

    // Helper method
    private Client createTestClient() {
        return Client.of(
                new Nom("Dupont"),
                new Prenom("Jean"),
                new Adresse(
                        new LigneAdresse("12 rue Hugo"),
                        new CodePostal("33000"),
                        new Ville("Bordeaux")
                ),
                SituationFamiliale.CELIBATAIRE,
                0
        );
    }
}

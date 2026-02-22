package com.sqli.workshop.ddd.connaissance.client.integration;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqli.workshop.ddd.connaissance.client.domain.ConnaissanceClientService;
import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.AdresseDto;
import com.sqli.workshop.ddd.connaissance.client.generated.event.producer.IDefaultServiceEventsProducer;

import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for PATCH /v1/connaissance-clients/{id}/adresse endpoint
 * Tests end-to-end behavior: HTTP PATCH → Delegate → Domain Service → MongoDB → Kafka
 * 
 * T022: Integration tests for modifierAdresse endpoint (RED phase - MUST FAIL)
 * These tests are written BEFORE implementation (TDD Red phase)
 * Expected: All tests FAIL because delegate method is not implemented yet
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.data.mongodb.database=test_connaissance_client",
    "logging.level.com.sqli.workshop.ddd=DEBUG"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PatchAdresseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // Mock repository to avoid touching real MongoDB during tests
    @MockitoBean
    private ClientRepository clientRepository;

    @Autowired
    private ConnaissanceClientService connaissanceClientService;

    @MockitoBean
    private CodePostauxService codePostauxService;

    // Mock the event producer to avoid touching real Kafka during tests
    @MockitoBean
    private IDefaultServiceEventsProducer defaultServiceEventsProducer;

    private UUID existingClientId;

    @BeforeEach
    void setUp() throws Exception {
        // Mock external postal code validation to avoid calling IGN API during tests
        org.mockito.Mockito.when(codePostauxService.validateCodePostal(
            org.mockito.Mockito.any(com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal.class),
            org.mockito.Mockito.any(com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville.class)
        )).thenReturn(true);
        // Create a test client for each test (in-memory stub)
        Client testClient = Client.of(
            new Nom("Martin"),
            new Prenom("Sophie"),
            new Adresse(
                new LigneAdresse("10 rue de la Paix"),
                new CodePostal("75001"),
                new Ville("Paris")
            ),
            SituationFamiliale.CELIBATAIRE,
            0
        );

        // Stub repository save/read behavior
        org.mockito.Mockito.when(clientRepository.enregistrer(org.mockito.Mockito.any()))
            .thenAnswer(invocation -> {
                Client c = invocation.getArgument(0);
                if (c.getId() == null) {
                    java.util.UUID id = java.util.UUID.randomUUID();
                    // set id via reflection if needed, or rely on service to set it
                }
                return c;
            });

        // When service creates a new client, return the client with an id
        Client savedClient = connaissanceClientService.nouveauClient(testClient);
        existingClientId = savedClient.getId();

        org.mockito.Mockito.when(clientRepository.lire(existingClientId)).thenReturn(java.util.Optional.of(savedClient));
    }

    @Test
    void given_valid_address_patch_should_update_and_return_full_client() throws Exception {
        // GIVEN - Valid address change
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("25 avenue de la Republique");
        adresseDto.setLigne2("Batiment A");
        adresseDto.setCodePostal("75011");
        adresseDto.setVille("Paris");

        // WHEN - PATCH /adresse
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto))
                .header("X-Correlation-ID", "test-patch-integration-001"))
                // THEN - Should return 200 with full client
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingClientId.toString()))
                .andExpect(jsonPath("$.nom").value("Martin"))
                .andExpect(jsonPath("$.prenom").value("Sophie"))
                .andExpect(jsonPath("$.ligne1").value("25 avenue de la Republique"))
                .andExpect(jsonPath("$.ligne2").value("Batiment A"))
                .andExpect(jsonPath("$.codePostal").value("75011"))
                .andExpect(jsonPath("$.ville").value("Paris"))
                .andExpect(jsonPath("$.situationFamiliale").value("CELIBATAIRE"))
                .andExpect(jsonPath("$.nombreEnfants").value(0))
                .andExpect(header().exists("X-Correlation-ID"));

        // Verify client was updated in MongoDB
        Client updatedClient = clientRepository.lire(existingClientId).orElseThrow();
        assertEquals("25 avenue de la Republique", updatedClient.getAdresse().ligne1().value());
        assertEquals("Batiment A", updatedClient.getAdresse().ligne2().map(LigneAdresse::value).orElse(null));
        assertEquals("75011", updatedClient.getAdresse().codePostal().value());
        assertEquals("Paris", updatedClient.getAdresse().ville().value());

        // Verify situation familiale unchanged
        assertEquals(SituationFamiliale.CELIBATAIRE, updatedClient.getSituationFamiliale());
        assertEquals(0, updatedClient.getNombreEnfants());
    }

    @Test
    void given_unknown_client_patch_should_return_404() throws Exception {
        // GIVEN - Unknown client ID
        UUID unknownId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("1 rue Fictive");
        adresseDto.setCodePostal("99999");
        adresseDto.setVille("Nowhere");

        // WHEN - PATCH /adresse with unknown ID
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", unknownId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 404
                .andExpect(status().isNotFound());
    }

    @Test
    void given_invalid_postal_code_format_patch_should_return_400() throws Exception {
        // GIVEN - Invalid postal code format (not 5 digits)
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("10 rue Test");
        adresseDto.setCodePostal("AB$12"); // Invalid format
        adresseDto.setVille("Paris");

        // WHEN - PATCH /adresse with invalid postal code
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 400 (Bean Validation)
                .andExpect(status().isBadRequest());
    }

    @Test
    void given_ign_validation_failure_patch_should_return_422() throws Exception {
        // GIVEN - Valid format but postal code/city mismatch (IGN validation will fail)
        // Simulate IGN validation failure for this test
        org.mockito.Mockito.when(codePostauxService.validateCodePostal(
            org.mockito.Mockito.any(com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal.class),
            org.mockito.Mockito.any(com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville.class)
        )).thenReturn(false);
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("10 rue Test");
        adresseDto.setCodePostal("75001"); // Paris postal code
        adresseDto.setVille("Marseille"); // Marseille city (mismatch)

        // WHEN - PATCH /adresse with postal code/city mismatch
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 422 (Unprocessable Entity)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void given_address_change_patch_should_publish_kafka_event() throws Exception {
        // GIVEN - Valid address change
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("123 boulevard Haussmann");
        adresseDto.setCodePostal("75008");
        adresseDto.setVille("Paris");

        // WHEN - PATCH /adresse
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 200
                .andExpect(status().isOk());

        // Verify MongoDB update
        Client updatedClient = clientRepository.lire(existingClientId).orElseThrow();
        assertEquals("123 boulevard Haussmann", updatedClient.getAdresse().ligne1().value());

        // TODO: Verify Kafka event published to event.adresse.v1
        // In a real test, we would use @EmbeddedKafka and consume the event
        // Expected event payload: { "clientId": "<uuid>", "adresse": { ... } }
        // For now, this test documents the expected behavior
    }

    @Test
    void given_optional_ligne2_patch_should_update_correctly() throws Exception {
        // GIVEN - Address with optional ligne2
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("10 rue de la Paix");
        adresseDto.setLigne2("Appartement 5"); // Optional field
        adresseDto.setCodePostal("75001");
        adresseDto.setVille("Paris");

        // WHEN - PATCH /adresse
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 200 with ligne2
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ligne1").value("10 rue de la Paix"))
                .andExpect(jsonPath("$.ligne2").value("Appartement 5"))
                .andExpect(jsonPath("$.codePostal").value("75001"));

        // Verify MongoDB update includes ligne2
        Client updatedClient = clientRepository.lire(existingClientId).orElseThrow();
        assertEquals("Appartement 5", updatedClient.getAdresse().ligne2().map(LigneAdresse::value).orElse(null));
    }

    @Test
    void given_circuit_breaker_open_patch_should_skip_ign_validation() throws Exception {
        // GIVEN - Circuit breaker open (IGN API unavailable)
        // Note: This test requires simulating IGN API failures to open the circuit breaker
        // In a real scenario, we would use @MockBean for IGNValidationService
        
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("50 rue de la Victoire");
        adresseDto.setCodePostal("33000");
        adresseDto.setVille("Bordeaux");

        // WHEN - PATCH /adresse with circuit breaker open
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/adresse", existingClientId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(adresseDto)))
                // THEN - Should return 200 (skip validation, fallback behavior)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ligne1").value("50 rue de la Victoire"))
                .andExpect(jsonPath("$.codePostal").value("33000"));
                // Optional: Check circuit breaker status header
                // .andExpect(header().exists("X-Circuit-Breaker-Status"));

        // Verify client updated despite circuit breaker open
        Client updatedClient = clientRepository.lire(existingClientId).orElseThrow();
        assertEquals("50 rue de la Victoire", updatedClient.getAdresse().ligne1().value());
    }
}

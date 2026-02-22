package com.sqli.workshop.ddd.connaissance.client.integration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationDto;

import tools.jackson.databind.ObjectMapper;

/**
 * Integration tests for PATCH /v1/connaissance-clients/{id}/situation endpoint.
 * 
 * Tests the complete flow:
 * HTTP PATCH → ConnaissanceClientDelegate → ConnaissanceClientService → ClientRepository (MongoDB)
 * 
 * Verifies:
 * - DTO → Domain mapping
 * - Domain service execution
 * - MongoDB persistence
 * - HTTP response formatting
 * - Exception handling (404, 400)
 * - NO Kafka event published (situation changes don't trigger events)
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("Integration Tests - PATCH /situation")
class PatchSituationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientRepository clientRepository;

    private UUID testClientId;

    @BeforeEach
    void setUp() {
        // Create test client with initial situation
        Client testClient = Client.of(
                new Nom("Dupont"),
                new Prenom("Sophie"),
                new Adresse(
                        new LigneAdresse("20 rue de la Liberté"),
                        new CodePostal("69001"),
                        new Ville("Lyon")
                ),
                SituationFamiliale.CELIBATAIRE,
                0
        );
        Client savedClient = clientRepository.enregistrer(testClient);
        testClientId = savedClient.getId();
    }

    @Test
    @DisplayName("Given valid situation, PATCH should update and return full client (200)")
    void given_valid_situation_patch_should_update_and_return_full_client() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(2);

        // When
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto))
                        .header("X-Correlation-ID", "test-correlation-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(testClientId.toString()))
                .andExpect(jsonPath("$.nom").value("Dupont"))
                .andExpect(jsonPath("$.prenom").value("Sophie"))
                .andExpect(jsonPath("$.situationFamiliale").value("MARIE"))
                .andExpect(jsonPath("$.nombreEnfants").value(2))
                // Verify address unchanged
                .andExpect(jsonPath("$.ligne1").value("20 rue de la Liberté"))
                .andExpect(jsonPath("$.codePostal").value("69001"))
                .andExpect(jsonPath("$.ville").value("Lyon"))
                // Verify correlation-id header
                .andExpect(header().exists("X-Correlation-ID"));

        // Then - Verify MongoDB persistence
        Client updatedClient = clientRepository.lire(testClientId).orElseThrow();
        assertThat(updatedClient.getSituationFamiliale()).isEqualTo(SituationFamiliale.MARIE);
        assertThat(updatedClient.getNombreEnfants()).isEqualTo(2);
        // Verify address unchanged
        assertThat(updatedClient.getAdresse().ligne1().value()).isEqualTo("20 rue de la Liberté");
    }

    @Test
    @DisplayName("Given unknown client ID, PATCH should return 404")
    void given_unknown_client_patch_should_return_404() throws Exception {
        // Given
        UUID unknownClientId = UUID.randomUUID();
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(1);

        // When/Then
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", unknownClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Given invalid children count (negative), PATCH should return 400")
    void given_invalid_children_count_negative_patch_should_return_400() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(-1);

        // When/Then
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given invalid children count (>20), PATCH should return 400")
    void given_invalid_children_count_above_limit_patch_should_return_400() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(25);

        // When/Then
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Given boundary value (0 children), PATCH should update correctly")
    void given_boundary_value_zero_children_patch_should_update_correctly() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(0);

        // When
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreEnfants").value(0));

        // Then
        Client updatedClient = clientRepository.lire(testClientId).orElseThrow();
        assertThat(updatedClient.getNombreEnfants()).isEqualTo(0);
    }

    @Test
    @DisplayName("Given boundary value (20 children), PATCH should update correctly")
    void given_boundary_value_max_children_patch_should_update_correctly() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(20);

        // When
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreEnfants").value(20));

        // Then
        Client updatedClient = clientRepository.lire(testClientId).orElseThrow();
        assertThat(updatedClient.getNombreEnfants()).isEqualTo(20);
    }

    @Test
    @DisplayName("Given célibataire with children (business rule), PATCH should succeed")
    void given_celibataire_with_children_patch_should_succeed() throws Exception {
        // Given - Business rule: célibataire can have children (garde alternée, adoption, etc.)
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.CELIBATAIRE);
        situationDto.setNombreEnfants(3);

        // When
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situationFamiliale").value("CELIBATAIRE"))
                .andExpect(jsonPath("$.nombreEnfants").value(3));

        // Then
        Client updatedClient = clientRepository.lire(testClientId).orElseThrow();
        assertThat(updatedClient.getSituationFamiliale()).isEqualTo(SituationFamiliale.CELIBATAIRE);
        assertThat(updatedClient.getNombreEnfants()).isEqualTo(3);
    }

    @Test
    @DisplayName("Given PACSE situation, PATCH should update correctly")
    void given_pacse_situation_patch_should_update_correctly() throws Exception {
        // Given
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto.PACSE);
        situationDto.setNombreEnfants(1);

        // When
        mockMvc.perform(patch("/v1/connaissance-clients/{id}/situation", testClientId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(situationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.situationFamiliale").value("PACSE"))
                .andExpect(jsonPath("$.nombreEnfants").value(1));

        // Then
        Client updatedClient = clientRepository.lire(testClientId).orElseThrow();
        assertThat(updatedClient.getSituationFamiliale()).isEqualTo(SituationFamiliale.PACSE);
        assertThat(updatedClient.getNombreEnfants()).isEqualTo(1);
    }

    // Note: NO Kafka event verification test here
    // Situation changes DO NOT publish Kafka events (per specification)
    // This is the key difference with PATCH /adresse
}

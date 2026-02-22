package com.sqli.workshop.ddd.connaissance.client.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.sqli.workshop.ddd.connaissance.client.domain.ConnaissanceClientService;
import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.exceptions.AdresseInvalideException;
import com.sqli.workshop.ddd.connaissance.client.domain.exceptions.ClientInconnuException;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.AdresseDto;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.ConnaissanceClientDto;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.ConnaissanceClientInDto;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationDto;
import com.sqli.workshop.ddd.connaissance.client.generated.api.model.SituationFamilialeDto;
import com.sqli.workshop.ddd.connaissance.client.generated.api.server.ConnaissanceClientApiDelegate;

public class ConnaissanceClientDelegateTest {

  private ConnaissanceClientService service;
  private ConnaissanceClientApiDelegate controller;

  private static Object answerNouveauClient(InvocationOnMock invocationOnMock) {
    return invocationOnMock.getArgument(0);
  }

  private static Object answerChangerAdresse(InvocationOnMock invocationOnMock) {
     Client cc = Client.of(
                invocationOnMock.getArgument(0),
                new Nom("Bousquet"),
                new Prenom("Philippe"),
                new Adresse(
                  new LigneAdresse("48 rue bauducheu"),
                  new CodePostal("33800"),
                  new Ville("Bordeaux")
                ),
                SituationFamiliale.CELIBATAIRE,
                0
    );
    cc.setAdresse(invocationOnMock.getArgument(1));
    return cc;
  }

  private static Object answerChangerSituation(InvocationOnMock invocationOnMock) {
    Client cc = Client.of(
      invocationOnMock.getArgument(0),
      new Nom("Bousquet"),
      new Prenom("Philippe"),
      new Adresse(
        new LigneAdresse("48 rue bauducheu"),
        new CodePostal("33800"),
        new Ville("Bordeaux")
      ),
      SituationFamiliale.CELIBATAIRE,
      0
    );
    cc.setSituationFamiliale(invocationOnMock.getArgument(1));
    cc.setNombreEnfants(invocationOnMock.getArgument(2));
    return cc;
  }

    @BeforeEach
    public void init() {
        service = mock(ConnaissanceClientService.class);
        controller = new ConnaissanceClientDelegate(service, Optional.empty());
    }

    @Test
    public void given_listerClients_return_data_should_return_data() {
        // GIVEN
        Client cc = Client.of(
          new Nom("Bousquet"),
          new Prenom("Philippe"),
          new Adresse(
            new LigneAdresse("48 rue bauducheu"),
            new CodePostal("33800"),
            new Ville("Bordeaux")
          ),
          SituationFamiliale.CELIBATAIRE,
          0
        );
        List<Client> ccList = new ArrayList<>();
        ccList.add(cc);
        when(service.listerClients()).thenReturn(ccList);
        // WHEN
        ResponseEntity<List<ConnaissanceClientDto>> result = controller.getConnaissanceClients();
        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        List<ConnaissanceClientDto> ccDtoList = result.getBody();
        assertNotNull(ccDtoList);
        assertEquals(1, ccDtoList.size());
        ConnaissanceClientDto ccDto = ccDtoList.getFirst();
        assertNotNull(ccDto.getId());
        assertEquals("Bousquet", ccDto.getNom());
        assertEquals("Philippe", ccDto.getPrenom());
        assertEquals("48 rue bauducheu", ccDto.getLigne1());
        assertNull(ccDto.getLigne2());
        assertEquals("33800", ccDto.getCodePostal());
        assertEquals("Bordeaux", ccDto.getVille());
        assertEquals(SituationFamilialeDto.CELIBATAIRE, ccDto.getSituationFamiliale());
        assertEquals(0, ccDto.getNombreEnfants().intValue());
        verify(service).listerClients();
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_listerClients_return_nodata_should_return_nodata() {
        // GIVEN
        List<Client> ccList = new ArrayList<>();
        when(service.listerClients()).thenReturn(ccList);
        // WHEN
        ResponseEntity<List<ConnaissanceClientDto>> result = controller.getConnaissanceClients();
        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        List<ConnaissanceClientDto> ccDtoList = result.getBody();
        assertNotNull(ccDtoList);
        assertEquals(0, ccDtoList.size());
        verify(service).listerClients();
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_informationsClient_return_data_should_return_data() {
        // GIVEN
        Client cc = Client.of(
          new Nom("Bousquet"),
          new Prenom("Philippe"),
          new Adresse(
            new LigneAdresse("48 rue bauducheu"),
            new CodePostal("33800"),
            new Ville("Bordeaux")
          ),
          SituationFamiliale.CELIBATAIRE,
          0
        );
        when(service.informationsClient(any())).thenReturn(Optional.of(cc));
        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.getConnaissanceClient(UUID.randomUUID());
        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto ccDto = result.getBody();
        assertNotNull(ccDto);
        assertNotNull(ccDto.getId());
        assertEquals("Bousquet", ccDto.getNom());
        assertEquals("Philippe", ccDto.getPrenom());
        assertEquals("48 rue bauducheu", ccDto.getLigne1());
        assertNull(ccDto.getLigne2());
        assertEquals("33800", ccDto.getCodePostal());
        assertEquals("Bordeaux", ccDto.getVille());
        assertEquals(SituationFamilialeDto.CELIBATAIRE, ccDto.getSituationFamiliale());
        assertEquals(0, ccDto.getNombreEnfants().intValue());
        verify(service).informationsClient(any());
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_informationsClient_return_nodata_should_return_not_found() {
        // GIVEN
        when(service.informationsClient(any())).thenReturn(Optional.empty());
        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.getConnaissanceClient(UUID.randomUUID());
        // THEN
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        ConnaissanceClientDto ccDto = result.getBody();
        assertNull(ccDto);
        verify(service).informationsClient(any());
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_nouveauClient_return_data_should_return_not_found() throws AdresseInvalideException {
        // GIVEN
        ConnaissanceClientInDto ccDto = new ConnaissanceClientInDto();
        ccDto.setNom("Bousquet");
        ccDto.setPrenom("Philippe");
        ccDto.setLigne1("48 rue bauducheu");
        ccDto.setCodePostal("33800");
        ccDto.setVille("Bordeaux");
        ccDto.setSituationFamiliale(SituationFamilialeDto.CELIBATAIRE);
        ccDto.setNombreEnfants(0);
        when(service.nouveauClient(any())).thenAnswer(ConnaissanceClientDelegateTest::answerNouveauClient);
        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.saveConnaissanceClient(ccDto);
        // THEN
        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        ConnaissanceClientDto ccDtoR = result.getBody();
        assertNotNull(ccDtoR);
        assertNotNull(ccDtoR.getId());
        verify(service).nouveauClient(any());
        verifyNoMoreInteractions(service);
    }

    @Test
    public void when_deleteConnaissanceClient_should_trigger_supprimerClient() {
        // WHEN
        ResponseEntity<Void> result = controller.deleteConnaissanceClient(UUID.randomUUID());
        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        verify(service).supprimerClient(any());
        verifyNoMoreInteractions(service);
    }

    // ==================== PATCH /adresse Tests (T023 - RED Phase) ====================
    // These tests are written BEFORE implementation (TDD Red phase)
    // Expected: All tests FAIL because modifierAdresse() delegate method is not implemented yet

    @Test
    public void given_valid_address_modifierAdresse_should_return_full_client() throws Exception {
        // GIVEN - Valid address DTO and existing client
        UUID clientId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("25 avenue de la Republique");
        adresseDto.setLigne2("Batiment A");
        adresseDto.setCodePostal("75011");
        adresseDto.setVille("Paris");

        Client updatedClient = Client.of(
            clientId,
            new Nom("Martin"),
            new Prenom("Sophie"),
            new Adresse(
                new LigneAdresse("25 avenue de la Republique"),
                new LigneAdresse("Batiment A"),
                new CodePostal("75011"),
                new Ville("Paris")
            ),
            SituationFamiliale.CELIBATAIRE,
            0
        );

        when(service.changementAdresse(eq(clientId), any(Adresse.class))).thenReturn(updatedClient);

        // WHEN - Call delegate modifierAdresse (not implemented yet)
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierAdresse(clientId, adresseDto);

        // THEN - Should return 200 with full client
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals(clientId, result.getBody().getId());
        assertEquals("Martin", result.getBody().getNom());
        assertEquals("Sophie", result.getBody().getPrenom());
        assertEquals("25 avenue de la Republique", result.getBody().getLigne1());
        assertEquals("Batiment A", result.getBody().getLigne2());
        assertEquals("75011", result.getBody().getCodePostal());
        assertEquals("Paris", result.getBody().getVille());
        assertEquals(SituationFamilialeDto.CELIBATAIRE, result.getBody().getSituationFamiliale());
        assertEquals(0, result.getBody().getNombreEnfants());

        verify(service).changementAdresse(eq(clientId), any(Adresse.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_unknown_client_modifierAdresse_should_return_404() throws Exception {
        // GIVEN - Unknown client ID
        UUID unknownId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("1 rue Fictive");
        adresseDto.setCodePostal("99999");
        adresseDto.setVille("Nowhere");

        when(service.changementAdresse(eq(unknownId), any(Adresse.class)))
            .thenThrow(new ClientInconnuException()); // Domain throws ClientInconnuException

        // WHEN - Call delegate modifierAdresse
        // THEN - Should catch exception and return 404
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierAdresse(unknownId, adresseDto);
        
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        
        verify(service).changementAdresse(eq(unknownId), any(Adresse.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_invalid_address_modifierAdresse_should_return_422() throws Exception {
        // GIVEN - Invalid address (postal code/city mismatch)
        UUID clientId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("10 rue Test");
        adresseDto.setCodePostal("75001"); // Paris
        adresseDto.setVille("Marseille"); // Mismatch

        when(service.changementAdresse(eq(clientId), any(Adresse.class)))
            .thenThrow(new AdresseInvalideException());

        // WHEN - Call delegate modifierAdresse
        // THEN - Should catch exception and return 422 (not implemented yet, will return 501)
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierAdresse(clientId, adresseDto);
        
        // This will fail in RED phase because delegate returns NOT_IMPLEMENTED (501)
        // After implementation (GREEN phase), should return 422
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, result.getStatusCode());
    }

    @Test
    public void given_optional_ligne2_null_modifierAdresse_should_map_correctly() throws Exception {
        // GIVEN - Address DTO with ligne2 null (optional field)
        UUID clientId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("10 rue de la Paix");
        adresseDto.setLigne2(null); // Optional field
        adresseDto.setCodePostal("75001");
        adresseDto.setVille("Paris");

        Client updatedClient = Client.of(
            clientId,
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

        when(service.changementAdresse(eq(clientId), any(Adresse.class))).thenReturn(updatedClient);

        // WHEN - Call delegate modifierAdresse
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierAdresse(clientId, adresseDto);

        // THEN - Should return 200 with ligne2 null in response
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
        assertEquals("10 rue de la Paix", result.getBody().getLigne1());
        assertNull(result.getBody().getLigne2()); // Should be null
        assertEquals("75001", result.getBody().getCodePostal());
        assertEquals("Paris", result.getBody().getVille());

        verify(service).changementAdresse(eq(clientId), any(Adresse.class));
        verifyNoMoreInteractions(service);
    }

    @Test
    public void given_dto_mapping_modifierAdresse_should_convert_dto_to_domain() throws Exception {
        // GIVEN - AdresseDto with all fields
        UUID clientId = UUID.randomUUID();
        AdresseDto adresseDto = new AdresseDto();
        adresseDto.setLigne1("123 boulevard Haussmann");
        adresseDto.setLigne2("Etage 3");
        adresseDto.setCodePostal("75008");
        adresseDto.setVille("Paris");

        Client updatedClient = Client.of(
            clientId,
            new Nom("Dupont"),
            new Prenom("Jean"),
            new Adresse(
                new LigneAdresse("123 boulevard Haussmann"),
                new LigneAdresse("Etage 3"),
                new CodePostal("75008"),
                new Ville("Paris")
            ),
            SituationFamiliale.MARIE,
            2
        );

        when(service.changementAdresse(eq(clientId), any(Adresse.class))).thenReturn(updatedClient);

        // WHEN - Call delegate modifierAdresse
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierAdresse(clientId, adresseDto);

        // THEN - Should map DTO → Domain → DTO correctly
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto responseDto = result.getBody();
        assertNotNull(responseDto);
        
        // Verify address fields mapped correctly
        assertEquals("123 boulevard Haussmann", responseDto.getLigne1());
        assertEquals("Etage 3", responseDto.getLigne2());
        assertEquals("75008", responseDto.getCodePostal());
        assertEquals("Paris", responseDto.getVille());
        
        // Verify other client fields present (full client response)
        assertEquals("Dupont", responseDto.getNom());
        assertEquals("Jean", responseDto.getPrenom());
        assertEquals(SituationFamilialeDto.MARIE, responseDto.getSituationFamiliale());
        assertEquals(2, responseDto.getNombreEnfants());

        verify(service).changementAdresse(eq(clientId), any(Adresse.class));
        verifyNoMoreInteractions(service);
    }

    // ==================== PATCH /situation Tests ====================

    @Test
    @DisplayName("Given valid situation, modifierSituation() should return 200 OK with full client")
    void given_valid_situation_modifierSituation_should_return_full_client() throws ClientInconnuException {
        // GIVEN - Valid situation update
        UUID clientId = UUID.fromString("abc615cc-ae15-4bf5-a4bb-717ba7a26dc4");
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(2);

        // Mock domain service response
        Client mockClient = Client.of(
                new Nom("Martin"),
                new Prenom("Sophie"),
                new Adresse(
                        new LigneAdresse("10 rue de la Paix"),
                        new CodePostal("75001"),
                        new Ville("Paris")
                ),
                SituationFamiliale.MARIE,
                2
        );
        when(service.changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(2)))
                .thenReturn(mockClient);

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(clientId, situationDto);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto responseDto = result.getBody();
        assertNotNull(responseDto);
        
        // Verify situation fields mapped correctly
        assertEquals(SituationFamilialeDto.MARIE, responseDto.getSituationFamiliale());
        assertEquals(2, responseDto.getNombreEnfants());
        
        // Verify other client fields present (full client response)
        assertEquals("Martin", responseDto.getNom());
        assertEquals("Sophie", responseDto.getPrenom());
        assertEquals("10 rue de la Paix", responseDto.getLigne1());
        assertEquals("75001", responseDto.getCodePostal());
        assertEquals("Paris", responseDto.getVille());

        verify(service).changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(2));
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Given unknown client, modifierSituation() should return 404 Not Found")
    void given_unknown_client_modifierSituation_should_return_404() throws ClientInconnuException {
        // GIVEN - Unknown client
        UUID unknownClientId = UUID.fromString("76cd9d55-c0ff-4e1e-9f04-17caab5deeee");
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(1);

        // Mock service throwing ClientInconnuException
        when(service.changementSituation(eq(unknownClientId), eq(SituationFamiliale.MARIE), eq(1)))
                .thenThrow(new ClientInconnuException());

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(unknownClientId, situationDto);

        // THEN
        assertEquals(HttpStatus.NOT_FOUND, result.getStatusCode());
        assertNull(result.getBody());

        verify(service).changementSituation(eq(unknownClientId), eq(SituationFamiliale.MARIE), eq(1));
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Given boundary value (0 children), modifierSituation() should map correctly")
    void given_boundary_value_zero_children_modifierSituation_should_map_correctly() throws ClientInconnuException {
        // GIVEN - Boundary value: 0 children
        UUID clientId = UUID.fromString("abc615cc-ae15-4bf5-a4bb-717ba7a26dc4");
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(0);

        // Mock domain service response
        Client mockClient = Client.of(
                new Nom("Dubois"),
                new Prenom("Marie"),
                new Adresse(
                        new LigneAdresse("5 place de la République"),
                        new CodePostal("69001"),
                        new Ville("Lyon")
                ),
                SituationFamiliale.MARIE,
                0
        );
        when(service.changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(0)))
                .thenReturn(mockClient);

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(clientId, situationDto);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto responseDto = result.getBody();
        assertNotNull(responseDto);
        assertEquals(0, responseDto.getNombreEnfants());

        verify(service).changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(0));
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Given boundary value (20 children), modifierSituation() should map correctly")
    void given_boundary_value_max_children_modifierSituation_should_map_correctly() throws ClientInconnuException {
        // GIVEN - Boundary value: 20 children (maximum)
        UUID clientId = UUID.fromString("abc615cc-ae15-4bf5-a4bb-717ba7a26dc4");
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(SituationFamilialeDto.MARIE);
        situationDto.setNombreEnfants(20);

        // Mock domain service response
        Client mockClient = Client.of(
                new Nom("Rousseau"),
                new Prenom("Pierre"),
                new Adresse(
                        new LigneAdresse("100 avenue des Champs-Elysées"),
                        new CodePostal("75008"),
                        new Ville("Paris")
                ),
                SituationFamiliale.MARIE,
                20
        );
        when(service.changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(20)))
                .thenReturn(mockClient);

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(clientId, situationDto);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto responseDto = result.getBody();
        assertNotNull(responseDto);
        assertEquals(20, responseDto.getNombreEnfants());

        verify(service).changementSituation(eq(clientId), eq(SituationFamiliale.MARIE), eq(20));
        verifyNoMoreInteractions(service);
    }

    @Test
    @DisplayName("Given DTO mapping, modifierSituation() should convert all situation familiale values correctly")
    void given_dto_mapping_modifierSituation_should_convert_situation_familiale() throws ClientInconnuException {
        // GIVEN - Test all situation familiale enum values
        UUID clientId = UUID.fromString("abc615cc-ae15-4bf5-a4bb-717ba7a26dc4");

        // Test CELIBATAIRE
        SituationDto celibataireDto = new SituationDto();
        celibataireDto.setSituationFamiliale(SituationFamilialeDto.CELIBATAIRE);
        celibataireDto.setNombreEnfants(0);

        Client mockClientCelibataire = Client.of(
                new Nom("Test"),
                new Prenom("User"),
                new Adresse(
                        new LigneAdresse("1 rue Test"),
                        new CodePostal("75001"),
                        new Ville("Paris")
                ),
                SituationFamiliale.CELIBATAIRE,
                0
        );
        when(service.changementSituation(eq(clientId), eq(SituationFamiliale.CELIBATAIRE), eq(0)))
                .thenReturn(mockClientCelibataire);

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(clientId, celibataireDto);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(SituationFamilialeDto.CELIBATAIRE, result.getBody().getSituationFamiliale());

        verify(service).changementSituation(eq(clientId), eq(SituationFamiliale.CELIBATAIRE), eq(0));
    }

    @Test
    @DisplayName("Given PACSE situation, modifierSituation() should map correctly")
    void given_pacse_situation_modifierSituation_should_map_correctly() throws ClientInconnuException {
        // GIVEN - PACSE situation (Pacte Civil de Solidarité)
        UUID clientId = UUID.fromString("abc615cc-ae15-4bf5-a4bb-717ba7a26dc4");
        SituationDto situationDto = new SituationDto();
        situationDto.setSituationFamiliale(SituationFamilialeDto.PACSE);
        situationDto.setNombreEnfants(1);

        Client mockClient = Client.of(
                new Nom("Lambert"),
                new Prenom("Claire"),
                new Adresse(
                        new LigneAdresse("25 boulevard Saint-Germain"),
                        new CodePostal("75005"),
                        new Ville("Paris")
                ),
                SituationFamiliale.PACSE,
                1
        );
        when(service.changementSituation(eq(clientId), eq(SituationFamiliale.PACSE), eq(1)))
                .thenReturn(mockClient);

        // WHEN
        ResponseEntity<ConnaissanceClientDto> result = controller.modifierSituation(clientId, situationDto);

        // THEN
        assertEquals(HttpStatus.OK, result.getStatusCode());
        ConnaissanceClientDto responseDto = result.getBody();
        assertNotNull(responseDto);
        assertEquals(SituationFamilialeDto.PACSE, responseDto.getSituationFamiliale());
        assertEquals(1, responseDto.getNombreEnfants());

        verify(service).changementSituation(eq(clientId), eq(SituationFamiliale.PACSE), eq(1));
        verifyNoMoreInteractions(service);
    }
}


package com.sqli.workshop.ddd.connaissance.client.domain;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.mockito.invocation.InvocationOnMock;

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
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;

public class ConnaissanceClientServiceImplTest {

 private ConnaissanceClientService service;

  private ClientRepository repository;
  private CodePostauxService codePostauxService;
  private AdresseEventService adresseEventService;
  
  private static Object answer(InvocationOnMock invocationOnMock) {
    return invocationOnMock.getArgument(0);
  }

 @BeforeEach
 public void init() {
    this.repository = mock(ClientRepository.class);
    this.codePostauxService = mock(CodePostauxService.class);
    this.adresseEventService = mock(AdresseEventService.class);   
    this.service = new ConnaissanceClientServiceImpl(
      repository,
      codePostauxService,
      adresseEventService
    );
 }

 @Test
 public void given_connaissance_client_not_null_get_should_return_data() {
    // GIVEN
    Client cc = Client.of(
        new Nom("Bousquet"),
        new Prenom("Philippe"),
        new Adresse(
          new LigneAdresse("48 rue Bauducheu"),
          new CodePostal("33800"), 
          new Ville("Bordeaux")
        ),
        SituationFamiliale.CELIBATAIRE,
        0
    );
   UUID id = cc.getId();
   when(repository.lire(id)).thenReturn(Optional.of(cc));
   // WHEN
   Optional<Client> result = service.informationsClient(id);
   // THEN
   assertTrue(result.isPresent());
   assertEquals(id, result.get().getId());
   assertEquals("Bousquet", result.get().getNom().value());
   verify(repository).lire(id);
 }


  @Test
  public void given_connaissance_client_null_get_should_return_empty() {
    // GIVEN
    UUID id = UUID.randomUUID();
    when(repository.lire(id)).thenReturn(Optional.empty());
    // WHEN
    Optional<Client> result = service.informationsClient(id);
    // THEN
    assertTrue(result.isEmpty());
    verify(repository).lire(id);
  }


  @Test
  public void given_connaissance_client_save_return_id() {
    // GIVEN
    Client ccToSave = Client.of(
      new Nom("Bousquet"),
      new Prenom("Philippe"),
      new Adresse(
        new LigneAdresse("48 rue Bauducheu"),
        new CodePostal("33800"), 
        new Ville("Bordeaux")
      ),
      SituationFamiliale.CELIBATAIRE,
      0
  );
    when(codePostauxService.validateCodePostal(any(),any())).thenReturn(true);
    when(repository.enregistrer(any())).thenReturn(ccToSave);
    // WHEN
    Client result = null;
    try {
      result = service.nouveauClient(ccToSave);
    } catch (AdresseInvalideException a) {
      fail();
    }
    // THEN
    verify(repository).enregistrer(any(Client.class));
    assertNotNull(result.getId());
    assertEquals("Bousquet", result.getNom().value());
    assertEquals("Philippe", result.getPrenom().value());
    assertTrue(result.getAdresse().ligne2().isEmpty());
    assertEquals("33800", result.getAdresse().codePostal().value());
    assertEquals("Bordeaux", result.getAdresse().ville().value());
    assertEquals(SituationFamiliale.CELIBATAIRE, result.getSituationFamiliale());
    assertEquals(Integer.valueOf(0), result.getNombreEnfants());
  }

  @Test
  public void given_adresse_save_return_ok() {
    // GIVEN
    Adresse adresse = new Adresse(
      new LigneAdresse("lg1"), 
      new LigneAdresse("lg2"), 
      new CodePostal("33800"), 
      new Ville("Bordeaux")
    );
  Client cc = Client.of(
            new Nom("Bousquet"),
            new Prenom("Philippe"),
            new Adresse(
              new LigneAdresse("48 rue Bauducheu"),
              new CodePostal("33800"), 
              new Ville("Bordeaux")
            ),
            SituationFamiliale.CELIBATAIRE,
            0
    );
    UUID id = cc.getId();
    when(codePostauxService.validateCodePostal(any(),any())).thenReturn(true);
    when(repository.lire(id)).thenReturn(Optional.of(cc));
    when(repository.enregistrer(any())).thenAnswer(ConnaissanceClientServiceImplTest::answer);
    // WHEN
    Client result = null;
    try {
      result = service.changementAdresse(id, adresse);
    } catch (AdresseInvalideException e) {
      fail();
    } catch (ClientInconnuException e) {
      fail();
    }
    // THEN
    assertEquals("lg1", result.getAdresse().ligne1().value());
    verify(repository).lire(id);
    verify(repository).enregistrer(any(Client.class));
  }

  @Test
  public void given_situation_save_return_ok() {
    // GIVEN
    Client cc = Client.of(
      new Nom("Bousquet"),
      new Prenom("Philippe"),
      new Adresse(
        new LigneAdresse("48 rue Bauducheu"),
        new CodePostal("33800"), 
        new Ville("Bordeaux")
      ),
      SituationFamiliale.CELIBATAIRE,
      0
    );
    UUID id = cc.getId();
    when(repository.lire(any())).thenReturn(Optional.of(cc));
    when(repository.enregistrer(any())).thenAnswer(ConnaissanceClientServiceImplTest::answer);
    // WHEN
    Client result = null;
    try {
      result = service.changementSituation(id, SituationFamiliale.MARIE, 1);
    }
    catch (Exception e) {
      fail(e);
    }
    // THEN
    assertEquals(SituationFamiliale.MARIE, result.getSituationFamiliale());
    assertEquals(Integer.valueOf(1), result.getNombreEnfants());
    verify(repository).lire(any());
    verify(repository).enregistrer(any(Client.class));
  }

  @Test
  public void given_situation_no_client_save_return_ok() {
    // GIVEN
    when(repository.lire(any())).thenReturn(Optional.empty());
    // WHEN
    try {
      service.changementSituation(UUID.randomUUID(), SituationFamiliale.MARIE, 0);
      fail(ClientInconnuException.class.getName() + " Excpeted");
    }
    catch (Exception e) {
      assertTrue(e instanceof ClientInconnuException);
    }
    // THEN
    verify(repository).lire(any());
    verifyNoMoreInteractions(repository);
  }

  @Test
  public void given_Client_findall_return_list() {
    // GIVEN
    Client cc = Client.of(
      new Nom("Bousquet"),
      new Prenom("Philippe"),
      new Adresse(
        new LigneAdresse("48 rue Bauducheu"),
        new CodePostal("33800"), 
        new Ville("Bordeaux")
      ),
      SituationFamiliale.CELIBATAIRE,
      0
    );
    List<Client> list = new ArrayList<>();
    list.add(cc);
    when(repository.lister()).thenReturn(list);
    // WHEN
    List<Client> result = service.listerClients();
    // THEN
    assertEquals(1, result.size());
    verify(repository).lister();
  }

    @Test
    public void delete_client_return_ok() {
      service.supprimerClient(UUID.randomUUID());
      verify(repository).supprimer(any());
    }

}

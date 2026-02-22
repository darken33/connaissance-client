package com.sqli.workshop.ddd.connaissance.client.db;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ClientRepositoryImplTest - TODO: description
 *
 * @author TODO
 */
public class ClientRepositoryImplTest {

    private ClientDbRepository repository;
    private ClientRepository service;

  private static Object answer(InvocationOnMock invocationOnMock) {
    return invocationOnMock.getArgument(0);
  }

    @BeforeEach
/**
 * init - TODO: description
 *
 */
    public void init() {
        repository = mock(ClientDbRepository.class);
        service = new ClientRepositoryImpl(
            repository,
            new ClientDbMapperImpl()
        );
    }

    @Test
/**
 * given_repository_findById_return_data_should_return_data - TODO: description
 *
 */
    public void given_repository_findById_return_data_should_return_data() {
        // GIVEN
        UUID ccUuid = UUID.randomUUID();
        ClientDb ccdb = new ClientDb();
        ccdb.setId(ccUuid.toString());
        ccdb.setNom("Bousquet");
        ccdb.setPrenom("Philippe");
        ccdb.setLigne1("48 rue bauducheu");
        ccdb.setCodePostal("33800");
        ccdb.setVille("Bordeaux");
        ccdb.setSituationFamiliale("CELIBATAIRE");
        ccdb.setNombreEnfants(0);
        String id = ccUuid.toString();
        when(repository.findById(id)).thenReturn(Optional.of(ccdb));
        // WHEN
        Optional<Client> ccOpt = service.lire(ccUuid);
        // THEN
        assertTrue(ccOpt.isPresent());
        var cc = ccOpt.get();
        assertEquals(ccdb.getId(), cc.getId().toString());
        assertEquals(ccdb.getNom(), cc.getNom().value());
        assertEquals(ccdb.getPrenom(), cc.getPrenom().value());
        assertEquals(ccdb.getLigne1(), cc.getAdresse().ligne1().value());
        assertTrue(cc.getAdresse().ligne2().isEmpty());
        assertEquals(ccdb.getCodePostal(), cc.getAdresse().codePostal().value());
        assertEquals(ccdb.getVille(), cc.getAdresse().ville().value());
        assertEquals(ccdb.getSituationFamiliale(), cc.getSituationFamiliale().toString());
        assertEquals(ccdb.getNombreEnfants(), cc.getNombreEnfants());
        verify(repository).findById(id);
    }

    @Test
/**
 * given_repository_findById_return_empty_should_return_empty - TODO: description
 *
 */
    public void given_repository_findById_return_empty_should_return_empty() {
        // GIVEN
        UUID ccUuid = UUID.randomUUID();
        String id = ccUuid.toString();
        when(repository.findById(id)).thenReturn(Optional.empty());
        // WHEN
        Optional<Client> ccOpt = service.lire(ccUuid);
        // THEN
        assertTrue(ccOpt.isEmpty());
        verify(repository).findById(id);
    }

    @Test
    public void given_repository_findAll_return_data_should_return_data() {
        // GIVEN
        UUID ccUuid = UUID.randomUUID();
        ClientDb ccdb = new ClientDb();
        ccdb.setId(ccUuid.toString());
        ccdb.setNom("Bousquet");
        ccdb.setPrenom("Philippe");
        ccdb.setLigne1("48 rue bauducheu");
        ccdb.setCodePostal("33800");
        ccdb.setVille("Bordeaux");
        ccdb.setSituationFamiliale("CELIBATAIRE");
        ccdb.setNombreEnfants(0);
        List<ClientDb> ccdbList = new ArrayList<>();
        ccdbList.add(ccdb);
        when(repository.findAll()).thenReturn(ccdbList);
        // WHEN
        List<Client> ccList = service.lister();
        // THEN
        assertEquals(1, ccList.size());
        var cc = ccList.getFirst();
        assertEquals(ccdb.getId(), cc.getId().toString());
        assertEquals(ccdb.getNom(), cc.getNom().value());
        assertEquals(ccdb.getPrenom(), cc.getPrenom().value());
        assertEquals(ccdb.getLigne1(), cc.getAdresse().ligne1().value());
        assertTrue(cc.getAdresse().ligne2().isEmpty());
        assertEquals(ccdb.getCodePostal(), cc.getAdresse().codePostal().value());
        assertEquals(ccdb.getVille(), cc.getAdresse().ville().value());
        assertEquals(ccdb.getSituationFamiliale(), cc.getSituationFamiliale().toString());
        assertEquals(ccdb.getNombreEnfants(), cc.getNombreEnfants());
        verify(repository).findAll();
    }

    @Test
    public void given_repository_findAll_return_empty_should_return_empty() {
        // GIVEN
        when(repository.findAll()).thenReturn(new ArrayList<>());
        // WHEN
        var ccList = service.lister();
        // THEN
        assertTrue(ccList.isEmpty());
        verify(repository).findAll();
    }

    @Test
    public void given_repository_save_triggered_should_return_data() {
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
        when(repository.save(any())).thenAnswer(ClientRepositoryImplTest::answer);
        // WHEN
        var cc2 = service.enregistrer(cc);
        // THEN
        assertEquals(cc.getId(),cc2.getId());
        verify(repository).save(any());
    }

    @Test
    public void given_repository_delete_triggered() {
        // WHEN
        service.supprimer(UUID.randomUUID());
        // THEN
        verify(repository).deleteById(any());
    }

}
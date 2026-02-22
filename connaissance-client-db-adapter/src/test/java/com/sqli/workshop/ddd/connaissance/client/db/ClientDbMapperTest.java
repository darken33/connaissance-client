package com.sqli.workshop.ddd.connaissance.client.db;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClientDbMapperTest {

    private static ClientDbMapper mapper;

    @BeforeAll
    public static void init() {
        mapper = new ClientDbMapperImpl();
    }

    @Test
    public void mapFromDomain_without_ligne2() {
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
        ClientDb ccdb = mapper.mapFromDomain(cc);
        assertEquals(cc.getId().toString(), ccdb.getId());
        assertEquals(cc.getNom().value(), ccdb.getNom());
        assertEquals(cc.getPrenom().value(), ccdb.getPrenom());
        assertEquals(cc.getAdresse().ligne1().value(), ccdb.getLigne1());
        assertNull(ccdb.getLigne2());
        assertEquals(cc.getAdresse().codePostal().value(), ccdb.getCodePostal());
        assertEquals(cc.getAdresse().ville().value(), ccdb.getVille());
        assertEquals(cc.getSituationFamiliale().toString(), ccdb.getSituationFamiliale());
        assertEquals(cc.getNombreEnfants(), ccdb.getNombreEnfants());
    }

    @Test
    public void mapFromDomain_with_ligne2() {
        Client cc = Client.of(
                new Nom("Bousquet"),
                new Prenom("Philippe"),
                new Adresse(
                    new LigneAdresse("48 rue bauducheu"),
                    new LigneAdresse("suite"),
                    new CodePostal("33800"),
                    new Ville("Bordeaux")
                ),
                SituationFamiliale.CELIBATAIRE,
                0
        );
        ClientDb ccdb = mapper.mapFromDomain(cc);
        assertEquals(cc.getId().toString(), ccdb.getId());
        assertEquals(cc.getNom().value(), ccdb.getNom());
        assertEquals(cc.getPrenom().value(), ccdb.getPrenom());
        assertEquals(cc.getAdresse().ligne1().value(), ccdb.getLigne1());
        assertEquals(cc.getAdresse().ligne2().orElseThrow().value(), ccdb.getLigne2());
        assertEquals(cc.getAdresse().codePostal().value(), ccdb.getCodePostal());
        assertEquals(cc.getAdresse().ville().value(), ccdb.getVille());
        assertEquals(cc.getSituationFamiliale().toString(), ccdb.getSituationFamiliale());
        assertEquals(cc.getNombreEnfants(), ccdb.getNombreEnfants());
    }

    @Test
    public void mapToDomain_without_ligne2() {
        ClientDb ccdb = new ClientDb();
        ccdb.setId(UUID.randomUUID().toString());
        ccdb.setNom("Bousquet");
        ccdb.setPrenom("Philippe");
        ccdb.setLigne1("48 rue bauducheu");
        ccdb.setCodePostal("33800");
        ccdb.setVille("Bordeaux");
        ccdb.setSituationFamiliale("CELIBATAIRE");
        ccdb.setNombreEnfants(0);
        Client cc = mapper.mapToDomain(ccdb);
        assertEquals(ccdb.getId(), cc.getId().toString());
        assertEquals(ccdb.getNom(), cc.getNom().value());
        assertEquals(ccdb.getPrenom(), cc.getPrenom().value());
        assertEquals(ccdb.getLigne1(), cc.getAdresse().ligne1().value());
        assertTrue(cc.getAdresse().ligne2().isEmpty());
        assertEquals(ccdb.getCodePostal(), cc.getAdresse().codePostal().value());
        assertEquals(ccdb.getVille(), cc.getAdresse().ville().value());
        assertEquals(ccdb.getSituationFamiliale(), cc.getSituationFamiliale().toString());
        assertEquals(ccdb.getNombreEnfants(), cc.getNombreEnfants());
    }

    @Test
    public void mapToDomain_with_ligne2() {
        ClientDb ccdb = new ClientDb();
        ccdb.setId(UUID.randomUUID().toString());
        ccdb.setNom("Bousquet");
        ccdb.setPrenom("Philippe");
        ccdb.setLigne1("48 rue bauducheu");
        ccdb.setLigne2("Ligne 2");
        ccdb.setCodePostal("33800");
        ccdb.setVille("Bordeaux");
        ccdb.setSituationFamiliale("CELIBATAIRE");
        ccdb.setNombreEnfants(0);
        Client cc = mapper.mapToDomain(ccdb);
        assertEquals(ccdb.getId(), cc.getId().toString());
        assertEquals(ccdb.getNom(), cc.getNom().value());
        assertEquals(ccdb.getPrenom(), cc.getPrenom().value());
        assertEquals(ccdb.getLigne1(), cc.getAdresse().ligne1().value());
        assertEquals(ccdb.getLigne2(), cc.getAdresse().ligne2().orElseThrow().value());
        assertEquals(ccdb.getCodePostal(), cc.getAdresse().codePostal().value());
        assertEquals(ccdb.getVille(), cc.getAdresse().ville().value());
        assertEquals(ccdb.getSituationFamiliale(), cc.getSituationFamiliale().toString());
        assertEquals(ccdb.getNombreEnfants(), cc.getNombreEnfants());
    }

}
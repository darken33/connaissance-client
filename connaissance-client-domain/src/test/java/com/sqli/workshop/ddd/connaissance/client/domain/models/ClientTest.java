package com.sqli.workshop.ddd.connaissance.client.domain.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;

public class ClientTest {

  @Test
  public void allargsconstructor_will_return_ok() {
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
    assertNotNull(cc.getId());
    assertNotNull(cc.getNom());
    assertNotNull(cc.getPrenom());
    assertNotNull(cc.getAdresse());
    assertNotNull(cc.getSituationFamiliale());
    assertNotNull(cc.getNom());
  }

  @Test
  public void sort_Client_on_fields_nom_prenom() {
    Client cc0 = Client.of(
            new Nom("Bousquet"),
            new Prenom("Thierry"),
            new Adresse(
                new LigneAdresse("Adresse 1"),
                new CodePostal("33600"), 
                new Ville("Pessac")
            ),
            SituationFamiliale.MARIE,
            2
    );
    Client cc1 = Client.of(
            new Nom("Cesar Ceballos"),
            new Prenom("Dulce"),
            new Adresse(
                new LigneAdresse("Adresse 2"),
                new CodePostal("33800"), 
                new Ville("Bordeaux")
            ),
            SituationFamiliale.CELIBATAIRE,
            1
    );
    Client cc2 = Client.of(
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
    List<Client> ClientList = new ArrayList<>();
    ClientList.add(cc0);
    ClientList.add(cc1);
    ClientList.add(cc2);
    var liste = ClientList.stream().sorted().toList();
    assertEquals(3, liste.size());
    assertEquals("Philippe", liste.getFirst().getPrenom().value());
    assertEquals("Thierry", liste.get(1).getPrenom().value());
    assertEquals("Dulce", liste.get(2).getPrenom().value());
  }

}

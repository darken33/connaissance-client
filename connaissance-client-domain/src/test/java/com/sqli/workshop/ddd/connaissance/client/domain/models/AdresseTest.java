package com.sqli.workshop.ddd.connaissance.client.domain.models;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;

/**
 * Classe de test de l'objet métier Adresse
 */
class AdresseTest {
  
  @Test
  void constructor_with_all_field_will_return_ok() {
    Adresse adr = new Adresse(
      new LigneAdresse("lg1"), 
      new LigneAdresse("lg2"), 
      new CodePostal("33800"), 
      new Ville("Bordeaux")
    );
    assertEquals("lg1", adr.ligne1().value());
    assertTrue(adr.ligne2().isPresent());
    assertEquals("lg2", adr.ligne2().get().value());
    assertEquals("33800", adr.codePostal().value());
    assertEquals("Bordeaux", adr.ville().value());
  }

  @Test
  public void constructor_without_ligne2_will_return_empty_field() {
    Adresse adr = new Adresse(
      new LigneAdresse("lg1"), 
      new CodePostal("33800"), 
      new Ville("Bordeaux")
    );
    assertEquals("lg1", adr.ligne1().value());
    assertTrue(adr.ligne2().isEmpty());
    assertEquals("33800", adr.codePostal().value());
    assertEquals("Bordeaux", adr.ville().value());
  }

}

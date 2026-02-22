package com.sqli.workshop.ddd.connaissance.client.cpostal;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.client.CodesPostauxApi;

public class CodePostauxServiceImplIT {

    private CodePostauxService cpService;

    @BeforeEach
    public void setup() throws Exception {
        cpService = new CodePostauxServiceImpl(new CodesPostauxApi());
    }

    @Test
    public void given_code_postal_ville_should_return_true() throws Exception {
        // GIVEN
        String codePostal = "33800";
        String ville = "Bordeaux";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        // WHEN
        boolean result = cpService.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertTrue(result);
    }

    @Test
    public void given_code_postal_ville_invalid_should_return_false() throws Exception {
        // GIVEN
        String codePostal = "33800";
        String ville = "Toulouse";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        // WHEN
        boolean result = cpService.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
    }

    @Test
    public void given_code_postal_invalid_should_return_false() throws Exception {
        // GIVEN
        String codePostal = "33XXX";
        String ville = "Bordeaux";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        // WHEN
        boolean result = cpService.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
    }
}

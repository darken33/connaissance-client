package com.sqli.workshop.ddd.connaissance.client.cpostal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.client.CodesPostauxApi;
import com.sqli.workshop.ddd.connaissance.client.generated.codepostal.model.Commune;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;

public class CodePostauxServiceImplTest implements CodePostauxService {
    
    CodesPostauxApi codesPostauxApi;
    CodePostauxService service;

    @BeforeEach
    public void init() {
        codesPostauxApi = mock(CodesPostauxApi.class);
        service = new CodePostauxServiceImpl(codesPostauxApi);
    }


    @Test
    public void code_postal_and_ville_valid_and_api_returns_one_validateCodePostal_should_return_true() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Bordeaux";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        Commune communeResult = new Commune();
        communeResult.setCodeCommune("33800");
        communeResult.setCodePostal("33800");
        communeResult.setNomCommune("BORDEAUX");
        List<Commune> communes = Arrays.asList(communeResult);
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.ok().body(communes); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertTrue(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void code_postal_and_ville_valid_and_api_returns_list_validateCodePostal_should_return_true() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Bordeaux";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        Commune communeResult = new Commune();
        communeResult.setCodeCommune("33800");
        communeResult.setCodePostal("33800");
        communeResult.setNomCommune("PESSAC");
        Commune communeResult2 = new Commune();
        communeResult2.setCodeCommune("33800");
        communeResult2.setCodePostal("33800");
        communeResult2.setNomCommune("BORDEAUX");
        List<Commune> communes = Arrays.asList(communeResult, communeResult2);
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.ok().body(communes); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertTrue(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void code_postal_and_ville_valid_and_api_returns_empty_validateCodePostal_should_return_false() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Bordeaux";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        List<Commune> communes = new ArrayList<>();
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.ok().body(communes); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void code_postal_valid_and_ville_invalid_and_api_returns_one_validateCodePostal_should_return_false() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Pessac";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        Commune communeResult = new Commune();
        communeResult.setCodeCommune("33800");
        communeResult.setCodePostal("33800");
        communeResult.setNomCommune("BORDEAUX");
        List<Commune> communes = Arrays.asList(communeResult);
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.ok().body(communes); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void api_returns_400_validateCodePostal_should_return_false() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Pessac";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.badRequest().build(); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void api_returns_500_validateCodePostal_should_return_false() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Pessac";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        ResponseEntity<List<Commune>> httpResult = ResponseEntity.internalServerError().build(); 
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenReturn(httpResult);
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }

    @Test
    public void api_throws_exception_validateCodePostal_should_return_false() {
        // GIVEN
        String codePostal = "33800";
        String ville = "Pessac";
        CodePostal rCodePostal = new CodePostal(codePostal);
        Ville rVille = new Ville(ville);
        when(codesPostauxApi.codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal)).thenThrow(new HttpClientErrorException(HttpStatusCode.valueOf(400)));
        // WHEN
        boolean result = service.validateCodePostal(rCodePostal, rVille);
        // THEN
        assertFalse(result);
        verify(codesPostauxApi).codesPostauxCommunesCodePostalGetWithHttpInfo(codePostal);
    }
}


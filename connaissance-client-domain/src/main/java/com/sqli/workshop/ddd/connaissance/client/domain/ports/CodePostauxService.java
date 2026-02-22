package com.sqli.workshop.ddd.connaissance.client.domain.ports;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;

public interface CodePostauxService {

    default boolean validateCodePostal(CodePostal codePostal, Ville ville) {
        return true;
    }
    
}
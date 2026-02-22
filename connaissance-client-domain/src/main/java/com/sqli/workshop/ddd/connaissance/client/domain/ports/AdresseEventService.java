package com.sqli.workshop.ddd.connaissance.client.domain.ports;

import java.util.UUID;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Destinataire;

public interface AdresseEventService {

    default boolean sendEvent(UUID id, Destinataire destinataire, Adresse adresse) {
        return true;
    }
    
}
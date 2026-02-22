package com.sqli.workshop.ddd.connaissance.client.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NonNull;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.exceptions.AdresseInvalideException;
import com.sqli.workshop.ddd.connaissance.client.domain.exceptions.ClientInconnuException;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;

/**
 * Uses Cases métier de la fiche Connaissance Client
 */
public interface ConnaissanceClientService {

    default Client nouveauClient(@NonNull Client client) throws AdresseInvalideException {
        return null;
    }

    default List<Client> listerClients() {
        return List.of();
    }

    default Optional<Client> informationsClient(@NonNull UUID id) {
        return Optional.empty();
    }

    default Client changementAdresse(@NonNull UUID id, @NonNull Adresse adresse) throws AdresseInvalideException, ClientInconnuException {
        return null;
    }

    default Client changementSituation(@NonNull UUID id, @NonNull SituationFamiliale situationFamiliale, @NonNull Integer nombreEnfants) throws ClientInconnuException {
        return null;
    }

    default void supprimerClient(@NonNull UUID id) {
    }

}

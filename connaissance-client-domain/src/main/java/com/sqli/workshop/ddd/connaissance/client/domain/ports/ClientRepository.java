package com.sqli.workshop.ddd.connaissance.client.domain.ports;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;

/**
 * Port pour la sauvegarde de l'objet métier ConnaissanceClient
 */
public interface ClientRepository {

    List<Client>     lister();
    Optional<Client> lire(UUID id);
    Client           enregistrer(Client client);
    void             supprimer(UUID id);

}

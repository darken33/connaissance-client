package com.sqli.workshop.ddd.connaissance.client.db;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
/**
 * ClientRepositoryImpl - TODO: description
 *
 * @author TODO
 */
public class ClientRepositoryImpl implements ClientRepository {

    private final ClientDbRepository dbRepository;
    private final ClientDbMapper mapper;

    @Override
/**
 * enregistrer - TODO: description
 *
 * @param Client TODO
 * @return TODO
 */
    public Client enregistrer(Client Client) {
        if ("Error".equals(Client.getNom().value())) throw new RuntimeException("Simulated database error for testing purposes.");
        var ClientDb = mapper.mapFromDomain(Client);
        ClientDb = dbRepository.save(ClientDb);
        return mapper.mapToDomain(ClientDb);
    }

    @Override
/**
 * lire - TODO: description
 *
 * @param id TODO
 * @return TODO
 */
    public Optional<Client> lire(UUID id) {
        Optional<ClientDb> ClientDb = dbRepository.findById(id.toString());
        return ClientDb.map(mapper::mapToDomain);
    }

    @Override
/**
 * lister - TODO: description
 *
 * @return TODO
 */
    public List<Client> lister() {
        List<ClientDb> ClientDbs = dbRepository.findAll();
        return ClientDbs.stream().map(mapper::mapToDomain).collect(Collectors.toList());
    }

    @Override
/**
 * supprimer - TODO: description
 *
 * @param id TODO
 */
    public void supprimer(UUID id) {
        dbRepository.deleteById(id.toString());
    }
}
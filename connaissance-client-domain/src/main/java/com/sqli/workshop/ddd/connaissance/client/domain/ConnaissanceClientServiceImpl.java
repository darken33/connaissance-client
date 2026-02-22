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
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Destinataire;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.ClientRepository;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.CodePostauxService;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ConnaissanceClientServiceImpl implements ConnaissanceClientService {

    private final ClientRepository repository;
    private final CodePostauxService codePostauxService;
    private final AdresseEventService adresseEventService;

    private void sendAdresseEvent(Client client) {
        adresseEventService.sendEvent(
            client.getId(), 
            new Destinataire(client.getNom(), client.getPrenom()), 
            client.getAdresse()
        );
    }

    @Override
    public List<Client> listerClients() {
        return repository.lister();
    }

    @Override
    public Optional<Client> informationsClient(@NonNull UUID id) {
        return repository.lire(id);
    }

    @Override
    public Client nouveauClient(@NonNull Client client) throws AdresseInvalideException {
        if (!codePostauxService.validateCodePostal(client.getAdresse().codePostal(), client.getAdresse().ville())) throw new AdresseInvalideException();
        var result = repository.enregistrer(client);
        sendAdresseEvent(result);
        return result;
    }

    @Override
    public Client changementAdresse(@NonNull UUID id, @NonNull Adresse adresse) throws AdresseInvalideException, ClientInconnuException {
        Client client = informationsClient(id).orElseThrow(ClientInconnuException::new);
        if (!codePostauxService.validateCodePostal(adresse.codePostal(), adresse.ville())) throw new AdresseInvalideException();
        client.setAdresse(adresse);
        var result = repository.enregistrer(client);
        sendAdresseEvent(result);
        return result;
    }

    @Override
    public Client changementSituation(@NonNull UUID id, @NonNull SituationFamiliale situationFamiliale, @NonNull Integer nombreEnfants) throws ClientInconnuException {
        Client client = informationsClient(id).orElseThrow(ClientInconnuException::new);
        client.setSituationFamiliale(situationFamiliale);
        client.setNombreEnfants(nombreEnfants);
        return repository.enregistrer(client);
    }

    @Override
    public void supprimerClient(@NonNull UUID id) {
        repository.supprimer(id);
    }

}

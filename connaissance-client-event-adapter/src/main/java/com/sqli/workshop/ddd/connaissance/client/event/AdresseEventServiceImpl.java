package com.sqli.workshop.ddd.connaissance.client.event;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Destinataire;
import com.sqli.workshop.ddd.connaissance.client.domain.ports.AdresseEventService;
import com.sqli.workshop.ddd.connaissance.client.generated.event.model.Adresse;
import com.sqli.workshop.ddd.connaissance.client.generated.event.model.AdresseMessagePayload;
import com.sqli.workshop.ddd.connaissance.client.generated.event.producer.IDefaultServiceEventsProducer;

import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
public class AdresseEventServiceImpl implements AdresseEventService {
    
    IDefaultServiceEventsProducer adresseEventService;

    @Override
    public boolean sendEvent(UUID id, Destinataire destinataire,
            com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse adresse) {
                AdresseMessagePayload payload = new AdresseMessagePayload();
                payload.setClientId(id.toString());
                Adresse adresseMsg = new Adresse();
                adresseMsg.setDestinataire(destinataire.nom().value() + " " + destinataire.prenom().value());
                adresseMsg.setCodePostal(adresse.codePostal().value());
                adresseMsg.setLigne1(adresse.ligne1().value());
                if (adresse.ligne2().isPresent()) {
                    adresseMsg.setLigne2(adresse.ligne2().get().value());
                }
                adresseMsg.setVille(adresse.ville().value());
                payload.setAdresse(adresseMsg);
                adresseEventService.sendAdresseMessage(payload);
                return true;
            }

}

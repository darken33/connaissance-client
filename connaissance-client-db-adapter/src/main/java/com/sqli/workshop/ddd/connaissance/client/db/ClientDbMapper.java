package com.sqli.workshop.ddd.connaissance.client.db;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.Client;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.CodePostal;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.LigneAdresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Ville;

import org.mapstruct.*;

import java.util.Optional;
import java.util.UUID;

@Mapper(componentModel = "spring", nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
/**
 * ClientDbMapper - TODO: description
 *
 * @author TODO
 */
public interface ClientDbMapper {

    @Mapping(source = "nom.value", target = "nom")
    @Mapping(source = "prenom.value", target = "prenom")
    @Mapping(source = "id", target = "id", qualifiedByName = "mapId")
    @Mapping(source = "adresse.ligne1.value", target = "ligne1")
    @Mapping(source = "adresse.ligne2", target = "ligne2", qualifiedByName = "mapLigne2")
    @Mapping(source = "adresse.codePostal.value", target = "codePostal")
    @Mapping(source = "adresse.ville.value", target = "ville")
    ClientDb mapFromDomain(Client cclient);

    default String mapLigneAdresse(LigneAdresse ligne) {
        return (ligne != null ? ligne.value() : null); 
    }

    default String mapCodePostal(CodePostal codePostal) {
        return (codePostal != null ? codePostal.value() : null); 
    }

    default String mapVille(Ville ville) {
        return (ville != null ? ville.value() : null); 
    }
   
    @Named(value = "mapId")
    default String map(UUID id) {
        return id.toString();
    }

    @Named(value = "mapLigne2")
    default String map(Optional<LigneAdresse> ligne) {
        if (ligne.isPresent()) return ligne.get().value();
        return null;
    }

    default Client mapToDomain(ClientDb ClientDb) {
        return Client.of(
                UUID.fromString(ClientDb.getId()),
                new Nom(ClientDb.getNom()),
                new Prenom(ClientDb.getPrenom()),
                (ClientDb.getLigne2() != null ?
                    new Adresse(
                        new LigneAdresse(ClientDb.getLigne1()),
                        new LigneAdresse(ClientDb.getLigne2()),
                        new CodePostal(ClientDb.getCodePostal()),
                        new Ville(ClientDb.getVille())
                    ) : 
                    new Adresse(
                        new LigneAdresse(ClientDb.getLigne1()),
                        new CodePostal(ClientDb.getCodePostal()),
                        new Ville(ClientDb.getVille())
                    ) 
                ),
                SituationFamiliale.valueOf(ClientDb.getSituationFamiliale()),
                ClientDb.getNombreEnfants()
        );
    }

}
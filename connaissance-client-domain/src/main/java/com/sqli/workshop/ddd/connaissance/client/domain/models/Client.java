package com.sqli.workshop.ddd.connaissance.client.domain.models;

import com.sqli.workshop.ddd.connaissance.client.domain.enums.SituationFamiliale;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Adresse;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Nom;
import com.sqli.workshop.ddd.connaissance.client.domain.models.types.Prenom;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.io.Serializable;
import java.util.UUID;

/**
 * Objet métier représentant une fiche Client
 */
@FieldDefaults(level= AccessLevel.PRIVATE)
@AllArgsConstructor(staticName = "of")
@ToString
@EqualsAndHashCode
@Getter
public class Client implements Serializable, Comparable<Client> {

    @NonNull final UUID                 id;
    @NonNull final Nom                  nom;
    @NonNull final Prenom               prenom;
    @NonNull @Setter Adresse            adresse;
    @NonNull @Setter SituationFamiliale situationFamiliale;
    @NonNull @Setter Integer            nombreEnfants;

    /**
     * Constructeur sans UUID
     */
    public static Client of(Nom nom, Prenom prenom, Adresse adresse, SituationFamiliale situationFamiliale, Integer nombreEnfants) {
        return new Client(UUID.randomUUID(), nom, prenom, adresse, situationFamiliale, nombreEnfants);
    }

    @Override
    public int compareTo(Client o) {
        if (this.nom.equals(o.getNom())) {
            return this.prenom.compareTo(o.getPrenom());
        }
        return this.nom.compareTo(o.getNom());
    }
}


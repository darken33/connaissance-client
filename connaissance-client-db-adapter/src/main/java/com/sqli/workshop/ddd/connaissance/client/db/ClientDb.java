package com.sqli.workshop.ddd.connaissance.client.db;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Objet Connaissance Client pour la base de données
 */
@FieldDefaults(level= AccessLevel.PRIVATE)
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "connaissanceclient")
public class ClientDb {

    @Id
    private String id;

    private String nom;

    private String prenom;

    private String ligne1;

    private String ligne2;

    private String codePostal;

    private String ville;

    private String situationFamiliale;

    private Integer nombreEnfants;

}

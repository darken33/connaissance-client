package com.sqli.workshop.ddd.connaissance.client.domain.models.types;

import java.util.Optional;

public record Adresse(
        LigneAdresse            ligne1,
        Optional<LigneAdresse>  ligne2,
        CodePostal              codePostal,
        Ville                   ville
) {
        public Adresse(LigneAdresse ligne1, CodePostal codePostal, Ville ville) {
                this(ligne1, Optional.empty(), codePostal, ville);
        }
        public Adresse(LigneAdresse ligne1, LigneAdresse ligne2, CodePostal codePostal, Ville ville) {
                this(ligne1, Optional.of(ligne2), codePostal, ville);
        }
}

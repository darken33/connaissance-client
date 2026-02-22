package com.sqli.workshop.ddd.connaissance.client.domain.models.types;

import org.jspecify.annotations.NonNull;

public record Destinataire(
    @NonNull Nom    nom,
    @NonNull Prenom prenom
) {}

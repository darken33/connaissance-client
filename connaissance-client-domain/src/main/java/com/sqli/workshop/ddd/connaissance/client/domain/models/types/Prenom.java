package com.sqli.workshop.ddd.connaissance.client.domain.models.types;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Prenom(
                @NotNull 
                @Pattern(regexp = "^[a-zA-Z ,.'-]+$") 
                @Size(min = 2, max = 50) 
                String value
            ) implements Comparable<Prenom> {
    @Override
    public int compareTo(Prenom o) {
        return value().compareTo(o.value());
    }
}
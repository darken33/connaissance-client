package com.sqli.workshop.ddd.connaissance.client.domain.models.types;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record Nom(
                @NotNull 
                @Pattern(regexp = "^[a-zA-Z ,.'-]+$") 
                @Size(min = 2, max = 50) 
                String value
            ) implements Comparable<Nom> {
    @Override
    public int compareTo(Nom o) {
        return value().compareTo(o.value());
    }
}
